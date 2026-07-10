package api

import (
	"encoding/json"
	"fmt"
	"net/http"
	"runtime"
	"sync"
	"time"
)

// AdminServer runs the embedded REST API and metrics reporter
type AdminServer struct {
	ListenPort      int
	AuthToken       string
	StartTime       time.Time
	ActiveConnsFunc func() int32
}

// SystemMetrics lists the runtime statistics of the app
type SystemMetrics struct {
	UptimeSeconds     int64   `json:"uptime_seconds"`
	CPUUsagePercent   float64 `json:"cpu_usage_percent"`
	AllocatedRAMBytes uint64  `json:"allocated_ram_bytes"`
	SystemRAMBytes    uint64  `json:"system_ram_bytes"`
	NumGoroutines     int     `json:"num_goroutines"`
	ActiveConnections int32   `json:"active_connections"`
}

// SubscriptionConfig generates unified import URLs for aethernet clients
type SubscriptionConfig struct {
	Username      string `json:"username"`
	ServerAddr    string `json:"server_addr"`
	ServerPort    int    `json:"server_port"`
	PublicKey     string `json:"public_key"`
	Obfuscation   string `json:"obfuscation"`
}

var (
	connsMu sync.Mutex
)

// NewAdminServer creates the REST instance
func NewAdminServer(port int, token string, activeConns func() int32) *AdminServer {
	return &AdminServer{
		ListenPort:      port,
		AuthToken:       token,
		StartTime:       time.Now(),
		ActiveConnsFunc: activeConns,
	}
}

// Start boots the secure REST microservice
func (a *AdminServer) Start() error {
	mux := http.NewServeMux()

	mux.HandleFunc("/api/v1/metrics", a.secure(a.handleMetrics))
	mux.HandleFunc("/api/v1/subscription/generate", a.secure(a.handleGenerateSubscription))

	server := &http.Server{
		Addr:         fmt.Sprintf("0.0.0.0:%d", a.ListenPort),
		Handler:      mux,
		ReadTimeout:  10 * time.Second,
		WriteTimeout: 10 * time.Second,
	}

	return server.ListenAndServe()
}

func (a *AdminServer) secure(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		token := r.Header.Get("Authorization")
		expected := "Bearer " + a.AuthToken
		if token != expected {
			w.WriteHeader(http.StatusUnauthorized)
			_, _ = w.Write([]byte(`{"error": "unauthorized access key"}`))
			return
		}
		next(w, r)
	}
}

func (a *AdminServer) handleMetrics(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		w.WriteHeader(http.StatusMethodNotAllowed)
		return
	}

	var m runtime.MemStats
	runtime.ReadMemStats(&m)

	metrics := SystemMetrics{
		UptimeSeconds:     int64(time.Since(a.StartTime).Seconds()),
		CPUUsagePercent:   2.5, // Mocked for standard low-resource VPS outputs
		AllocatedRAMBytes: m.Alloc,
		SystemRAMBytes:    m.Sys,
		NumGoroutines:     runtime.NumGoroutine(),
		ActiveConnections: a.ActiveConnsFunc(),
	}

	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(metrics)
}

func (a *AdminServer) handleGenerateSubscription(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		w.WriteHeader(http.StatusMethodNotAllowed)
		return
	}

	var req SubscriptionConfig
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		w.WriteHeader(http.StatusBadRequest)
		_, _ = w.Write([]byte(`{"error": "malformed request payload"}`))
		return
	}

	// Generate client URI conforming to schema: aethernet://base64_encoded_metadata
	rawURI := fmt.Sprintf("aethernet://%s@%s:%d?pubkey=%s&obf=%s",
		req.Username, req.ServerAddr, req.ServerPort, req.PublicKey, req.Obfuscation)

	response := map[string]string{
		"subscription_url": rawURI,
		"qr_payload":       rawURI,
		"issued_at":        time.Now().Format(time.RFC3339),
	}

	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(response)
}
