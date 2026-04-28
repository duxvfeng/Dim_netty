package com.example.chat.client;

import java.lang.reflect.Method;

public class ChatClientLauncher {

    public static void main(String[] args) throws Exception {
        Class<?> appClass = Class.forName("com.example.chat.client.ChatClientApp");
        Method launchMethod = Class.forName("javafx.application.Application")
                .getMethod("launch", Class.class, String[].class);
        launchMethod.invoke(null, appClass, args);
    }
}
