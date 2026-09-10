@echo off
chcp 65001 >nul
set JAVA_HOME=F:\tools\jdk17
cd /d F:\project\my-blog\web3-blog\backend

echo Starting Redis...
start "Redis" /B F:\tools\redis\redis-server.exe F:\tools\redis\redis.windows.conf

echo Starting services...

start "Gateway"    /B java -jar gateway/target/gateway-1.0.0.jar       > target\logs\svc_gateway.log 2>&1
start "User"       /B java -jar user-service/target/user-service-1.0.0.jar      > target\logs\svc_8081.log 2>&1
start "Blog"       /B java -jar blog-service/target/blog-service-1.0.0.jar      > target\logs\svc_8082.log 2>&1
start "Forum"      /B java -jar forum-service/target/forum-service-1.0.0.jar    > target\logs\svc_8083.log 2>&1
start "Shop"       /B java -jar shop-service/target/shop-service-1.0.0.jar      > target\logs\svc_8084.log 2>&1
start "Media"      /B java -jar media-service/target/media-service-1.0.0.jar    > target\logs\svc_8085.log 2>&1
start "Quant"      /B java -jar quant-service/target/quant-service-1.0.0.jar    > target\logs\svc_8086.log 2>&1
start "Tool"       /B java -jar tool-service/target/tool-service-1.0.0.jar      > target\logs\svc_8087.log 2>&1
start "Software"   /B java -jar software-service/target/software-service-1.0.0.jar  > target\logs\svc_8088.log 2>&1
start "Resource"   /B java -jar resource-service/target/resource-service-1.0.0.jar  > target\logs\svc_8089.log 2>&1
start "AIP"        /B java -jar ai-proxy-service/target/ai-proxy-service-1.0.0.jar > target\logs\svc_8093.log 2>&1
start "Jarvis"     /B java -jar jarvis-service/target/jarvis-service-1.0.0.jar    > target\logs\svc_jarvis.log 2>&1
start "Admin"      /B java -jar admin-service/target/admin-service-1.0.0.jar     > target\logs\svc_admin.log 2>&1

echo Waiting for services to start...
timeout /t 20 /nobreak >nul

echo.
echo Checking ports...
netstat -ano | findstr "LISTENING" | findstr /R ":[0-9]* " | findstr /R "808[1-9] 8080 900[12]"

echo.
echo Done! Check target\logs\ for service logs.
