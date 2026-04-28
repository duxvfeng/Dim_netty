package com.example.chat.server;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChannelManager {
    private static final ChannelGroup CHANNELS = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private static final ConcurrentMap<Channel, String> CHANNEL_USERNAME_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Channel> USERNAME_CHANNEL_MAP = new ConcurrentHashMap<>();

    private ChannelManager() {
    }

    public static void addChannel(Channel channel) {
        CHANNELS.add(channel);
        log.info("New channel connected: {}", channel.id().asShortText());
    }

    public static void removeChannel(Channel channel) {
        CHANNELS.remove(channel);
        String username = CHANNEL_USERNAME_MAP.remove(channel);
        if (username != null) {
            USERNAME_CHANNEL_MAP.remove(username);
        }
        log.info("Channel disconnected: {}", channel.id().asShortText());
    }

    public static void registerUser(Channel channel, String username) {
        CHANNEL_USERNAME_MAP.put(channel, username);
        USERNAME_CHANNEL_MAP.put(username, channel);
        log.info("User '{}' logged in with channel: {}", username, channel.id().asShortText());
    }

    public static String getUsername(Channel channel) {
        return CHANNEL_USERNAME_MAP.get(channel);
    }

    public static Channel getChannel(String username) {
        return USERNAME_CHANNEL_MAP.get(username);
    }

    public static Set<String> getOnlineUsers() {
        return USERNAME_CHANNEL_MAP.keySet();
    }

    public static int getOnlineCount() {
        return USERNAME_CHANNEL_MAP.size();
    }

    public static void broadcast(String message) {
        CHANNELS.writeAndFlush(new TextWebSocketFrame(message));
        log.debug("Broadcast message: {}", message);
    }

    public static void broadcastToOthers(Channel excludeChannel, String message) {
        CHANNELS.stream()
                .filter(ch -> ch != excludeChannel)
                .forEach(ch -> ch.writeAndFlush(new TextWebSocketFrame(message)));
        log.debug("Broadcast to others (excluded: {}): {}", 
                excludeChannel.id().asShortText(), message);
    }

    public static void sendToUser(String username, String message) {
        Channel channel = USERNAME_CHANNEL_MAP.get(username);
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(new TextWebSocketFrame(message));
            log.debug("Send message to user '{}': {}", username, message);
        }
    }
}
