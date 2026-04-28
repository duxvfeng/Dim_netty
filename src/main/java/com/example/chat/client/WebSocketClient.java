package com.example.chat.client;

import java.net.URI;
import java.util.function.Consumer;

import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ClientEndpoint
public class WebSocketClient {

    private Session session;
    private Consumer<String> messageHandler;
    private Runnable onConnect;
    private Runnable onDisconnect;
    private final String serverUri;

    public WebSocketClient(String serverUri) {
        this.serverUri = serverUri;
    }

    public void setMessageHandler(Consumer<String> messageHandler) {
        this.messageHandler = messageHandler;
    }

    public void setOnConnect(Runnable onConnect) {
        this.onConnect = onConnect;
    }

    public void setOnDisconnect(Runnable onDisconnect) {
        this.onDisconnect = onDisconnect;
    }

    public boolean connect() {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            URI uri = URI.create(serverUri);
            container.connectToServer(this, uri);
            log.info("Connecting to WebSocket server: {}", serverUri);
            return true;
        } catch (Exception e) {
            log.error("Failed to connect to WebSocket server", e);
            return false;
        }
    }

    public void disconnect() {
        try {
            if (session != null && session.isOpen()) {
                session.close();
                log.info("Disconnected from WebSocket server");
            }
        } catch (Exception e) {
            log.error("Error during disconnect", e);
        }
    }

    public void sendMessage(String message) {
        try {
            if (session != null && session.isOpen()) {
                session.getBasicRemote().sendText(message);
                log.debug("Sent message: {}", message);
            } else {
                log.warn("Cannot send message: session is not open");
            }
        } catch (Exception e) {
            log.error("Failed to send message", e);
        }
    }

    public boolean isConnected() {
        return session != null && session.isOpen();
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        log.info("WebSocket connection opened");
        if (onConnect != null) {
            onConnect.run();
        }
    }

    @OnMessage
    public void onMessage(String message) {
        log.debug("Received message: {}", message);
        if (messageHandler != null) {
            messageHandler.accept(message);
        }
    }

    @OnClose
    public void onClose(Session session) {
        log.info("WebSocket connection closed");
        this.session = null;
        if (onDisconnect != null) {
            onDisconnect.run();
        }
    }
}
