@echo off
chcp 65001 >nul
cd /d %~dp0
echo [netease] starting NeteaseCloudMusicApi on http://localhost:3000 ...
node server.js