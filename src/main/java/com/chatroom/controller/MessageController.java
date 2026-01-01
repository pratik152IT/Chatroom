package com.chatroom.controller;

import com.chatroom.model.Message;
import com.chatroom.service.MessageService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import spark.Request;
import spark.Response;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static spark.Spark.*;

public class MessageController {
    private MessageService messageService;
    private Gson gson;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;

        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();

        setupRoutes();
    }

    private void setupRoutes() {

        post("/api/messages", this::sendMessage);

        get("/api/messages", this::getAllMessages);

        get("/api/messages/user/:userId", this::getMessagesByUser);

        delete("/api/messages/:messageId", this::deleteMessage);
    }

    private String sendMessage(Request req, Response res) {
        try {

            Map<String, Object> body = gson.fromJson(req.body(), Map.class);

            Long userId = ((Double) body.get("userId")).longValue();
            String messageText = (String) body.get("messageText");

            Message message = messageService.sendMessage(userId, messageText);

            res.status(201);

            res.type("application/json");
            return gson.toJson(message);

        } catch (IllegalArgumentException e) {
            res.status(400);

            return createErrorResponse(e.getMessage());

        } catch (Exception e) {
            res.status(500);
            return createErrorResponse("Internal server error: " + e.getMessage());
        }
    }

    private String getAllMessages(Request req, Response res) {
        try {
            List<Message> messages = messageService.getAllMessages();

            res.status(200);
            res.type("application/json");
            return gson.toJson(messages);

        } catch (Exception e) {
            res.status(500);
            return createErrorResponse("Internal server error");
        }
    }

    private String getMessagesByUser(Request req, Response res) {
        try {
            Long userId = Long.parseLong(req.params(":userId"));
            List<Message> messages = messageService.getMessagesByUser(userId);

            res.status(200);
            res.type("application/json");
            return gson.toJson(messages);

        } catch (NumberFormatException e) {
            res.status(400);
            return createErrorResponse("Invalid user ID");

        } catch (Exception e) {
            res.status(500);
            return createErrorResponse("Internal server error");
        }
    }

    private String deleteMessage(Request req, Response res) {
        try {
            Long messageId = Long.parseLong(req.params(":messageId"));
            Long userId = Long.parseLong(req.queryParams("userId"));

            messageService.deleteMessage(messageId, userId);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Message deleted successfully");

            res.status(200);
            res.type("application/json");
            return gson.toJson(response);

        } catch (NumberFormatException e) {

            res.status(400);
            return createErrorResponse("Invalid message or user ID");

        } catch (IllegalArgumentException e) {

            res.status(403);

            return createErrorResponse(e.getMessage());

        } catch (Exception e) {
            res.status(500);
            return createErrorResponse("Internal server error");
        }
    }

    private String createErrorResponse(String error) {
        Map<String, String> errorMap = new HashMap<>();
        errorMap.put("error", error);
        return gson.toJson(errorMap);
    }
}

class LocalDateTimeAdapter implements com.google.gson.JsonSerializer<LocalDateTime>,
        com.google.gson.JsonDeserializer<LocalDateTime> {

    @Override
    public com.google.gson.JsonElement serialize(LocalDateTime dateTime,
                                                 java.lang.reflect.Type type,
                                                 com.google.gson.JsonSerializationContext context) {
        return new com.google.gson.JsonPrimitive(dateTime.toString());
    }

    @Override
    public LocalDateTime deserialize(com.google.gson.JsonElement json,
                                     java.lang.reflect.Type type,
                                     com.google.gson.JsonDeserializationContext context) {
        return LocalDateTime.parse(json.getAsString());
    }
}