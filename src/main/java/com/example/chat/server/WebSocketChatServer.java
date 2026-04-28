package com.example.chat.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebSocketChatServer {

    private final int port;
    private final String websocketPath;

    public WebSocketChatServer(int port, String websocketPath) {
        this.port = port;
        this.websocketPath = websocketPath;
    }

    public WebSocketChatServer(int port) {
        this(port, "/chat");
    }

    public WebSocketChatServer() {
        this(8080, "/chat");
    }

    public void start() throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new WebSocketServerInitializer(websocketPath, 65536))
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            log.info("WebSocket Chat Server starting on port {}...", port);
            log.info("WebSocket path: ws://localhost:{}{}", port, websocketPath);

            ChannelFuture future = bootstrap.bind(port).sync();
            log.info("WebSocket Chat Server started successfully!");
            log.info("Press Ctrl+C to stop the server");

            Channel channel = future.channel();
            channel.closeFuture().sync();

        } finally {
            log.info("Shutting down WebSocket Chat Server...");
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            log.info("WebSocket Chat Server stopped");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int port = 8080;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number, using default port 8080");
            }
        }

        WebSocketChatServer server = new WebSocketChatServer(port);
        server.start();
    }
}
