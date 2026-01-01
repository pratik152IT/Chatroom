package com.chatroom.repository;

import com.chatroom.model.Message;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * MessageRepository - Handles all database operations for messages
 * Demonstrates:
 * - Saving messages with foreign keys
 * - Querying with ordering
 * - Converting SQL results to Java objects
 */
public class MessageRepository {
    private Connection connection;

    public MessageRepository(Connection connection) {
        this.connection = connection;
        createTable();
    }

    /**
     * Create messages table with foreign key to users
     */
    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS messages (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL, " +
                "username TEXT NOT NULL, " +
                "message_text TEXT NOT NULL, " +
                "timestamp TEXT NOT NULL, " +
                "FOREIGN KEY (user_id) REFERENCES users(id))";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            System.out.println("✅ Messages table ready");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create messages table", e);
        }
    }

    /**
     * Save a new message to database
     * @param message - Message object to save
     * @return Message object with generated ID
     */
    public Message save(Message message) {
        String sql = "INSERT INTO messages (user_id, username, message_text, timestamp) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, message.getUserId());
            pstmt.setString(2, message.getUsername());
            pstmt.setString(3, message.getMessageText());
            pstmt.setString(4, message.getTimestamp().toString());
            pstmt.executeUpdate();

            // SQLite-specific way to get last inserted ID
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    message.setId(rs.getLong(1));
                }
            }

            return message;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save message", e);
        }
    }

    /**
     * Get all messages ordered by timestamp (oldest first)
     * Shows complete chat history in chronological order
     * @return List of all messages
     */
    public List<Message> findAllOrderByTimestamp() {
        String sql = "SELECT id, user_id, username, message_text, timestamp FROM messages ORDER BY timestamp ASC";
        List<Message> messages = new ArrayList<>();

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Message message = new Message(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getString("username"),
                        rs.getString("message_text"),
                        LocalDateTime.parse(rs.getString("timestamp"))
                );
                messages.add(message);
            }
            return messages;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch messages", e);
        }
    }

    /**
     * Find all messages sent by a specific user
     * @param userId - ID of the user
     * @return List of user's messages
     */
    public List<Message> findByUserId(Long userId) {
        String sql = "SELECT id, user_id, username, message_text, timestamp " +
                "FROM messages WHERE user_id = ? ORDER BY timestamp ASC";
        List<Message> messages = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Message message = new Message(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getString("username"),
                        rs.getString("message_text"),
                        LocalDateTime.parse(rs.getString("timestamp"))
                );
                messages.add(message);
            }
            return messages;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch user messages", e);
        }
    }


    public Optional<Message> findById(Long id) {
        String sql = "SELECT id, user_id, username, message_text, timestamp FROM messages WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Message message = new Message(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getString("username"),
                        rs.getString("message_text"),
                        LocalDateTime.parse(rs.getString("timestamp"))
                );
                return Optional.of(message);
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find message", e);
        }
    }


    public void deleteById(Long id) {
        String sql = "DELETE FROM messages WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete message", e);
        }
    }
}