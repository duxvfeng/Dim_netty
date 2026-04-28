@echo off
chcp 65001 >nul
setlocal

echo ========================================
echo    Netty WebSocket Chat Client
echo ========================================
echo.

where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到 Maven, 请先安装 Maven 并配置环境变量
    pause
    exit /b 1
)

echo [信息] 正在启动 JavaFX 聊天客户端...
echo [信息] 请确保服务端已启动 (默认地址: localhost:8080)
echo.
echo [提示] 方式1: 使用 mvn javafx:run (推荐)
echo [提示] 方式2: 使用 mvn exec:java 配合 ChatClientLauncher
echo ========================================
echo.

mvn javafx:run

if %errorlevel% neq 0 (
    echo.
    echo [错误] 使用 javafx:run 启动失败, 尝试使用启动器方式...
    echo.
    mvn exec:java -Dexec.mainClass="com.example.chat.client.ChatClientLauncher"
)

if %errorlevel% neq 0 (
    echo.
    echo [错误] 启动失败，请检查错误信息
    echo.
    echo [可能的解决方法]:
    echo 1. 确保已安装 JDK 17 或更高版本
    echo 2. 确保已安装 JavaFX SDK 或通过 Maven 依赖获取
    echo 3. 尝试运行: mvn clean compile 后再启动
    pause
)

endlocal
