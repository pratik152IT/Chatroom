package com.chatroom.service;

import com.chatroom.model.Message;
import com.chatroom.model.User;
import com.chatroom.repository.MessageRepository;
import java.util.List;
import java.util.Optional;


public class MessageService {
    private MessageRepository messageRepository;
    private UserService userService;

    public MessageService(MessageRepository messageRepository, UserService userService) {
        this.messageRepository = messageRepository;
        this.userService = userService;
    }


    public Message sendMessage(Long userId, String messageText) {
        // Validate message text
        if (messageText == null || messageText.trim().isEmpty()) {
            throw new IllegalArgumentException("Message cannot be empty");
        }

        if (messageText.length() > 1000) {
            throw new IllegalArgumentException("Message too long (max 1000 characters)");
        }

        // Verify user exists
        // This prevents orphaned messages from non-existent users
        User user = userService.findUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Create and save message
        // Timestamp is set automatically in Message constructor
        Message message = new Message(userId, user.getUsername(), messageText.trim());
        return messageRepository.save(message);
    }


    public List<Message> getAllMessages() {
        return messageRepository.findAllOrderByTimestamp();
    }

    /**
     * Get all messages sent by a specific user
     * Useful for viewing a user's message history
     *
     * @param userId - ID of the user
     * @return List of user's messages
     */
    public List<Message> getMessagesByUser(Long userId) {
        return messageRepository.findByUserId(userId);
    }

    public void deleteMessage(Long messageId, Long userId) {
        // Find message
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        // Check ownership - CRITICAL SECURITY CHECK
        // This prevents users from deleting other people's messages
        if (!message.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Access denied: You can only delete your own messages");
        }


        messageRepository.deleteById(messageId);
    }

    public Optional<Message> getMessageById(Long messageId) {
        return messageRepository.findById(messageId);
    }
}