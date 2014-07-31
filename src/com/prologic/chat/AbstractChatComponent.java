package com.prologic.chat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.Iterator;

import static com.prologic.util.Closer.close;
import static java.lang.System.arraycopy;
import static java.lang.Thread.State.NEW;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;

public abstract class AbstractChatComponent implements Runnable, ChatConfig {
    protected final String id;
    protected final ByteBuffer buffer;
    protected final Selector selector;
    protected final AbstractSelectableChannel channel;
    protected final InetSocketAddress address;
    private volatile boolean running;

    public AbstractChatComponent(String id, int port) throws IOException {
        this(id, new InetSocketAddress(port));
    }

    public AbstractChatComponent(String id, InetSocketAddress address) throws IOException {
        this.id = id;
        this.address = address;
        this.buffer = ByteBuffer.allocate(BUFFER_SIZE);
        this.channel = channel(address);
        this.selector = selector();
    }

    protected abstract AbstractSelectableChannel channel(InetSocketAddress address) throws IOException;

    protected abstract Selector selector() throws IOException;

    protected abstract void handleIncomingData(SelectionKey sender, byte[] data) throws IOException;

    protected abstract void write(SelectionKey key) throws IOException;

    public synchronized void start() {
        Thread executionThread = new Thread(this, id);
        running = true;
        executionThread.start();
        while (executionThread.getState() == NEW) {
            //
        }
    }

    public void stop() throws IOException {
        running = false;
    }

    @Override
    public void run() {
        while (running) {
            runMainLoop();
        }
        cleanUp();
    }

    private void cleanUp() {
        for (SelectionKey key : selector.keys()) {
            close(key.channel());
        }
        close(selector);
    }

    private void runMainLoop() {
        try {
            selector.select(1000);

            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();

            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();

                if (!key.isValid()) {
                    continue;
                }

                if (key.isConnectable()) {
                    connect(key);
                } else if (key.isAcceptable()) {
                    accept(key);
                } else if (key.isReadable()) {
                    read(key);
                } else if (key.isWritable()) {
                    write(key);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, OP_READ);
    }

    protected void connect(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        try {
            channel.finishConnect();
            channel.configureBlocking(false);
            channel.register(selector, OP_WRITE);
        } catch (IOException e) {
            e.printStackTrace();
            key.channel().close();
            key.cancel();
        }
    }

    protected void read(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        buffer.clear();
        int readCount;

        try {
            readCount = channel.read(buffer);
        } catch (IOException e) {
            key.cancel();
            channel.close();
            return;
        }

        if (readCount == -1) {
            // Channel is no longer active - clean up
            key.channel().close();
            key.cancel();
            return;
        }

        byte [] data = new byte[buffer.position()];

        arraycopy(buffer.array(), 0, data, 0, buffer.position());

        handleIncomingData(key, data);
    }
}
