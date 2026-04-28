package com.example.chat.client;

import java.lang.reflect.Method;

public class ChatClientLauncher {

    public static void main(String[] args) {
        try {
            Class<?> appClass = Class.forName("com.example.chat.client.ChatClientApp");
            Method launchMethod = Class.forName("javafx.application.Application")
                    .getMethod("launch", Class.class, String[].class);
            
            launchMethod.invoke(null, appClass, args);
        } catch (ClassNotFoundException e) {
            System.err.println("错误: 未找到 JavaFX 类");
            System.err.println("请确保已添加 JavaFX 依赖:");
            System.err.println("  - javafx-controls");
            System.err.println("  - javafx-fxml");
            System.err.println("  - javafx-base");
            System.err.println("  - javafx-graphics");
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("启动 JavaFX 应用失败:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
