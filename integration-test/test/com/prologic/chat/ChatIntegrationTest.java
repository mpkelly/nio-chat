package test.com.prologic.chat;

import com.prologic.chat.ChatClient;
import com.prologic.chat.ChatResponseHandler;
import com.prologic.chat.ChatServer;
import org.junit.After;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ChatIntegrationTest {
    String address = "localhost";
    ChatServer server;
    ChatClient client1;
    ChatClient client2;

    @After
    public void cleanUp() throws Exception {
        client1.stop();
        client2.stop();
        server.stop();
    }

    @Test(timeout = 2000)
    public void DeliversAllMessagesToConnectedClients() throws Exception {
        server = new ChatServer("server", 0);
        server.start();
        int port = server.getPort();

        assertNotEquals("port", port, 0);

        final CountDownLatch latch = new CountDownLatch(2);

        client1 = new ChatClient("client one", address, port, new ChatResponseHandler() {
            @Override
            public void onMessage(String message) {
                latch.countDown();
                assertEquals("message", "two", message);
            }
        });
        client1.start();

        client2 = new ChatClient("client two", address, port, new ChatResponseHandler() {
            @Override
            public void onMessage(String message) {
                latch.countDown();
                assertEquals("message", "one", message);
            }
        });
        client2.start();

        // Allow a bit of time for both connections to be established
        Thread.sleep(500);

        client1.sendMessage("one");
        client2.sendMessage("two");

        latch.await();
    }

    @Test(timeout = 5000)
    public void HandlesContinuousStreamOfMessages() throws Exception {
        server = new ChatServer("server", 0);
        server.start();
        int port = server.getPort();

        assertNotEquals("port", port, 0);

        int messageCount = 100;

        final CountDownLatch latch = new CountDownLatch(messageCount);

        client1 = new ChatClient("client one", address, port, new ChatResponseHandler() {
            @Override public void onMessage(String message) {}
        });
        client1.start();

        client2 = new ChatClient("client two", address, port, new ChatResponseHandler() {
            @Override
            public void onMessage(String message) {
                latch.countDown();
                assertEquals("message", "one", message);
            }
        });
        client2.start();

        // Allow a bit of time for both connections to be established
        Thread.sleep(500);

        for (int i = 0; i < messageCount; i++) {
            client1.sendMessage("one");
        }
        latch.await();
    }
}
