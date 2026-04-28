package com.example.chat.server;

import java.util.List;
import java.util.stream.Collectors;

import com.example.chat.common.ChatMessage;
import com.google.gson.Gson;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebSocketServerHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private static final Gson GSON = new Gson();

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        ChannelManager.addChannel(channel);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        String username = ChannelManager.getUsername(channel);
        
        if (username != null) {
            ChatMessage logoutMessage = ChatMessage.createLogoutMessage(username);
            String logoutJson = GSON.toJson(logoutMessage);
            ChannelManager.broadcast(logoutJson);
            
            log.info("User '{}' logged out", username);
            
            sendOnlineUsersList();
        }
        
        ChannelManager.removeChannel(channel);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("Client connected: {}", ctx.channel().id().asShortText());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("Client disconnected: {}", ctx.channel().id().asShortText());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        if (frame instanceof TextWebSocketFrame) {
            String request = ((TextWebSocketFrame) frame).text();
            log.info("Received message: {}", request);
            
            try {
                ChatMessage message = GSON.fromJson(request, ChatMessage.class);
                handleMessage(ctx, message);
            } catch (Exception e) {
                log.error("Failed to parse message: {}", request, e);
            }
        } else {
            log.warn("Unsupported frame type: {}", frame.getClass().getName());
        }
    }

    private void handleMessage(ChannelHandlerContext ctx, ChatMessage message) {
        Channel channel = ctx.channel();
        String username = ChannelManager.getUsername(channel);
        
        switch (message.getType()) {
            case LOGIN:
                handleLogin(ctx, message);
                break;
            case CHAT:
                if (username == null) {
                    sendSystemMessage(channel, "请先登录");
                    return;
                }
                handleChat(ctx, message);
                break;
            case LOGOUT:
                if (username != null) {
                    handleLogout(ctx, username);
                }
                break;
            default:
                log.warn("Unknown message type: {}", message.getType());
        }
    }

    private void handleLogin(ChannelHandlerContext ctx, ChatMessage message) {
        Channel channel = ctx.channel();
        String username = message.getSender();
        
        if (username == null || username.trim().isEmpty()) {
            sendSystemMessage(channel, "用户名不能为空");
            return;
        }
        
        if (ChannelManager.getChannel(username) != null) {
            sendSystemMessage(channel, "用户名已存在");
            return;
        }
        
        ChannelManager.registerUser(channel, username);
        
        int offlineMessageCount = OfflineMessageManager.getOfflineMessageCount(username);
        
        ChatMessage welcomeMessage = ChatMessage.createSystemMessage(
                "欢迎 " + username + " 加入聊天室！当前在线 " + ChannelManager.getOnlineCount() + " 人" +
                (offlineMessageCount > 0 ? "，您有 " + offlineMessageCount + " 条离线消息" : ""));
        String welcomeJson = GSON.toJson(welcomeMessage);
        channel.writeAndFlush(new TextWebSocketFrame(welcomeJson));
        
        if (offlineMessageCount > 0) {
            sendOfflineMessages(channel, username);
        }
        
        ChatMessage loginBroadcast = ChatMessage.createSystemMessage(
                username + " 进入了聊天室");
        String broadcastJson = GSON.toJson(loginBroadcast);
        ChannelManager.broadcastToOthers(channel, broadcastJson);
        
        log.info("User '{}' logged in successfully", username);
        sendOnlineUsersList();
    }

    private void sendOfflineMessages(Channel channel, String username) {
        List<ChatMessage> offlineMessages = OfflineMessageManager.getAndRemoveMessages(username);
        
        for (ChatMessage msg : offlineMessages) {
            ChatMessage offlineMsg = ChatMessage.createOfflineMessage(
                    msg.getSender(),
                    msg.getTarget(),
                    msg.getContent()
            );
            offlineMsg.setTimestamp(msg.getTimestamp());
            String offlineJson = GSON.toJson(offlineMsg);
            channel.writeAndFlush(new TextWebSocketFrame(offlineJson));
        }
        
        log.info("Sent {} offline messages to user '{}'", offlineMessages.size(), username);
    }

    private void handleChat(ChannelHandlerContext ctx, ChatMessage message) {
        Channel channel = ctx.channel();
        String username = ChannelManager.getUsername(channel);
        
        ChatMessage chatMessage = ChatMessage.createChatMessage(username, message.getContent());
        String chatJson = GSON.toJson(chatMessage);
        
        if (message.getTarget() != null && !message.getTarget().isEmpty()) {
            chatMessage.setTarget(message.getTarget());
            String targetJson = GSON.toJson(chatMessage);
            
            Channel targetChannel = ChannelManager.getChannel(message.getTarget());
            if (targetChannel != null && targetChannel.isActive()) {
                ChannelManager.sendToUser(message.getTarget(), targetJson);
                channel.writeAndFlush(new TextWebSocketFrame(targetJson));
                log.info("Sent private message from '{}' to '{}'", username, message.getTarget());
            } else {
                OfflineMessageManager.storeMessage(chatMessage);
                sendSystemMessage(channel, message.getTarget() + " 当前不在线，消息已保存为离线消息");
                channel.writeAndFlush(new TextWebSocketFrame(targetJson));
                log.info("Stored offline message for user '{}' from '{}'", message.getTarget(), username);
            }
        } else {
            ChannelManager.broadcast(chatJson);
        }
    }

    private void handleLogout(ChannelHandlerContext ctx, String username) {
        Channel channel = ctx.channel();
        
        ChatMessage logoutMessage = ChatMessage.createLogoutMessage(username);
        String logoutJson = GSON.toJson(logoutMessage);
        ChannelManager.broadcast(logoutJson);
        
        ChannelManager.removeChannel(channel);
        log.info("User '{}' logged out", username);
        sendOnlineUsersList();
    }

    private void sendSystemMessage(Channel channel, String content) {
        ChatMessage systemMessage = ChatMessage.createSystemMessage(content);
        String systemJson = GSON.toJson(systemMessage);
        channel.writeAndFlush(new TextWebSocketFrame(systemJson));
    }

    private void sendOnlineUsersList() {
        List<String> users = ChannelManager.getOnlineUsers().stream()
                .sorted()
                .collect(Collectors.toList());
        String usersStr = String.join(",", users);
        ChatMessage onlineUsersMessage = ChatMessage.createOnlineUsersMessage(usersStr);
        String usersJson = GSON.toJson(onlineUsersMessage);
        ChannelManager.broadcast(usersJson);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Exception in channel: {}", ctx.channel().id().asShortText(), cause);
        ctx.close();
    }
}
