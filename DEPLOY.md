# Deploy

## Prerequisites

- Docker & Docker Compose
- Node.js 18+
- JDK 17+
- Maven 3.9+

## Step 1: Start infrastructure

```bash
docker compose up -d mysql redis rabbitmq nacos
```

## Step 2: Build and start backend services

```bash
mvn -f backend/pom.xml clean package -DskipTests
docker compose up -d gateway user-service blog-service forum-service shop-service media-service quant-service tool-service software-service
```

## Step 3: Start frontend

```bash
cd frontend
npm install
npm run dev
```

## Access

- Frontend：http://localhost:5173
- Gateway：http://localhost:8080
- Nacos：http://localhost:8848/nacos
- RabbitMQ：http://localhost:15672

## Notes

- MySQL 端口 3306，用户名 root，密码 root
- 所有微服务通过 Nacos 注册与发现
- 请求统一走 Gateway 8080 端口
