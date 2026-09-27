package observability

import (
	"encoding/json"
	"log"
	"net/http"
	"strconv"
	"time"

	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/collectors"
	"github.com/prometheus/client_golang/prometheus/promhttp"
)

// Monitor keeps metric labels bounded and never records a phone number, token,
// request body, query string, or raw URL path.
type Monitor struct {
	requests *prometheus.CounterVec
	duration *prometheus.HistogramVec
	registry *prometheus.Registry
}

var knownRoutes = map[string]string{
	"/healthz": "GET", "/api/v2/catalog": "GET", "/api/v2/register": "POST",
	"/api/v2/login": "POST", "/api/v2/auth/otp/request": "POST",
	"/api/v2/auth/otp/verify": "POST", "/api/v2/me": "GET",
	"/api/v2/logout": "POST", "/api/v2/activity/puja": "POST",
	"/api/v2/shrine": "PUT", "/api/v2/purchase": "POST",
}

func New() *Monitor {
	reg := prometheus.NewRegistry()
	m := &Monitor{
		requests: prometheus.NewCounterVec(prometheus.CounterOpts{
			Name: "worship_http_requests_total", Help: "HTTP requests by route, method and status.",
		}, []string{"route", "method", "status"}),
		duration: prometheus.NewHistogramVec(prometheus.HistogramOpts{
			Name: "worship_http_request_duration_seconds", Help: "HTTP request duration in seconds.",
			Buckets: []float64{.005, .01, .025, .05, .1, .25, .5, 1, 2.5, 5, 10},
		}, []string{"route", "method"}),
		registry: reg,
	}
	reg.MustRegister(m.requests, m.duration, collectors.NewGoCollector(), collectors.NewProcessCollector(collectors.ProcessCollectorOpts{}))
	// Export zero-valued 500 series before traffic so the first 500 can be
	// detected by Prometheus increase(), even soon after a fresh deployment.
	for path, method := range knownRoutes {
		m.requests.WithLabelValues(path, method, "500")
	}
	return m
}

func (m *Monitor) Metrics() http.Handler {
	return promhttp.HandlerFor(m.registry, promhttp.HandlerOpts{})
}

type responseRecorder struct {
	http.ResponseWriter
	status int
}

func (w *responseRecorder) WriteHeader(code int) {
	if w.status != 0 {
		return
	}
	w.status = code
	w.ResponseWriter.WriteHeader(code)
}
func (w *responseRecorder) Write(b []byte) (int, error) {
	if w.status == 0 {
		w.WriteHeader(http.StatusOK)
	}
	return w.ResponseWriter.Write(b)
}

func route(path string) string {
	if _, ok := knownRoutes[path]; ok {
		return path
	}
	return "/other"
}

func (m *Monitor) Wrap(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		start := time.Now()
		name := route(r.URL.Path)
		rw := &responseRecorder{ResponseWriter: w}
		defer func() {
			if recovered := recover(); recovered != nil {
				// Error details stay in the server log; the response is generic.
				log.Printf("panic route=%s", name)
				if rw.status == 0 {
					rw.Header().Set("Content-Type", "application/json")
					rw.WriteHeader(http.StatusInternalServerError)
					_ = json.NewEncoder(rw).Encode(map[string]string{"error": "Internal server error"})
				}
			}
			if rw.status == 0 {
				rw.status = http.StatusOK
			}
			elapsed := time.Since(start).Seconds()
			status := strconv.Itoa(rw.status)
			m.requests.WithLabelValues(name, r.Method, status).Inc()
			m.duration.WithLabelValues(name, r.Method).Observe(elapsed)
			entry, _ := json.Marshal(map[string]any{"route": name, "method": r.Method, "status": rw.status, "duration_ms": elapsed * 1000})
			log.Print(string(entry))
		}()
		next.ServeHTTP(rw, r)
	})
}
