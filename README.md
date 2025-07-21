# Fault-Tolerant Livestreaming Authentication System

## Table of Contents

- [Overview](#overview)
- [Goals](#goals)
- [Technologies](#technologies)
- [Project Structure](#project-structure)
- [Deploy via Docker Swarm](#deploy-via-docker-swarm)
- [Heartbeat Monitoring](#heartbeat-monitoring)
- [Chaos Simulation](#chaos-simulation)
- [Sample Outputs](#sample-outputs)
- [Test the System](#test-the-system)
- [Architecture](#architecture)
  - [Legacy Architecture](#legacy-architecture)
  - [Extended Fault-Tolerant Architecture](#extended-fault-tolerant-architecture)
  - [Mermaid: Deployment Components](#mermaid-deployment-components)
- [Class Diagram](#class-diagram)
- [Sequence Diagram](#sequence-diagram)
- [Security Tactics](#security-tactics)
- [API Endpoints](#api-endpoints)
- [Cleanup](#cleanup)

---

## Overview

This project enhances the [authentication and authenticate livestreaming system](https://github.com/ajbarea/authentication-and-authorization) by introducing redunancy and fault recovery using container replication and monitoring. The system features:

- **Spring Boot** service for authentication and stream key management  
- **NGINX RTMP** server for ingesting livestreams  
- **.NET-based Heartbeat Monitor** for real-time health checks  
- **Chaos Injection Tool** (C#) to test system resilience  
- **Docker Swarm** to orchestrate and replicate services  

---

## Goals

- Ensure continuous uptime for authentication and streaming services  
- Minimize downtime using replication and Swarm failover  
- Monitor service health and log failures/restarts  
- Validate resilience through chaos engineering  

---

## Technologies

- **Java (Spring Boot)** – Auth service API and Streaming key Authentication 
- **NGINX RTMP** – Livestream input & HLS output  
- **C# / .NET Core** – Heartbeat Monitor & Chaos Testing  
- **MySQL** – User information and streaming keys
- **Docker Swarm** – Orchestration and load balancing  
- **ffmpeg / OBS** – Streaming input  
- **Docker.DotNet** – .NET API to interact with Docker /  Docker Swarm

---

## Project Structure

```
├── app/              # Spring Boot Auth service
├── choas/            # Chaos engineering simulation (C#)
├── monitor/          # Heartbeat monitor (C#)
├── nginx-rtmp/       # NGINX RTMP config + Dockerfile
├── docker-stack.yml  # Full swarm deployment
├── compose.yaml      # Local dev (optional)
├── docs/             # Architecture & test screenshots
```

---

## Deploy via Docker Swarm

### 1. Build Images

```bash
docker build -t spring-boot-app:latest ./app
docker build -t nginx-rtmp-server:latest ./nginx-rtmp
```

### 2. Initialize Swarm (if needed)

```bash
docker swarm init
```

### 3. Deploy Stack

```bash
docker stack deploy -c docker-stack.yml mystack
```

Check all replicas are up:

```bash
docker service ls
```

---

## Heartbeat Monitoring

The monitor service:

- Checks Spring Boot and RTMP replicas  
- Uses Docker.DotNet to detect container status  
- Reports container restarts, up/down transitions  
- Logs status continuously

1. Change directory into the monitor folder
```bash
   cd monitor
```

2. Run the heartbeat monitoring system
```bash
   dotnet run
```

---

## Chaos Simulation

The `choas` tool simulates service failure scenarios:

- Stops containers  
- Disrupts networks  
- Stresses CPU or memory  
- Injects latency or kills the RTMP/auth stack  

### Run Chaos Test:

```bash
cd choas
dotnet run
```

---

## Sample Outputs

### Chaos Attack Log

![sample_choas](./docs/choas_sample.png)

### Container Recovery (via Docker Swarm)

![sample_restart](./docs/container_restarting.png)

---

## Test the System

### 1. Register a User

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "yolo2",
    "password": "supersecret",
    "confirmPassword": "supersecret",
    "email": "email@example.com"
  }'
```

### 2. Stream with OBS or ffmpeg

```bash
ffmpeg -f lavfi -i testsrc2=size=1280x720:rate=30 \
  -f lavfi -i sine=frequency=1000:sample_rate=44100 \
  -c:v libx264 -preset veryfast -c:a aac -f flv \
  rtmp://localhost/live/<your-stream-key>
```

### 3. View Stream (HLS)

```bash
http://localhost:9090/live/stream_<your-username>/index.m3u8
```

---

## Architecture

### Legacy Architecture

![livestreamingarch.png](./docs/livestreamingarch.png)

### Extended Fault-Tolerant Architecture

![faultRecoveryExtended](./docs/failover_extended.png)

### Mermaid: Deployment Components

```mermaid
graph TD
    Client[Streaming Client] -->|RTMP Stream| LB[Docker Swarm Routing Mesh]
    Viewer[Stream Viewer] -->|HLS Stream| LB
    Admin[System Admin] -->|Monitor| LB
    
    subgraph Docker Swarm
        LB -->|RTMP| RTMP1[NGINX RTMP 1]
        LB -->|RTMP| RTMP2[NGINX RTMP 2]
        LB -->|HTTP| APP1[Spring Boot App 1]
        LB -->|HTTP| APP2[Spring Boot App 2]
        LB -->|HTTP| HB[Heartbeat Service]
        
        RTMP1 -->|Auth Check| APP1
        RTMP1 -->|Auth Check| APP2
        RTMP2 -->|Auth Check| APP1
        RTMP2 -->|Auth Check| APP2
        
        HB -->|Health Check| APP1
        HB -->|Health Check| APP2
        
        APP1 -->|Data| DB[MySQL]
        APP2 -->|Data| DB
        
        RTMP1 -.->|Shared Storage| VOL[Shared Volume]
        RTMP2 -.->|Shared Storage| VOL
    end
```

---

## Class Diagram

```mermaid
classDiagram
   class RegisterRequest {
      +String username
      +String password
      +String confirmPassword
      +String email
   }

   class User {
      +UUID id
      +String username
      +String password
      +String email
      +String streamKey
      +boolean active
      +generateStreamKey()
   }

   class UserRepository {
      +findByUsername(String)
      +findByStreamKey(String)
   }

   class UserService {
      -UserRepository userRepository
      -PasswordEncoder passwordEncoder
      +register(RegisterRequest)
      +findByStreamKey(String)
   }

   class AuthController {
      -UserService userService
      +register(RegisterRequest)
   }

   class StreamController {
      -UserService userService
      +start(String)
      +stop(String)
      +redirect()
   }

   class HealthController {
      +healthCheck()
   }

   RegisterRequest --> UserService : used by
   UserService --> UserRepository
   UserService --> User
   AuthController --> UserService
   StreamController --> UserService
```

---

## Sequence Diagram

```mermaid
sequenceDiagram
    participant Client as Streaming Client
    participant Swarm as Docker Swarm
    participant RTMP1 as NGINX RTMP 1
    participant RTMP2 as NGINX RTMP 2
    participant App1 as Spring Boot App 1
    participant App2 as Spring Boot App 2
    participant HB as Heartbeat Service
    participant Admin as System Admin

    Client->>Swarm: RTMP Stream
    Swarm->>RTMP1: Route Stream
    RTMP1->>App1: Validate Stream Key
    App1->>RTMP1: Authorized
    RTMP1->>Swarm: Stream Processing

    Note over App1: App1 Fails

    HB->>App1: Health Check
    App1--xHB: No Response
    HB->>HB: Mark App1 as DOWN
    HB->>Admin: Report Status Change

    Client->>Swarm: Continued Streaming
    Swarm->>RTMP1: Route Stream
    RTMP1->>App1: Validate Stream Key
    App1--xRTMP1: No Response
    RTMP1->>App2: Validate Stream Key (Failover)
    App2->>RTMP1: Authorized
    RTMP1->>Swarm: Stream Processing Continues

    Note over App1: App1 Recovers

    HB->>App1: Health Check
    App1->>HB: Healthy Response
    HB->>HB: Mark App1 as UP
    HB->>Admin: Report Status Change

    Note over Swarm: Load Balancing Resumes
```

---

## Security Tactics

- Stream key-based authentication  
- NGINX `on_publish` and `on_publish_done` for stream auth  
- BCrypt password hashing  
- Auth service secured via Spring Security  
- HLS view is public; RTMP ingest is authenticated  

---

## API Endpoints

| Endpoint                  | Method | Description                           |
|--------------------------|--------|---------------------------------------|
| `/health`                | GET    | Basic system health check             |
| `/api/auth/register`     | POST   | Create new user + stream key          |
| `/api/stream/start`      | POST   | NGINX `on_publish` validation         |
| `/api/stream/stop`       | POST   | NGINX `on_publish_done` cleanup       |
| `/heartbeat/status`      | GET    | Get all replica statuses              |

---

## Cleanup

```bash
docker stack rm mystack
docker volume prune
```
