# AWS ECS Setup Guide

Step-by-step AWS Console walkthrough for deploying the Spring Boot application on Amazon ECS with Fargate.

**For application features, local setup, and CLI commands, see [README.md](README.md)**

---

## What is Amazon ECS?

**Amazon ECS (Elastic Container Service)** is AWS's fully managed container orchestration service for running Docker containers. ECS eliminates the need to manage container orchestration software and integrates deeply with AWS services like ECR, IAM, CloudWatch, and Application Load Balancers. Common uses include microservices, batch processing, web applications, and CI/CD pipelines.

---

## 1. Create ECS Cluster

ECS clusters are logical groupings where your containerized applications run.

**Steps:**

1. Go to **ECS** → Enable **New ECS experience** (top left if prompted).
2. Click **Clusters** → **Create cluster**.
3. **Cluster name**: `DemoCluster` (or your preferred name).
4. **Default namespace**: Leave as default.

---

## 2. Configure Infrastructure Options

Choose how to obtain compute capacity for your cluster:

**Option 1: Fargate only (Recommended for beginners)**
*   Serverless - AWS manages all servers
*   No instance management needed
*   Great for most common workloads

**Option 2: Fargate and Managed Instances**
*   Amazon ECS manages patching and scaling
*   More control over instance types
*   Great for advanced workloads

**Option 3: Fargate and Self-managed instances**
*   You manage instances (patching, scaling)
*   Full control over infrastructure
*   Can use Reserved Instances for cost savings

**For this guide**: Select **Option 1: Fargate only** (simplest setup)

---

## 3. Create Cluster

1. Click **Create**.
2. Wait for cluster creation to complete (1-2 minutes).

**What gets created:**
*   ECS cluster with Fargate capacity provider
*   Default CloudWatch log group
*   AWS Fargate infrastructure (serverless)

---

## 4. Deploy Spring Boot Application to ECS

### 4.1 Create ECR Repository

1. Go to **AWS Console** → **ECR** (Elastic Container Registry)
2. Click **Create repository**
3. **Visibility**: Private
4. **Repository name**: `aws-ecs-spring-boot`
5. **Tag immutability**: Mutable
6. **Encryption**: AES-256
7. Click **Create repository**
8. **Copy** your AWS Account ID from the URI shown

---

### 4.2 Build and Push Docker Image

1. **Update build script:**
```bash
cp build-and-push.sh.example build-and-push.sh
nano build-and-push.sh
```

2. **Edit these values:**
   - `AWS_ACCOUNT_ID="your-account-id"` (from ECR)
   - `AWS_REGION="us-west-1"` (your region)
   - `ECR_REPOSITORY="aws-ecs-spring-boot"`

3. **Make executable and run:**
```bash
chmod +x build-and-push.sh
./build-and-push.sh
```

**Important:** The script builds for `linux/amd64` architecture (required for AWS Fargate). If you're on Apple Silicon (M1/M2 Mac), this ensures compatibility.

4. **Verify** image in ECR console and copy the Image URI

---

### 4.3 Create Task Definition

1. Go to **ECS** → **Task Definitions** → **Create new task definition**
2. **Task definition family**: `spring-boot-task`
3. **Launch type**: AWS Fargate only (uncheck EC2)
4. **Operating system**: Linux/X86_64
5. **Task size**:
   - **CPU**: 0.5 vCPU
   - **Memory**: 1 GB
6. **Task execution role**: Leave default (auto-creates `ecsTaskExecutionRole`)
7. **Container - 1**:
   - **Name**: `spring-boot-container`
   - **Image URI**: `<account-id>.dkr.ecr.us-west-1.amazonaws.com/aws-ecs-spring-boot:latest` (from ECR)
   - **Essential container**: Yes
   - **Port mappings**:
     - Container port: `8080`
     - Protocol: TCP
     - App protocol: HTTP
8. Click **Create**

---

### 4.4 Create ECS Service with Load Balancer

1. Go to **Clusters** → **SpringBootCluster** → **Services** → **Create**
2. **Deployment configuration**:
   - **Task definition**: `spring-boot-task` (latest)
   - **Service name**: `spring-boot-service`
   - **Desired tasks**: `1`
3. **Networking**:
   - **VPC**: Default VPC
   - **Subnets**: Select all 3 subnets
   - **Security group**: Create new
     - **Inbound rule**: Type Custom TCP, Port `8080`, Source 0.0.0.0/0
   - **Public IP**: Turn ON
4. **Load balancing**:
   - **Load balancer type**: Application Load Balancer
   - **Create new load balancer**:
     - Name: `spring-boot-alb`
   - **Listener**: Create new
     - Port: `80`, Protocol: HTTP
   - **Target group**: Create new
     - Name: `spring-boot-tg`
     - Port: `8080`
     - Health check path: `/health`
