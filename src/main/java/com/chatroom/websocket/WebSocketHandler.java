package com.chatroom.websocket;

import com.google.gson.Gson;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import java.io.IOException;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * WebSocketHandler - Handles WebSocket connections for real-time chat
 *
 * Features:
 * - Broadcast messages to all connected users
 * - Track online users
 * - Send join/leave notifications
 * - Handle user disconnections gracefully
 */
@WebSocket
public class WebSocketHandler {

    // Store all active WebSocket sessions (connected users)
    private static final Queue<Session> sessions = new ConcurrentLinkedQueue<>();

    // Store username for each session
    private static final Map<Session, String> userSessions = new ConcurrentHashMap<>();

    private static final Gson gson = new Gson();

    /**
     * Called when a new user connects via WebSocket
     */
    @OnWebSocketConnect
    public void onConnect(Session session) throws IOException {
        sessions.add(session);
        System.out.println("🔌 New WebSocket connection. Total users: " + sessions.size());

        // Send connection success message to the user
        sendToSession(session, new WebSocketMessage(
                "SYSTEM",
                "connected",
                "Connected to chat server",
                null,
                sessions.size()
        ));
    }

    /**
     * Called when a user disconnects
     */
    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        String username = userSessions.get(session);
        sessions.remove(session);
        userSessions.remove(session);

        System.out.println("❌ WebSocket closed: " + username + ". Total users: " + sessions.size());

        // Notify all users that someone left
        if (username != null) {
            broadcast(new WebSocketMessage(
                    "SYSTEM",
                    "user_left",
                    username + " left the chat",
                    username,
                    sessions.size()
            ));
        }
    }

    /**
     * Called when a message is received from a user
     */
    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        System.out.println("📨 Received WebSocket message: " + message);

        try {
            // Parse incoming JSON message
            Map<String, Object> data = gson.fromJson(message, Map.class);
            String type = (String) data.get("type");

            switch (type) {
                case "join":
                    handleJoin(session, data);
                    break;

                case "message":
                    handleChatMessage(session, data);
                    break;

                case "typing":
                    handleTyping(session, data);
                    break;

                default:
                    System.out.println("⚠️ Unknown message type: " + type);
            }

        } catch (Exception e) {
            System.err.println("❌ Error processing WebSocket message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle user join notification
     */
    private void handleJoin(Session session, Map<String, Object> data) {
        String username = (String) data.get("username");
        userSessions.put(session, username);

        System.out.println("👋 User joined: " + username);

        // Broadcast to all users that someone joined
        broadcast(new WebSocketMessage(
                "SYSTEM",
                "user_joined",
                username + " joined the chat",
                username,
                sessions.size()
        ));
    }

    /**
     * Handle chat message broadcast
     */
    private void handleChatMessage(Session session, Map<String, Object> data) {
        String username = (String) data.get("username");
        String messageText = (String) data.get("message");
        Long userId = data.get("userId") != null ?
                ((Double) data.get("userId")).longValue() : null;

        System.out.println("💬 Broadcasting message from " + username + ": " + messageText);

        // Broadcast message to all connected users
        broadcast(new WebSocketMessage(
                username,
                "message",
                messageText,
                username,
                sessions.size()
        ));
    }

    /**
     * Handle typing indicator
     */
    private void handleTyping(Session session, Map<String, Object> data) {
        String username = (String) data.get("username");
        Boolean isTyping = (Boolean) data.get("isTyping");

        // Broadcast typing status to all OTHER users (not sender)
        broadcastExcept(session, new WebSocketMessage(
                username,
                "typing",
                isTyping ? username + " is typing..." : "",
                username,
                sessions.size()
        ));
    }

    /**
     * Called when there's an error
     */
    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        System.err.println("❌ WebSocket error: " + error.getMessage());
        error.printStackTrace();
    }

    /**
     * Broadcast message to ALL connected users
     */
    private static void broadcast(WebSocketMessage message) {
        String json = gson.toJson(message);

        sessions.forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.getRemote().sendString(json);
                }
            } catch (IOException e) {
                System.err.println("❌ Error broadcasting to session: " + e.getMessage());
            }
        });
    }

    /**
     * Broadcast message to all users EXCEPT the sender
     */
    private static void broadcastExcept(Session excludeSession, WebSocketMessage message) {
        String json = gson.toJson(message);

        sessions.forEach(session -> {
            try {
                if (session.isOpen() && !session.equals(excludeSession)) {
                    session.getRemote().sendString(json);
                }
            } catch (IOException e) {
                System.err.println("❌ Error broadcasting to session: " + e.getMessage());
            }
        });
    }

    /**
     * Send message to a specific session
     */
    private static void sendToSession(Session session, WebSocketMessage message) throws IOException {
        if (session.isOpen()) {
            String json = gson.toJson(message);
            session.getRemote().sendString(json);
        }
    }

    /**
     * Get count of online users
     */
    public static int getOnlineUsersCount() {
        return sessions.size();
    }
}

/**
 * WebSocket Message Format
 */
class WebSocketMessage {
    private String username;
    private String type;
    private String message;
    private String from;
    private int onlineUsers;
    private long timestamp;

    public WebSocketMessage(String username, String type, String message, String from, int onlineUsers) {
        this.username = username;
        this.type = type;
        this.message = message;
        this.from = from;
        this.onlineUsers = onlineUsers;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public String getUsername() { return username; }
    public String getType() { return type; }
    public String getMessage() { return message; }
    public String getFrom() { return from; }
    public int getOnlineUsers() { return onlineUsers; }
    public long getTimestamp() { return timestamp; }
}