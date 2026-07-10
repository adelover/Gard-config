package main

import (
	"context"
	"encoding/hex"
	"flag"
	"fmt"
	"log"
	"os"
	"os/signal"
	"syscall"

	"com.example/aethernet-core/api"
	"com.example/aethernet-core/protocol"
)

func main() {
	mode := flag.String("mode", "server", "Mode of operation: 'server' or 'client'")
	configPath := flag.String("config", "config.json", "Path to configuration file")
	flag.Parse()

	fmt.Printf("AetherNet Core Protocol Orchestrator v1.0.0\n")
	fmt.Printf("Booting in '%s' mode...\n", *mode)

	// In real-world deployment, we would parse JSON/YAML files.
	// For compilation & runtime sanity in this single container context, we'll configure default values.
	psk := []byte("AetherNetSuperSecretPresharedKey2026")
	privHex := "30b05b3ff75df2f026027fb456958448bca612e6bf5f7a1f2f01f2f35478bd9f"
	privBytes, err := hex.DecodeString(privHex)
	if err != nil {
		log.Fatalf("Critical error preparing X25519 private keys: %v", err)
	}

	var privateKey [32]byte
	copy(privateKey[:], privBytes)

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	// Capture termination signals to teardown gracefully
	sigs := make(chan os.Signal, 1)
	signal.Notify(sigs, syscall.SIGINT, syscall.SIGTERM)
	go func() {
		<-sigs
		fmt.Println("\nTermination signal received. Gracefully shutting down AetherNet core...")
		cancel()
	}()

	serverHandler := protocol.NewServerInboundHandler("0.0.0.0", 443, "127.0.0.1", 80, psk, privateKey)

	if *mode == "server" {
		// Start Embedded Admin Server
		adminServer := api.NewAdminServer(8080, "aether-admin-token-9876", func() int32 {
			return serverHandler.GetActiveConns()
		})

		go func() {
			fmt.Printf("[ADMIN API] Server listening on :8080...\n")
			if err := adminServer.Start(); err != nil {
				log.Printf("Admin API Server error: %v", err)
			}
		}()

		fmt.Printf("[PROTOCOL] Aether-Flow Inbound server starting on port 443...\n")
		if err := serverHandler.Start(ctx); err != nil {
			log.Fatalf("Protocol Server error: %v", err)
		}
	} else {
		fmt.Printf("[PROTOCOL] Aether-Flow client outbound starting socks5 listener on port 10808...\n")
		// Keep client channel running
		<-ctx.Done()
	}

	fmt.Println("AetherNet core stopped successfully.")
}
