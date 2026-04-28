package com.example.chat.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import com.example.chat.common.ChatMessage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OfflineMessageManager {

    private static final ConcurrentMap<String, ConcurrentLinkedQueue<ChatMessage>> OFFLINE_MESSAGES = new ConcurrentHashMap<>();

    private static final int MAX_MESSAGES_PER_USER = 100;

    private OfflineMessageManager() {
    }

    public static void storeMessage(ChatMessage message) {
        String targetUser = message.getTarget();
        if (targetUser == null || targetUser.isEmpty()) {
            log.warn("Cannot store offline message: target user is null or empty");
            return;
        }

        OFFLINE_MESSAGES.computeIfAbsent(targetUser, k -> new ConcurrentLinkedQueue<>());

        ConcurrentLinkedQueue<ChatMessage> queue = OFFLINE_MESSAGES.get(targetUser);
        
        while (queue.size() >= MAX_MESSAGES_PER_USER) {
            queue.poll();
            log.warn("Offline message queue for user '{}' is full, removing oldest message", targetUser);
        }

        queue.offer(message);
        log.info("Stored offline message for user '{}', total offline messages: {}", targetUser, queue.size());
    }

    public static List<ChatMessage> getAndRemoveMessages(String username) {
        List<ChatMessage> messages = new ArrayList<>();
        
        ConcurrentLinkedQueue<ChatMessage> queue = OFFLINE_MESSAGES.get(username);
        if (queue != null && !queue.isEmpty()) {
            ChatMessage msg;
            while ((msg = queue.poll()) != null) {
                messages.add(msg);
            }
            log.info("Retrieved {} offline messages for user '{}'", messages.size(), username);
        }
        
        OFFLINE_MESSAGES.remove(username);
        return messages;
    }

    public static boolean hasOfflineMessages(String username) {
        ConcurrentLinkedQueue<ChatMessage> queue = OFFLINE_MESSAGES.get(username);
        return queue != null && !queue.isEmpty();
    }

    public static int getOfflineMessageCount(String username) {
        ConcurrentLinkedQueue<ChatMessage> queue = OFFLINE_MESSAGES.get(username);
        return queue != null ? queue.size() : 0;
    }

    public static void clearOfflineMessages(String username) {
        OFFLINE_MESSAGES.remove(username);
        log.info("Cleared offline messages for user '{}'", username);
    }

    public static void clearAll() {
        OFFLINE_MESSAGES.clear();
        log.info("Cleared all offline messages");
    }
}
