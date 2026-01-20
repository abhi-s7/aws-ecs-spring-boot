# AWS ECS Spring Boot Integration

A Spring Boot microservice demonstrating deployment on Amazon ECS with Fargate. Gift card purchase tracking service with health checks, container orchestration, and Application Load Balancer integration.

---

## AWS ECS Setup Guide

Before deploying this application, set up AWS ECS cluster and container infrastructure.

**Complete setup instructions: [AWS ECS Guide](AWS-ECS-Guide.md)**

**The guide covers:**
*   What is Amazon ECS and its use cases
*   ECS Cluster creation with Fargate
*   ECR repository and Docker image deployment
*   Task definitions and ECS services
*   Application Load Balancer configuration
*   Troubleshooting common issues

---

## Features

*   Gift Card Purchase API (CRUD operations)
*   Track gift cards by type (Amazon, Starbucks, McDonalds, etc.)
*   Calculate total purchases and amounts
*   Health check endpoints for load balancer
*   In-memory data store
*   Container-ready with Docker multi-stage build
*   Horizontal scaling with ECS

**Access**: `http://<alb-dns-name>`

---

## Architecture

**Backend** (Spring Boot):
*   `HealthController.java` - Root and health check endpoints
*   `GiftCardController.java` - Gift card purchase CRUD REST API
*   `GiftCard.java` - Gift card model (userName, giftCardType, amount, date)
*   `AwsEcsIntegrationWithSpringBootApplication.java` - Main application

**Container**:
*   `Dockerfile` - Multi-stage build (Maven build + JRE runtime)
*   `.dockerignore` - Excludes unnecessary files from image
*   `build-and-push.sh` - Automated ECR deployment script

**Configuration**:
*   `application.yaml` - Server port 8080, actuator endpoints
*   `pom.xml` - Spring Boot 3.5.9, Java 21, Lombok

**API Endpoints:**
*   `GET /` - Application info with hostname and IP (shows which container is serving)
*   `GET /health` - Health check for ALB target group
*   `GET /api/giftcards` - List all gift card purchases
*   `GET /api/giftcards/{id}` - Get gift card by ID
*   `POST /api/giftcards` - Create new gift card purchase
*   `PUT /api/giftcards/{id}` - Update gift card
*   `DELETE /api/giftcards/{id}` - Delete gift card
*   `GET /api/giftcards/count` - Total count with timestamp
*   `GET /api/giftcards/total` - Total amount and purchase count
*   `GET /api/giftcards/by-type/{type}` - Filter by gift card type

---

## Local Setup

**1. Clone the repository**
```bash
git clone <repository-url>
cd aws-ecs-spring-boot
```

**2. Build and run**
```bash
./mvnw clean install
./mvnw spring-boot:run
```

Access at `http://localhost:8080`

**3. Test endpoints**
```bash
# Home page with container info
curl http://localhost:8080/

# Health check
curl http://localhost:8080/health

# Get all gift cards
curl http://localhost:8080/api/giftcards

# Create gift card purchase
curl -X POST http://localhost:8080/api/giftcards \
  -H "Content-Type: application/json" \
  -d '{"userName":"Abhishek Kumar","giftCardType":"Amazon","amount":100.00,"date":"2026-01-20"}'

# Get total amount
curl http://localhost:8080/api/giftcards/total
```

---

## Docker Build (Local Testing)

**Build Docker image:**
```bash
docker build -t aws-ecs-spring-boot .
```

**Run container locally:**
```bash
docker run -p 8080:8080 aws-ecs-spring-boot
```

Access at `http://localhost:8080`

---

## API Response Examples

