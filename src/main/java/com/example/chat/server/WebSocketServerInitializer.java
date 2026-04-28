package com.example.chat.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

public class WebSocketServerInitializer extends ChannelInitializer<SocketChannel> {

    private final String websocketPath;
    private final int maxFrameSize;

    public WebSocketServerInitializer(String websocketPath, int maxFrameSize) {
        this.websocketPath = websocketPath;
        this.maxFrameSize = maxFrameSize;
    }

    public WebSocketServerInitializer() {
        this("/chat", 65536);
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast(new HttpServerCodec());

        pipeline.addLast(new ChunkedWriteHandler());

        pipeline.addLast(new HttpObjectAggregator(maxFrameSize));

        pipeline.addLast(new WebSocketServerProtocolHandler(websocketPath, null, true, maxFrameSize));

        pipeline.addLast(new WebSocketServerHandler());
    }
}
