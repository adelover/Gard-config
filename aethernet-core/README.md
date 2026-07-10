# AetherNet: Next-Generation Censorship Circumvention Protocol

AetherNet is an ultra-secure, highly efficient, and low-latency network proxy suite. It introduces the **Aether-Flow** protocol, designed from the ground up to resist state-of-the-art traffic analysis, passive and active probing, TLS fingerprinting, and replay attacks.

## Core Features

1. **Aether-Flow Protocol**: A stateless, asymmetric-encrypted transport protocol.
   - **Silent Active Probing Rejection**: The server implements an authenticating handshake using X25519 key-exchange and HMAC tokens. If a scanner or probe sends unauthorized bytes, the server **remains completely silent** and drops the packets. There are no RST packets or recognizable TLS errors to analyze.
   - **No TLS Fingerprinting**: Traditional proxies can be detected via client hello fingerprint patterns (e.g., JA3/JA4). AetherNet bypasses this entirely by using raw TCP, UDP, or customizable web sockets styled as standard HTTPS traffic, wrapping the payload in a custom encrypted envelope.

2. **AI-Driven Traffic Obfuscation Middleware**:
   - Analyzes packet sequence, sizes, and delays.
   - **Dynamic Mutation**: Intercepts packets and mutates their size (by adding cryptographically random padding up to maximum MTU) and inserts minute microsecond delays.
   - **Presets**: Mimics specific traffic shapes like **Video Streaming** (frequent bursty large packets), **Web Browsing/API calls** (transactional request-response spikes), and **Online Gaming** (continuous, fast, low-byte streams).

3. **Embedded Admin API & Web Panel**:
   - Features a lightweight, secure HTTP REST server written entirely in pure Go.
   - Tracks live system telemetries: CPU%, RAM (bytes allocated), current active connections, and separate Inbound/Outbound speedometers.
   - Generates unified, shareable **Subscription Links** (under the `aethernet://` schema).

4. **Resource-Optimized**:
   - Implements a modern `sync.Pool` buffer recycler to achieve zero allocation loops, making it extremely suitable for mobile devices (running in Termux or an Android background service) and budget VPS instances.

---

## Technical File Layout

- `main.go`: Main orchestrator, parses CLI commands, boots services.
- `config.json`: Multi-user configuration file featuring user tables, fallback destinations, dynamic ports, and obfuscation profiles.
- `protocol/handshake.go`: Cryptographic handshake implementing X25519, HMAC authentication, and silent dropping.
- `protocol/handler.go`: Low-level TCP proxy piping with asynchronous channel-backed loops.
- `obfuscator/obfuscator.go`: Traffic mutation middleware simulating streaming, gaming, and web behaviors.
- `api/admin.go`: Embedded REST service with secure bearer tokens, serving subscription managers and system health metrics.

---

## How to Compile & Run

### Prerequisites
- Go 1.20 or newer.

### Build the Core
```bash
# Clone the repository and navigate to the folder
cd aethernet-core

# Build the binary
go build -o aethernet main.go
```

### Start the Server
Create your `config.json` and start the server:
```bash
./aethernet --mode server --config config.json
```

### Start the Client
Run AetherNet in client mode to tunnel traffic:
```bash
./aethernet --mode client --config config.json --socks5 127.0.0.1:10808
```

## Protocol Frame Layout (Aether-Flow)

Each packet sent across Aether-Flow contains the following structured byte envelope:

```
+-------------------+--------------------+------------------+-----------------------+
|  HMAC-SHA256 (32) | Client Ephemeral   | Encrypted Header |  Encrypted Payload    |
|  (Auth Token)     | X25519 Key (32)    | (IV + Length)    |  (Variable + Padding) |
+-------------------+--------------------+------------------+-----------------------+
```

1. **HMAC-SHA256 (32 bytes)**: Computed using the Pre-Shared Key (PSK) over the Ephemeral Public Key and timestamp. If the timestamp varies by more than 120 seconds or the HMAC is invalid, the packet is silently discarded.
2. **Client Ephemeral Public Key (32 bytes)**: Used to derive a session symmetric key via ECDH (X25519).
3. **Encrypted Header**: AES-256-GCM encrypted metadata containing destination IP, destination Port, and Payload Length.
4. **Encrypted Payload + Padding**: AES-256-GCM encrypted application stream padded to fit the dynamic obfuscation shape.
