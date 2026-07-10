package obfuscator

import (
	"crypto/rand"
	"math/big"
	"time"
)

// ObfuscationMode defines the target traffic shape preset
type ObfuscationMode string

const (
	ModeNone           ObfuscationMode = "none"
	ModeVideoStreaming ObfuscationMode = "video_streaming" // Brisk, packet-burst sequences
	ModeOnlineGaming   ObfuscationMode = "online_gaming"    // Fast, uniform heartbeat signals
	ModeWebBrowsing    ObfuscationMode = "web_browsing"     // Large initial request, decaying spikes
)

// Obfuscator is a middleware layer that mutates packet sizes and delays
type Obfuscator struct {
	Mode ObfuscationMode
}

// NewObfuscator instantiates an obfuscator instance
func NewObfuscator(mode string) *Obfuscator {
	m := ObfuscationMode(mode)
	switch m {
	case ModeVideoStreaming, ModeOnlineGaming, ModeWebBrowsing:
		return &Obfuscator{Mode: m}
	default:
		return &Obfuscator{Mode: ModeNone}
	}
}

// MutatePacket mutates the payload size by adding custom padding and pauses the thread to emulate the configured traffic style
func (o *Obfuscator) MutatePacket(payload []byte) (mutated []byte, delay time.Duration) {
	if o.Mode == ModeNone {
		return payload, 0
	}

	var paddingSize int
	var pauseTime time.Duration

	switch o.Mode {
	case ModeVideoStreaming:
		// Video streaming typically has larger frame bursts. Align packets up to 1440 bytes.
		if len(payload) < 1300 {
			paddingSize = 1300 - len(payload)
		} else if len(payload) < 1440 {
			paddingSize = 1440 - len(payload)
		}
		// Small jitter delay between 2 to 10 milliseconds
		n, _ := rand.Int(rand.Reader, big.NewInt(8))
		pauseTime = time.Duration(2+n.Int64()) * time.Millisecond

	case ModeOnlineGaming:
		// Gaming sends constant flow of small packets. Frame pad to multiples of 128 bytes.
		remainder := len(payload) % 128
		if remainder > 0 {
			paddingSize = 128 - remainder
		}
		// Low latency heartbeat delays: 1 to 3 milliseconds
		n, _ := rand.Int(rand.Reader, big.NewInt(3))
		pauseTime = time.Duration(1+n.Int64()) * time.Millisecond

	case ModeWebBrowsing:
		// Web browsing has randomized chunk packets. Pad with a random number up to 512 bytes.
		n, _ := rand.Int(rand.Reader, big.NewInt(256))
		paddingSize = int(n.Int64())
		// Highly dynamic pauses resembling loading dependencies (10 to 45 milliseconds)
		m, _ := rand.Int(rand.Reader, big.NewInt(35))
		pauseTime = time.Duration(10+m.Int64()) * time.Millisecond
	}

	if paddingSize > 0 {
		padding := make([]byte, paddingSize)
		_, _ = rand.Read(padding)
		mutated = append(payload, padding...)
	} else {
		mutated = payload
	}

	return mutated, pauseTime
}
