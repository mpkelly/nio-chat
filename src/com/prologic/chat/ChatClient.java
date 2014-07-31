package com.prologic.chat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.LinkedList;

import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;

public class ChatClient extends AbstractChatComponent {
    private final LinkedList<String> messages;
    private final ChatResponseHandler handler;

    public ChatClient(String id, String hostname, int port, ChatResponseHandler handler) throws IOException {
        super(id, new InetSocketAddress(hostname, port));
        this.handler = handler;
        this.messages = new LinkedList<>();
    }

    @Override
    protected AbstractSelectableChannel channel(InetSocketAddress address) throws IOException {
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.connect(address);
        return channel;
    }

    @Override
    protected Selector selector() throws IOException {
        Selector selector = Selector.open();
        channel.register(selector, OP_CONNECT);
        return selector;
    }

    @Override
    protected void handleIncomingData(SelectionKey sender, byte[] data) {
        for (String message : new String(data).split(MESSAGE_DELIMITER)) {
            handler.onMessage(message);
        }
    }

    @Override
    protected void write(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        while (!messages.isEmpty()) {
            String message = messages.poll();
            channel.write(ByteBuffer.wrap(message.getBytes()));
        }
        key.interestOps(OP_READ);
    }

    public void sendMessage(String message) {
        messages.add(message + MESSAGE_DELIMITER);
        SelectionKey key = channel.keyFor(selector);
        key.interestOps(OP_WRITE);
    }
}
