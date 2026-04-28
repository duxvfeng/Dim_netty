@echo off
chcp 65001 >nul
setlocal

echo ========================================
echo    Netty WebSocket Chat Server
echo ========================================
echo.

where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到 Maven, 请先安装 Maven 并配置环境变量
    pause
    exit /b 1
)

echo [信息] 正在启动 WebSocket 聊天服务端...
echo [信息] 服务端将监听端口 8080
echo [信息] WebSocket 地址: ws://localhost:8080/chat
echo.
echo [提示] 按 Ctrl+C 停止服务
echo ========================================
echo.

mvn exec:java -Dexec.mainClass="com.example.chat.server.WebSocketChatServer"

if %errorlevel% neq 0 (
    echo.
    echo [错误] 启动失败，请检查错误信息
    pause
)

endlocal
