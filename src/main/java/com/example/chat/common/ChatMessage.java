package com.example.chat.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private MessageType type;
    private String sender;
    private String content;
    private String timestamp;
    private String target;

    public enum MessageType {
        LOGIN,
        LOGOUT,
        CHAT,
        SYSTEM,
        ONLINE_USERS
    }

    public static ChatMessage createLoginMessage(String username) {
        return ChatMessage.builder()
                .type(MessageType.LOGIN)
                .sender(username)
                .timestamp(LocalDateTime.now().format(FORMATTER))
                .build();
    }

    public static ChatMessage createLogoutMessage(String username) {
        return ChatMessage.builder()
                .type(MessageType.LOGOUT)
                .sender(username)
                .timestamp(LocalDateTime.now().format(FORMATTER))
                .build();
    }

    public static ChatMessage createChatMessage(String sender, String content) {
        return ChatMessage.builder()
                .type(MessageType.CHAT)
                .sender(sender)
                .content(content)
                .timestamp(LocalDateTime.now().format(FORMATTER))
                .build();
    }

    public static ChatMessage createSystemMessage(String content) {
        return ChatMessage.builder()
                .type(MessageType.SYSTEM)
                .content(content)
                .timestamp(LocalDateTime.now().format(FORMATTER))
                .build();
    }

    public static ChatMessage createOnlineUsersMessage(String users) {
        return ChatMessage.builder()
                .type(MessageType.ONLINE_USERS)
                .content(users)
                .timestamp(LocalDateTime.now().format(FORMATTER))
                .build();
    }
}
