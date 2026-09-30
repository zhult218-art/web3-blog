@echo off
chcp 65001 >nul
REM ============================================================
REM Web3 Portal — 一键启动本地开发环境全部服务
REM 本体逻辑已统一收敛到 start-all-services.ps1，本文件仅作双击入口。
REM ============================================================
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-all-services.ps1"
pause