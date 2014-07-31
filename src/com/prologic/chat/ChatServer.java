package com.prologic.chat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;

import static java.nio.channels.SelectionKey.OP_ACCEPT;
import static java.nio.channels.SelectionKey.OP_READ;

public class ChatServer extends AbstractChatComponent {

    public ChatServer(String id, int port) throws IOException {
        super(id, port);
    }

    public int getPort() throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) channel;
        return serverChannel.socket().getLocalPort();
    }

    @Override
    protected Selector selector() throws IOException {
        AbstractSelector selector = SelectorProvider.provider().openSelector();
        channel.register(selector, OP_ACCEPT);
        return selector;
    }

    @Override
    protected AbstractSelectableChannel channel(InetSocketAddress address) throws IOException {
        ServerSocketChannel channel = ServerSocketChannel.open();
        channel.configureBlocking(false);
        channel.socket().bind(address);
        return channel;
    }
    @Override
    protected void handleIncomingData(SelectionKey sender, byte[] data) throws IOException {
        for (SelectionKey key : selector.keys()) {
            if (key.channel() instanceof ServerSocketChannel) {
                continue;
            }
            if (key.equals(sender)) {
                continue;
            }
            key.attach(ByteBuffer.wrap(data));
            write(key);
        }
    }

    @Override
    protected void write(SelectionKey key) throws IOException {
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        SocketChannel channel = (SocketChannel) key.channel();
        channel.write(buffer);
        key.interestOps(OP_READ);
    }
}
