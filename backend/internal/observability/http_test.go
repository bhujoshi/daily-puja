package observability

import (
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

func TestMetricsTrackStatusAndTimingWithoutSensitivePaths(t *testing.T) {
	m := New()
	wrapped := m.Wrap(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(500)
	}))
	wrapped.ServeHTTP(httptest.NewRecorder(), httptest.NewRequest("POST", "/api/v2/purchase?token=private", nil))
	wrapped.ServeHTTP(httptest.NewRecorder(), httptest.NewRequest("GET", "/phone/9876543210", nil))
	recorder := httptest.NewRecorder()
	m.Metrics().ServeHTTP(recorder, httptest.NewRequest("GET", "/metrics", nil))
	data, _ := io.ReadAll(recorder.Result().Body)
	metrics := string(data)
	for _, expected := range []string{
		`worship_http_requests_total{method="POST",route="/api/v2/purchase",status="500"} 1`,
		`worship_http_request_duration_seconds_count{method="POST",route="/api/v2/purchase"} 1`,
		`route="/other"`,
	} {
		if !strings.Contains(metrics, expected) {
			t.Fatalf("missing %s", expected)
		}
	}
	if strings.Contains(metrics, "9876543210") || strings.Contains(metrics, "private") {
		t.Fatal("sensitive path or query leaked into metrics")
	}
}
