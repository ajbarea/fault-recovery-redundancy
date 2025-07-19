# 🛡️ Fault Recovery with Redundancy

## 📋 Description

This project demonstrates the implementation of **fault recovery architectural tactics** using **active redundancy** in a distributed system. The system implements:

- 🔄 **Active Redundancy**: Multiple service replicas running simultaneously with load balancing
- 🔍 **Fault Detection**: Heartbeat monitoring to detect failed replicas in real-time
- ⚡ **Automatic Recovery**: Docker Swarm's built-in orchestration for replica replacement
- 🔗 **Service Continuity**: Maintaining streaming service availability during failures
- 🖥️ **Cross-Processor Distribution**: Replicas distributed across different containers/processors

## 💡 Motivation

This project simulates a **critical user registration and stream key validation service** where service availability is paramount. The system uses **Docker Swarm** to deploy multiple replicas of a Spring Boot application, implementing **active redundancy** with automatic failover capabilities. When one replica fails, the system automatically detects the failure through **integrated heartbeat monitoring** and routes traffic to healthy replicas without service interruption.

**Non-deterministic failure simulation** is implemented through an integrated `FailureSimulator` service, which can simulate random or controlled failures for testing purposes, replicating real-world scenarios like hardware failures or network partitions.

This project demonstrates:

- 🏗️ The practical implementation of fault recovery tactics in a **Spring Boot microservice architecture**
- 🔄 How to use **active redundancy with automatic failover** in a working distributed system
- ❤️ How **heartbeat monitoring and threshold-based fault detection** can be implemented and tested in a containerized environment
- 🧪 **Integration of chaos engineering** principles through the `FailureSimulator` for comprehensive testing.

## ⚡ Quick Start

### Prerequisites

- Java and Maven
- Docker and Docker Compose

### 1. Build and Start the Fault Recovery System

```bash
# Clone the repository
git clone https://github.com/ajbarea/fault-recovery-redundancy.git
cd fault-recovery-redundancy

# Start all services with active redundancy
docker compose up -d --build
```

This starts:

- 🍃 **Spring Boot application - Primary replica** on port 8080
- 🍃 **Spring Boot application - Secondary replica** on port 8081
- 🗄️ **MySQL database** for shared state
- 🌐 **Frontend interface** on port 5173 (for testing the critical process)

### 2. Verify Fault Recovery System

```bash
# Check that both replicas are healthy
curl http://localhost:8080/heartbeat/status

# Test the critical process (user registration service)
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "testuser", "password": "test123", "confirmPassword": "test123", "email": "test@example.com"}'
```

### 3. Test Fault Recovery

```bash
# Simulate a failure of the primary replica
docker compose stop spring-boot-app-primary

# Verify the system still works with the secondary replica
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "resilient_user", "password": "test123", "confirmPassword": "test123", "email": "resilient@example.com"}'

# Check heartbeat status to see fault detection
curl http://localhost:8081/heartbeat/status
```

You now have a working fault recovery system with active redundancy! 🛡️

## 🏛️ Architecture

### 📊 Technical Diagrams

For detailed technical diagrams, see:

- [Livestreaming Class Diagram](docs/Livestreaming%20Class%20Diagram.md) - Complete system class structure and relationships
- [Failover Flow Diagram](docs/Failover%20Flow%20Diagram.md) - Fault recovery sequence and process flow
- [Swarm Architecture Diagram](docs/Swarm%20Architecture%20Diagram.md) - Docker Swarm deployment architecture
- [Components Architecture PNG](docs/livestreamingarch.png) - High-level system components overview
- [Class Diagram PNG](docs/livestreamingclass.png) - Visual class structure diagram

---

## 💻 Implementation Details

### 📦 Package Structure

The fault recovery system is implemented using the following Java package structure:

