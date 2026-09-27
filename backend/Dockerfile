FROM golang:1.26.5-alpine AS build
WORKDIR /src
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 go build -trimpath -ldflags='-s -w' -o /server ./cmd/server

FROM alpine:3.22
RUN addgroup -S app && adduser -S -G app app && mkdir /data && chown app:app /data
COPY --from=build /server /server
USER app
ENV PORT=8080 APP_BIND=0.0.0.0 METRICS_ADDR=0.0.0.0:9091 ACCOUNT_DATA_PATH=/data/accounts.json
EXPOSE 8080 9091
CMD ["/server"]
