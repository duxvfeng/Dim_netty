package com.example.chat.client;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.chat.common.ChatMessage;
import com.google.gson.Gson;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatController implements Initializable {

    @FXML
    private VBox loginBox;
    @FXML
    private VBox chatBox;
    @FXML
    private TextField serverAddressField;
    @FXML
    private TextField usernameField;
    @FXML
    private Button connectBtn;
    @FXML
    private TextArea chatArea;
    @FXML
    private TextField messageField;
    @FXML
    private Button sendBtn;
    @FXML
    private Button disconnectBtn;
    @FXML
    private ListView<String> onlineUsersList;
    @FXML
    private Label chatModeLabel;
    @FXML
    private Button switchToGroupChatBtn;

    private WebSocketClient webSocketClient;
    private String currentUsername;
    private String currentChatTarget = null;
    private boolean isPrivateChatMode = false;
    private final Gson gson = new Gson();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        chatArea.setEditable(false);
        chatBox.setVisible(false);
        loginBox.setVisible(true);

        messageField.setOnAction(event -> sendMessage());
        sendBtn.setOnAction(event -> sendMessage());
        connectBtn.setOnAction(event -> connect());
        disconnectBtn.setOnAction(event -> disconnect());
        switchToGroupChatBtn.setOnAction(event -> switchToGroupChat());

        onlineUsersList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selectedUser = onlineUsersList.getSelectionModel().getSelectedItem();
                if (selectedUser != null && !selectedUser.equals(currentUsername)) {
                    startPrivateChat(selectedUser);
                }
            }
        });

        updateChatModeLabel();
    }

    private void startPrivateChat(String targetUser) {
        currentChatTarget = targetUser;
        isPrivateChatMode = true;
        updateChatModeLabel();
        appendMessage("系统", "已切换到与 " + targetUser + " 的私聊模式");
    }

    private void switchToGroupChat() {
        currentChatTarget = null;
        isPrivateChatMode = false;
        onlineUsersList.getSelectionModel().clearSelection();
        updateChatModeLabel();
        appendMessage("系统", "已切换到群聊模式");
    }

    private void updateChatModeLabel() {
        if (isPrivateChatMode && currentChatTarget != null) {
            chatModeLabel.setText("当前: 私聊 @" + currentChatTarget);
            chatModeLabel.setTextFill(Color.RED);
            switchToGroupChatBtn.setVisible(true);
        } else {
            chatModeLabel.setText("当前: 群聊模式");
            chatModeLabel.setTextFill(Color.GREEN);
            switchToGroupChatBtn.setVisible(false);
        }
    }

    private void connect() {
        String serverAddress = serverAddressField.getText().trim();
        String username = usernameField.getText().trim();

        if (serverAddress.isEmpty()) {
            showAlert("错误", "请输入服务器地址");
            return;
        }

        if (username.isEmpty()) {
            showAlert("错误", "请输入用户名");
            return;
        }

        if (username.contains(",")) {
            showAlert("错误", "用户名不能包含逗号");
            return;
        }

        String wsUrl = "ws://" + serverAddress + "/chat";
        currentUsername = username;

        webSocketClient = new WebSocketClient(wsUrl);
        webSocketClient.setOnConnect(() -> {
            Platform.runLater(() -> {
                loginBox.setVisible(false);
                chatBox.setVisible(true);
                currentChatTarget = null;
                isPrivateChatMode = false;
                updateChatModeLabel();
                appendMessage("系统", "正在登录...");
                
                ChatMessage loginMessage = ChatMessage.createLoginMessage(currentUsername);
                webSocketClient.sendMessage(gson.toJson(loginMessage));
            });
        });

        webSocketClient.setOnDisconnect(() -> {
            Platform.runLater(() -> {
                showAlert("连接断开", "与服务器的连接已断开");
                disconnect();
            });
        });

        webSocketClient.setMessageHandler(this::handleMessage);

        executorService.submit(() -> {
            boolean connected = webSocketClient.connect();
            if (!connected) {
                Platform.runLater(() -> showAlert("连接失败", "无法连接到服务器"));
            }
        });
    }

    private void handleMessage(String message) {
        try {
            ChatMessage chatMessage = gson.fromJson(message, ChatMessage.class);
            Platform.runLater(() -> processMessage(chatMessage));
        } catch (Exception e) {
            log.error("Failed to parse message: {}", message, e);
        }
    }

    private void processMessage(ChatMessage message) {
        switch (message.getType()) {
            case CHAT:
                String sender = message.getSender();
                String content = message.getContent();
                if (message.getTarget() != null && !message.getTarget().isEmpty()) {
                    appendPrivateMessage(sender, message.getTarget(), content, message.getTimestamp());
                } else {
                    appendMessage(sender, content, message.getTimestamp());
                }
                break;
            case SYSTEM:
                appendMessage("系统", message.getContent(), message.getTimestamp());
                break;
            case LOGIN:
                appendMessage("系统", message.getSender() + " 进入了聊天室", message.getTimestamp());
                break;
            case LOGOUT:
                appendMessage("系统", message.getSender() + " 离开了聊天室", message.getTimestamp());
                break;
            case ONLINE_USERS:
                updateOnlineUsers(message.getContent());
                break;
            case OFFLINE_MESSAGE:
                appendMessage("系统", "【离线消息】" + message.getSender() + ": " + message.getContent(), message.getTimestamp());
                break;
            default:
                log.warn("Unknown message type: {}", message.getType());
        }
    }

    private void appendMessage(String sender, String content) {
        appendMessage(sender, content, "");
    }

    private void appendMessage(String sender, String content, String timestamp) {
        String time = timestamp.isEmpty() ? "" : " [" + timestamp + "]";
        chatArea.appendText(sender + time + ": " + content + "\n");
        chatArea.setScrollTop(Double.MAX_VALUE);
    }

    private void appendPrivateMessage(String sender, String target, String content, String timestamp) {
        String time = timestamp.isEmpty() ? "" : " [" + timestamp + "]";
        if (sender.equals(currentUsername)) {
            chatArea.appendText("[私聊 发送给 " + target + "]" + time + ": " + content + "\n");
        } else {
            chatArea.appendText("[私聊 来自 " + sender + "]" + time + ": " + content + "\n");
        }
        chatArea.setScrollTop(Double.MAX_VALUE);
    }

    private void updateOnlineUsers(String usersStr) {
        onlineUsersList.getItems().clear();
        if (usersStr != null && !usersStr.isEmpty()) {
            List<String> users = Arrays.asList(usersStr.split(","));
            onlineUsersList.getItems().addAll(users);
        }
    }

    private void sendMessage() {
        if (webSocketClient == null || !webSocketClient.isConnected()) {
            showAlert("错误", "未连接到服务器");
            return;
        }

        String content = messageField.getText().trim();
        if (content.isEmpty()) {
            return;
        }

        ChatMessage chatMessage = ChatMessage.createChatMessage(currentUsername, content);
        if (isPrivateChatMode && currentChatTarget != null) {
            chatMessage.setTarget(currentChatTarget);
        }
        
        webSocketClient.sendMessage(gson.toJson(chatMessage));
        messageField.clear();
    }

    private void disconnect() {
        if (webSocketClient != null) {
            if (webSocketClient.isConnected()) {
                ChatMessage logoutMessage = ChatMessage.createLogoutMessage(currentUsername);
                webSocketClient.sendMessage(gson.toJson(logoutMessage));
            }
            webSocketClient.disconnect();
        }

        chatBox.setVisible(false);
        loginBox.setVisible(true);
        chatArea.clear();
        onlineUsersList.getItems().clear();
        currentChatTarget = null;
        isPrivateChatMode = false;
        updateChatModeLabel();
        currentUsername = null;
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void shutdown() {
        if (webSocketClient != null) {
            webSocketClient.disconnect();
        }
        executorService.shutdownNow();
    }
}