- 🚀 **Main Application**: `com.swen755.fault_recovery_redundancy.FaultRecoveryRedundancyApplication`
- ⚙️ **Core Services**:
  - `com.swen755.fault_recovery_redundancy.service.HeartbeatService` - Main fault recovery orchestrator
  - `com.swen755.fault_recovery_redundancy.service.FailureSimulator` - Chaos engineering and testing
  - `com.swen755.fault_recovery_redundancy.service.UserService` - User management and authentication
- 🌐 **REST Controllers**:
  - `com.swen755.fault_recovery_redundancy.controller.HealthController` - Health check endpoints
  - `com.swen755.fault_recovery_redundancy.controller.HeartbeatController` - Heartbeat monitoring API
  - `com.swen755.fault_recovery_redundancy.controller.FailureSimulationController` - Chaos testing endpoints
  - `com.swen755.fault_recovery_redundancy.controller.AuthController` - User registration
  - `com.swen755.fault_recovery_redundancy.controller.StreamController` - Stream validation
- 🔒 **Configuration**: `com.swen755.fault_recovery_redundancy.config.SecurityConfig` - Security and endpoint access

### 🔧 Key Configuration Properties

```properties
# Fault Recovery Configuration
fault.recovery.replica.urls=http://spring-boot-app-primary:8080/health,http://spring-boot-app-secondary:8080/health
fault.recovery.heartbeat.interval=10000
fault.recovery.failure.threshold=3
fault.recovery.recovery.threshold=2
fault.recovery.health.timeout=5000

# Failure Simulation Configuration (for testing)
fault.recovery.simulation.enabled=false
fault.recovery.simulation.failure.probability=0.1
fault.recovery.simulation.recovery.probability=0.3
```

### 🐳 Docker Configuration

- 📄 **Docker Compose**: `compose.yaml` - Single node development setup
- 🐝 **Docker Swarm**: `docker-stack.yml` - Multi-node production deployment with service discovery
- 🩺 **Health Check Endpoints**: All containers expose `/health` endpoints for Docker health checks

---

## 🏗️ How to Run

### 🐳 Docker Compose Setup

```bash
# Clone the repository
git clone https://github.com/ajbarea/fault-recovery-redundancy.git
cd fault-recovery-redundancy

# Start all services with fault recovery
docker compose up -d --build
```

This will start:

- � **Spring Boot application - Primary replica** on port 8080
- 🍃 **Spring Boot application - Secondary replica** on port 8081
- 🗄️ **MySQL database** on port 3306
- 🌐 **Frontend React Application** on port 5173

### 🔍 Verify System Status

```bash
# Check service status
docker compose ps

# View logs for fault recovery monitoring
docker compose logs spring-boot-app-primary

# Check heartbeat status
curl http://localhost:8080/heartbeat/status
```

### 🛑 Stop Services

```bash
# Stop and remove containers
docker compose down

# Complete cleanup (removes volumes)
docker compose down -v
```

### 🐝 Optional: Docker Swarm for Multi-Node Testing

For testing fault recovery across different processors:

```bash
# Initialize Docker Swarm
docker swarm init

# Deploy with redundancy across nodes
docker stack deploy -c docker-stack.yml fault-recovery-demo

# Remove stack when done
docker stack rm fault-recovery-demo
```

---

## ⚡ Fault Recovery Tactics Demonstrated

This project demonstrates the following fault recovery tactics in a distributed streaming system:

- 🔄 **Active Redundancy**: Multiple service replicas running simultaneously with automatic load balancing and failover
- ❤️ **Heartbeat Monitoring**: Continuous health checks of all service replicas to detect failures in real-time
- ⚡ **Automatic Failover**: Docker Swarm orchestration automatically routes traffic away from failed replicas
- 🔍 **Service Discovery**: Dynamic discovery and routing to healthy replicas without manual intervention
- 🛠️ **Self-Healing**: Automatic restart and replacement of failed containers to maintain desired replica count
- 📉 **Graceful Degradation**: System continues to operate with reduced capacity during partial failures
- 🔒 **Fault Isolation**: Container-based isolation prevents failures from cascading across the system

## 🔍 API Endpoints & Fault Recovery Features

