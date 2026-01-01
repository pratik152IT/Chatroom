package com.chatroom.model;

import java.time.LocalDateTime;

public class Message {
    private Long id;
    private Long userId;
    private String username;  // Stored directly for easy retrieval
    private String messageText;
    private LocalDateTime timestamp;


    public Message() {}


    public Message(Long id, Long userId, String username, String messageText, LocalDateTime timestamp) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.messageText = messageText;
        this.timestamp = timestamp;
    }

    // Constructor for new message (ID auto-generated, timestamp set automatically)
    public Message(Long userId, String username, String messageText) {
        this.userId = userId;
        this.username = username;
        this.messageText = messageText;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Message{id=" + id + ", username='" + username + "', text='" + messageText + "', time=" + timestamp + "}";
    }
}