NIO-CHAT
=======

A chat server and client implementation using the Java NIO package. Implements the event-based selector pattern which is a neat way to avoid creating a new thread for every client connection.As a result this will scale much better than the traditional thread-per-connection approach. 

This chat server and client were written for an Android demo project but can easily be updated to work in a production environment.

If you plan to use this code there's a few things for you to consider:

- buffer size as specified in ChatConfig
- Message delimiter (also in ChatConfig). Currently a CRLF is used but this might not be appropriate for your application. Either way, a delimiter is needed so that clients can breakdown the payload from the server into individual messages as sent by other clients. 
- messages received by the server are currently delivered on the selector thread, which is fine for a small amount of clients but will become a bottleneck as the clients increase. You can easily introduce a java.util.concurrent.ThreadPoolExecutor in the ChatServer which can deliver messages and avoid blocking the selector thread for too long. 
- error handling could be improved. One option would be to add an onException() method to the ChatResponseHandler. 
- You will need to use a CharSet that works for both the client and server. UTF-8 is a good choice for Western languages while UTF-16 might be a better choice for East Asian language.