5. Click **Create**

**Why different ports?**
- Port 80 (Load Balancer): What users access from internet
- Port 8080 (Target Group): Where Spring Boot listens in container
- Load balancer forwards traffic: Port 80 → Port 8080

---

### 4.5 Configure ALB Security Group

**Important**: After service creation, configure ALB security group:

1. Go to **EC2** → **Load Balancers** → Select `spring-boot-alb`
2. **Security** tab → Note security group ID
3. Go to **Security Groups** → Select ALB security group
4. **Edit inbound rules** → Add rule:
   - Type: HTTP
   - Port: 80
   - Source: 0.0.0.0/0
5. Save rules

---

### 4.6 Get Application URL and Test

1. Wait 3-5 minutes for service to deploy
2. Go to **EC2** → **Load Balancers**
3. Find `spring-boot-alb`
4. Copy **DNS name** (e.g., `spring-boot-alb-123.us-west-1.elb.amazonaws.com`)
5. Open browser: `http://<dns-name>/`

**Test endpoints:**
```bash
# Home page
curl http://<dns-name>/

# Gift cards API
curl http://<dns-name>/api/giftcards

# Health check
curl http://<dns-name>/health

# Create gift card
curl -X POST http://<dns-name>/api/giftcards \
  -H "Content-Type: application/json" \
  -d '{"userName":"John Doe","giftCardType":"Amazon","amount":100.00,"date":"2026-01-20"}'

# Get total
curl http://<dns-name>/api/giftcards/total
```

**Verify**: You should see the Gift Card Purchase Service welcome page

---

## 5. Update Application

When you make code changes:

1. **Rebuild and push:**
```bash
./build-and-push.sh
```

2. **Force new deployment** (Console):
   - Go to service → **Update service**
   - Check **"Force new deployment"**
   - Click **Update**
   - Wait 2-3 minutes for new tasks to start

Or use CLI (see [README](README.md#update-application))

---

## 6. Monitor and View Logs

**View Service Status:**
1. Go to your cluster → Services → `spring-boot-service`
2. Check: Desired tasks: 1, Running tasks: 1, Status: Active
3. Click **Target group** → Verify health status: Healthy

**View Application Logs:**
1. Go to **CloudWatch** → **Log groups**
2. Find `/ecs/spring-boot-task`
3. Click log stream to view application logs

**View Task Details:**
1. Service → **Tasks** tab → Click task ID
2. See: Container details, private IP, logs, task revision

---

## 7. Scale Service

**Scale Up (to 3 tasks):**
1. Service → **Update service**
2. **Desired tasks**: Change to `3`
3. Click **Update**
4. Refresh browser → IP address changes (load balancer distributing traffic)

**Scale Down (to 0 tasks):**
1. Service → **Update service**
2. **Desired tasks**: Change to `0`
3. Click **Update**

---

## 8. ECS Service Auto Scaling

**Three Scaling Metrics:**
1. **ECS Service CPU Utilization**
2. **ECS Service Memory Utilization**
3. **ALB Request Count Per Target**

**Three Scaling Types:**
1. **Target Tracking**: Maintain specific target (e.g., CPU at 70%)
2. **Step Scaling**: Scale based on CloudWatch alarm thresholds
3. **Scheduled Scaling**: Scale ahead of predictable changes

**Note**: Fargate auto scaling is simpler (serverless). EC2 launch type requires scaling both tasks AND instances.

---

## 9. Troubleshooting Common Issues

**Task fails with "CannotPullContainerError - platform mismatch":**
*   Image built for wrong architecture (ARM64 instead of AMD64)
*   Solution: Script includes `--platform linux/amd64` flag
*   Rebuild: `./build-and-push.sh` and force new deployment

**Listener shows "Not reachable" on port 80:**
*   ALB security group missing inbound rule for HTTP port 80
*   Fix: EC2 → Security Groups → Select ALB security group → Edit inbound rules
*   Add rule: Type HTTP, Port 80, Source 0.0.0.0/0 → Save

**Target group shows unhealthy:**
*   Check ECS task security group allows traffic on port 8080 from ALB security group
*   Verify health check path is `/health`
*   Check CloudWatch logs for application errors

**For more troubleshooting and CLI commands, see [README.md](README.md#troubleshooting)**

---

## 10. Cleanup

1. **Delete service**: Services → Select → Delete → Force delete
2. **Delete load balancer**: EC2 → Load Balancers → Delete
3. **Delete target group**: EC2 → Target Groups → Delete
4. **Delete cluster**: Clusters → Delete cluster
5. **Delete ECR repository**: ECR → Repositories → Delete

---
