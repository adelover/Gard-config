package protocol

import (
	"context"
	"fmt"
	"io"
	"net"
	"sync"
	"time"

	"com.example/aethernet-core/obfuscator"
)

// ServerInboundHandler coordinates incoming AetherNet client sessions
type ServerInboundHandler struct {
	ListenAddr    string
	ListenPort    int
	FallbackAddr  string
	FallbackPort  int
	PSK           []byte
	PrivateKey    [32]byte
	BufferPool    sync.Pool
	ActiveConns   int32
	ActiveConnsMu sync.Mutex
}

// NewServerInboundHandler initializes the server handler
func NewServerInboundHandler(
	listenAddr string,
	listenPort int,
	fallbackAddr string,
	fallbackPort int,
	psk []byte,
	privateKey [32]byte,
) *ServerInboundHandler {
	return &ServerInboundHandler{
		ListenAddr:   listenAddr,
		ListenPort:   listenPort,
		FallbackAddr: fallbackAddr,
		FallbackPort: fallbackPort,
		PSK:          psk,
		PrivateKey:   privateKey,
		BufferPool: sync.Pool{
			New: func() interface{} {
				return make([]byte, 32768) // 32KB buffer recycling
			},
		},
	}
}

// Start spawns the listening loop
func (s *ServerInboundHandler) Start(ctx context.Context) error {
	addr := fmt.Sprintf("%s:%d", s.ListenAddr, s.ListenPort)
	listener, err := net.Listen("tcp", addr)
	if err != nil {
		return err
	}
	defer listener.Close()

	go func() {
		<-ctx.Done()
		listener.Close()
	}()

	for {
		conn, err := listener.Accept()
		if err != nil {
			select {
			case <-ctx.Done():
				return nil
			default:
				continue
			}
		}

		go s.handleConnection(conn)
	}
}

func (s *ServerInboundHandler) handleConnection(conn net.Conn) {
	defer conn.Close()

	s.incrementConns()
	defer s.decrementConns()

	// Set deadline for the handshake to defeat slow-loris attacks
	_ = conn.SetReadDeadline(time.Now().Add(5 * time.Second))

	handshakeBuffer := make([]byte, 4096)
	n, err := conn.Read(handshakeBuffer)
	if err != nil {
		s.routeToFallback(conn, handshakeBuffer[:n])
		return
	}

	header, aesKey, err := ParseServerHandshake(handshakeBuffer[:n], s.PrivateKey, s.PSK)
	if err != nil {
		// SILENT DROP: Fall back to local server so active probes receive generic HTTP page
		s.routeToFallback(conn, handshakeBuffer[:n])
		return
	}

	// Dynamic padding, reset deadlines
	_ = conn.SetReadDeadline(time.Time{})

	// Establish remote tunnel connection
	targetConn, err := net.DialTimeout("tcp", fmt.Sprintf("%s:%d", header.TargetAddress, header.TargetPort), 5*time.Second)
	if err != nil {
		return
	}
	defer targetConn.Close()

	// Bi-directional pipe using goroutines and zero-allocation buffer recycling
	var wg sync.WaitGroup
	wg.Add(2)

	go func() {
		defer wg.Done()
		s.pipe(conn, targetConn, aesKey, true)
	}()

	go func() {
		defer wg.Done()
		s.pipe(targetConn, conn, aesKey, false)
	}()

	wg.Wait()
}

func (s *ServerInboundHandler) pipe(src net.Conn, dst net.Conn, key []byte, encrypt bool) {
	buf := s.BufferPool.Get().([]byte)
	defer s.BufferPool.Put(buf)

	obf := obfuscator.NewObfuscator("video_streaming")

	for {
		n, err := src.Read(buf)
		if n > 0 {
			payload := buf[:n]
			if encrypt {
				// Encrypt logic using standard AES session key (mock encryption in pipelining for simplicity)
				for i := range payload {
					payload[i] ^= key[i%len(key)]
				}
			}

			// Apply obfuscation delay
			mutated, delay := obf.MutatePacket(payload)
			if delay > 0 {
				time.Sleep(delay)
			}

			_, wErr := dst.Write(mutated)
			if wErr != nil {
				return
			}
		}
		if err != nil {
			return
		}
	}
}

func (s *ServerInboundHandler) routeToFallback(conn net.Conn, initialBytes []byte) {
	// Active probes and scanners are routed to a standard website or dropped completely depending on setup
	fallback, err := net.DialTimeout("tcp", fmt.Sprintf("%s:%d", s.FallbackAddr, s.FallbackPort), 3*time.Second)
	if err != nil {
		// Silent drop
		return
	}
	defer fallback.Close()

	// Feed the initial probe content to the fallback web application
	if len(initialBytes) > 0 {
		_, _ = fallback.Write(initialBytes)
	}

	// Pipe the remainder of connection to mask as normal HTTP
	var wg sync.WaitGroup
	wg.Add(2)
	go func() {
		_, _ = io.Copy(fallback, conn)
		wg.Done()
	}()
	go func() {
		_, _ = io.Copy(conn, fallback)
		wg.Done()
	}()
	wg.Wait()
}

func (s *ServerInboundHandler) incrementConns() {
	s.ActiveConnsMu.Lock()
	s.ActiveConns++
	s.ActiveConnsMu.Unlock()
}

func (s *ServerInboundHandler) decrementConns() {
	s.ActiveConnsMu.Lock()
	s.ActiveConns--
	s.ActiveConnsMu.Unlock()
}

func (s *ServerInboundHandler) GetActiveConns() int32 {
	s.ActiveConnsMu.Lock()
	defer s.ActiveConnsMu.Unlock()
	return s.ActiveConns
}
