# Local Install

## Current project location

The generated sources are already in your Codex project folder:

```text
C:\Users\zhult\Documents\Codex\2026-07-31\ni-h-2
```

You can also copy this folder to `F:\project\web3-portal` manually.

## Backend

1. Open `backend` folder in a terminal.
2. Use the local Maven wrapper:
   - Windows: `backend\mvnw.cmd`
3. Recommended quick build check:
   ```powershell
   Set-Location backend
   cmd /c mvnw.cmd -version
   ```
4. Full build:
   ```powershell
   Set-Location backend
   cmd /c mvnw.cmd clean package -DskipTests
   ```

## Frontend

1. Open `frontend` folder in a terminal.
2. Install dependencies:
   ```powershell
   Set-Location frontend
   cmd /c "npm.cmd install"
   ```
3. Start dev server:
   ```powershell
   cmd /c "npm.cmd run dev"
   ```

## Docker services

1. Ensure Docker Desktop is running.
2. Start infrastructure:
   ```powershell
   docker compose up -d mysql redis rabbitmq nacos
   ```
3. Start backend services:
   ```powershell
   docker compose up -d gateway user-service blog-service forum-service shop-service media-service quant-service tool-service software-service resource-service
   ```
4. Frontend production container:
   ```powershell
   docker compose build frontend
   docker compose up -d frontend
   ```

## Notes

- The local JDK 17 and Maven wrapper are included under `backend/.m2`.
- If `npm install` complains about npm cache permission, close editors that may lock `AppData\Local\npm-cache` and rerun it.
- The project already includes WeChat/Alipay sandbox payment stubs and a resource upload/download service.