**GET /**
```json
{
  "message": "Welcome to Gift Card Purchase Service",
  "description": "ECS Demo - Track and manage gift card purchases",
  "application": "aws-ecs-spring-boot",
  "status": "running",
  "timestamp": "2026-01-20T10:30:45.123",
  "hostname": "ip-172-31-45-123",
  "ip": "172.31.45.123"
}
```

**GET /api/giftcards**
```json
[
  {
    "id": 1,
    "userName": "Abhishek Kumar",
    "giftCardType": "Amazon",
    "amount": 150.00,
    "date": "2026-01-20"
  },
  {
    "id": 2,
    "userName": "John Doe",
    "giftCardType": "Starbucks",
    "amount": 50.00,
    "date": "2026-01-20"
  }
]
```

**GET /api/giftcards/total**
```json
{
  "totalAmount": 225.00,
  "totalPurchases": 3,
  "timestamp": "2026-01-20T10:30:45.123"
}
```

---

## Tech Stack

*   Spring Boot 3.5.9, Java 21, Maven
*   Lombok, Spring Boot Actuator
*   Docker (Multi-stage build with AMD64 architecture)
*   Amazon ECS (Fargate)
*   Amazon ECR (Container Registry)
*   Application Load Balancer (ALB)

---

## ECS Deployment

The application runs on Amazon ECS with Fargate (serverless containers) and Application Load Balancer for traffic distribution.

**Fargate**: AWS manages all infrastructure, no server management needed

**ALB**: Distributes traffic across multiple containers, health checks on `/health`

**Benefit**: Fully managed, auto-scaling, highly available containerized application

---

### Automated Deployment (Recommended)

**Prerequisites:**
*   AWS CLI installed and configured (`aws configure`)
*   Docker Desktop running
*   ECR repository created

**Deploy:**
```bash
# Build and push to ECR
cp build-and-push.sh.example build-and-push.sh
nano build-and-push.sh  # Update AWS_ACCOUNT_ID
chmod +x build-and-push.sh
./build-and-push.sh
```

Then follow **[AWS ECS Guide](AWS-ECS-Guide.md)** to create cluster, task definition, and service.

---

### Manual Deployment Steps

See **[AWS ECS Guide](AWS-ECS-Guide.md)** for complete UI walkthrough.

**Quick CLI reference:**

```bash
# 1. Create ECR repository
aws ecr create-repository \
    --repository-name aws-ecs-spring-boot \
    --region us-west-1

# 2. Build and push (use script for proper architecture)
./build-and-push.sh

# 3. Get ALB DNS after service creation
aws elbv2 describe-load-balancers \
    --names spring-boot-alb \
    --query 'LoadBalancers[0].DNSName' \
    --output text
```

**Note**: Use the script instead of manual docker commands - it includes `--platform linux/amd64` flag required for Apple Silicon (M1/M2 Mac) compatibility with Fargate.

---

### Update Application

**1. Make code changes**

**2. Rebuild and push:**
```bash
./build-and-push.sh
```

**3. Force new deployment:**
```bash
# Using script
cp update-service.sh.example update-service.sh
nano update-service.sh  # Update cluster and service names
chmod +x update-service.sh
./update-service.sh

# Or using CLI directly
aws ecs update-service \
    --cluster SpringBootCluster \
    --service spring-boot-service \
    --force-new-deployment \
    --region us-west-1
```

---

### Service Scaling

```bash
# Scale up to 3 containers
aws ecs update-service \
    --cluster SpringBootCluster \
    --service spring-boot-service \
    --desired-count 3 \
    --region us-west-1

# Scale down to 0
aws ecs update-service \
    --cluster SpringBootCluster \
    --service spring-boot-service \
    --desired-count 0 \
    --region us-west-1
```

**Verify load balancing**: Refresh browser to see different container IPs in response

---

### View Logs

```bash
# List recent log streams
aws logs describe-log-streams \
    --log-group-name /ecs/spring-boot-task \
    --order-by LastEventTime \
    --descending \
    --max-items 5 \
    --region us-west-1
```

---

## Troubleshooting

See **[AWS ECS Guide - Troubleshooting](AWS-ECS-Guide.md#9-troubleshooting-common-issues)** for detailed solutions.

**Task fails with "CannotPullContainerError - platform mismatch":**
*   **Cause**: Docker image built for ARM64 (M1/M2 Mac) instead of AMD64
*   **Fix**: Script includes `--platform linux/amd64` flag
*   **Action**: Rebuild with `./build-and-push.sh` and force new deployment

**Listener shows "Not reachable" on port 80:**
*   **Cause**: ALB security group missing inbound rule for HTTP port 80
*   **Fix**:
    1. EC2 → Security Groups → Select ALB security group
    2. Edit inbound rules → Add rule: Type HTTP, Port 80, Source 0.0.0.0/0
    3. Save rules

**Target group shows unhealthy:**
*   Check ECS task security group allows traffic on port 8080 from ALB security group
*   Verify health check path is `/health` in target group
*   Check CloudWatch logs for application errors

**Check service status:**
```bash
aws ecs describe-services \
    --cluster SpringBootCluster \
    --services spring-boot-service \
    --region us-west-1 \
    --query 'services[0].events[:5]'
```

**View stopped tasks:**
```bash
aws ecs list-tasks \
    --cluster SpringBootCluster \
    --desired-status STOPPED \
    --region us-west-1
```

---

## Cleanup

**Via Console:**
1. **Delete service**: Services → Select → Delete → Force delete
2. **Delete load balancer**: EC2 → Load Balancers → Delete
3. **Delete target group**: EC2 → Target Groups → Delete
4. **Delete cluster**: Clusters → Delete cluster
5. **Delete ECR repository**: ECR → Repositories → Delete

**Via CLI:**
```bash
# Delete service
aws ecs delete-service \
    --cluster SpringBootCluster \
    --service spring-boot-service \
    --force \
    --region us-west-1

# Wait for service deletion, then delete cluster
aws ecs delete-cluster \
    --cluster SpringBootCluster \
    --region us-west-1

# Delete ECR images and repository
aws ecr batch-delete-image \
    --repository-name aws-ecs-spring-boot \
    --image-ids imageTag=latest \
    --region us-west-1

aws ecr delete-repository \
    --repository-name aws-ecs-spring-boot \
    --force \
    --region us-west-1
```

---

For detailed AWS Console walkthrough and step-by-step instructions, see **[AWS ECS Guide](AWS-ECS-Guide.md)**.