| Endpoint | Method | Description | Fault Recovery Feature |
|----------|--------|-------------|------------------------|
| `/health` | GET | Health check endpoint for individual replicas | ✅ Fault Detection |
| `/api/auth/register` | POST | Critical process - user registration service | ✅ Load Balanced across replicas |
| `/heartbeat/status` | GET | Get health status of all application replicas | ✅ System-wide Health Monitoring |
| `/heartbeat/primary` | GET | Get current primary replica information | ✅ Primary Replica Tracking |
| `/health/status` | GET | Get detailed fault recovery status | ✅ Fault Recovery Dashboard |
| `/health/replicas` | GET | Get detailed health status of all replicas | ✅ Replica Health Monitoring |
| `/simulation/enable` | POST | Enable failure simulation for testing | ✅ Fault Testing |
| `/simulation/disable` | POST | Disable failure simulation | ✅ Fault Testing |
| `/simulation/fail/{replicaUrl}` | POST | Manually simulate failure for specific replica | ✅ Controlled Testing |
| `/simulation/recover/{replicaUrl}` | POST | Manually recover replica from simulated failure | ✅ Recovery Testing |
| `/simulation/status` | GET | Get current failure simulation status | ✅ Testing Status |
| `/simulation/reset` | POST | Reset all simulated failures | ✅ Testing Reset |

### 📝 Critical Process Request Format (User Registration)

```json
{
  "username": "your_username",
  "password": "your_password", 
  "confirmPassword": "your_password",
  "email": "your_email@example.com"
}
```

### 📤 Critical Process Response Format

```json
{
  "message": "Registration successful",
  "username": "your_username",
  "email": "your_email@example.com",
  "streamKey": "generated-stream-key"
}
```

## 🧪 Testing Fault Recovery

You can test the fault recovery system by:

1. 🩺 **Health Check**: Verify all replicas are running

   ```bash
   curl http://localhost:8080/heartbeat/status
   ```

2. 🔗 **Service Continuity**: Register a new user during normal operation

   ```bash
   curl -X POST http://localhost:8080/api/auth/register \
     -H "Content-Type: application/json" \
     -d '{
       "username": "testuser",
       "password": "testpass123",
       "confirmPassword": "testpass123", 
       "email": "test@example.com"
     }'
   ```

3. 🎲 **Failure Simulation using FailureSimulator**: Simulate failures for controlled testing

   ```bash
   # Enable failure simulation
   curl -X POST http://localhost:8080/simulation/enable
   
   # Manually simulate failure for a specific replica
   curl -X POST http://localhost:8080/simulation/fail/http-__spring-boot-app-primary-8080_health
   
   # Check simulation status
   curl http://localhost:8080/simulation/status
   ```

4. 💥 **Physical Failure Simulation**: Scale down replicas to simulate actual failure (Docker Swarm only)

   ```bash
   # Simulate failure by reducing replicas
   docker service scale streaming-auth_spring-boot-app-primary=0
   ```

5. 🔍 **Fault Detection**: Verify heartbeat detects the failure

   ```bash
   curl http://localhost:8080/heartbeat/status
   ```

6. 🛡️ **Service Resilience**: Verify service continues to work with remaining replicas

   ```bash
   # Registration should still work with remaining replicas
   curl -X POST http://localhost:8081/api/auth/register \
     -H "Content-Type: application/json" \
     -d '{
       "username": "resilient_user",
       "email": "resilient@example.com"
     }'
   ```

7. 🔄 **Recovery Testing**: Restore full capacity and verify automatic recovery

   ```bash
   # For simulation-based failures:
   curl -X POST http://localhost:8080/simulation/recover/http-__spring-boot-app-primary-8080_health
   curl -X POST http://localhost:8080/simulation/disable
   
   # Reset all simulated failures (optional)
   curl -X POST http://localhost:8080/simulation/reset
   
   # For Docker Swarm failures:
   docker service scale streaming-auth_spring-boot-app-primary=1
   
   # Verify recovery
   curl http://localhost:8080/heartbeat/status
   ```
