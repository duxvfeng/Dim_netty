@echo off
chcp 65001 >nul
setlocal

echo ========================================
echo    Build Netty WebSocket Chat
echo ========================================
echo.

where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到 Maven, 请先安装 Maven 并配置环境变量
    pause
    exit /b 1
)

echo [信息] 正在清理并编译项目...
echo.

mvn clean compile

if %errorlevel% neq 0 (
    echo.
    echo [错误] 编译失败，请检查错误信息
    pause
    exit /b 1
)

echo.
echo [信息] 编译成功!
echo.
echo [信息] 正在打包 JAR 文件...
echo.

mvn package -DskipTests

if %errorlevel% neq 0 (
    echo.
    echo [警告] 打包失败，但编译已成功
    echo.
    echo [使用方法]
    echo - 启动服务端: run-server.bat 或 mvn exec:java -Dexec.mainClass="com.example.chat.server.WebSocketChatServer"
    echo - 启动客户端: run-client.bat 或 mvn javafx:run
    pause
    exit /b 0
)

echo.
echo [信息] 打包完成!
echo.
echo [生成的文件]:
echo - target/chat-client.jar
echo.
echo [注意] 运行 JAR 需要 JavaFX SDK，推荐使用 Maven 运行方式
echo.
echo [使用方法]:
echo 1. 先启动服务端: run-server.bat
echo 2. 再启动客户端: run-client.bat (可启动多个客户端进行聊天)
echo.
echo ========================================

pause
endlocal
