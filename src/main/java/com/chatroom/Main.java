package com.chatroom;

import com.chatroom.controller.MessageController;
import com.chatroom.controller.UserController;
import com.chatroom.repository.MessageRepository;
import com.chatroom.repository.UserRepository;
import com.chatroom.service.MessageService;
import com.chatroom.service.UserService;
import com.chatroom.websocket.WebSocketHandler;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static spark.Spark.*;


public class Main {

    public static void main(String[] args) {
        try {


            String portFromEnv = System.getenv("PORT");
            int port = (portFromEnv != null) ? Integer.parseInt(portFromEnv) : 8080;

            port(port);


            Connection connection = initDatabase();


            UserRepository userRepository = new UserRepository(connection);
            MessageRepository messageRepository = new MessageRepository(connection);


            UserService userService = new UserService(userRepository);
            MessageService messageService = new MessageService(messageRepository, userService);


            webSocket("/ws/chat", WebSocketHandler.class);
            System.out.println("WebSocket endpoint ready at: ws://localhost:8080/ws/chat");

            // Initialize controllers (sets up routes)
            new UserController(userService);
            new MessageController(messageService);

            // Global exception handlers
            setupExceptionHandlers();

            // Wait for Spark to initialize
            awaitInitialization();

            // Startup message
            printStartupMessage();

            // Graceful shutdown
            setupShutdownHook(connection);

        } catch (Exception e) {
            System.err.println("Failed to start application: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static Connection initDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");

            String url = "jdbc:sqlite:chat.db";
            Connection connection = DriverManager.getConnection(url);

            System.out.println("Database connected successfully");
            System.out.println("Database file: chat.db");
            return connection;

        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC driver not found", e);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    private static void setupExceptionHandlers() {
        notFound((req, res) -> {
            res.type("application/json");
            return "{\"error\": \"Endpoint not found\"}";
        });

        internalServerError((req, res) -> {
            res.type("application/json");
            return "{\"error\": \"Internal server error\"}";
        });

        exception(Exception.class, (e, req, res) -> {
            res.status(500);
            res.type("application/json");
            res.body("{\"error\": \"" + e.getMessage() + "\"}");
            System.err.println("Unhandled exception: " + e.getMessage());
            e.printStackTrace();
        });
    }

    private static void printStartupMessage() {
        System.out.println("\n===========================================");
        System.out.println("Chat App Started");
        System.out.println("Server running at: http://localhost:8080");
        System.out.println("WebSocket available at: ws://localhost:8080/ws/chat");
        System.out.println("===========================================\n");
        System.out.println("Available REST API Endpoints:");
        System.out.println("  POST   /api/users/register");
        System.out.println("  POST   /api/users/login");
        System.out.println("  GET    /api/users/:username");
        System.out.println("  POST   /api/messages");
        System.out.println("  GET    /api/messages");
        System.out.println("  GET    /api/messages/user/:userId");
        System.out.println("  DELETE /api/messages/:messageId?userId=:userId");
        System.out.println("\nWebSocket Endpoint:");
        System.out.println("  WS     /ws/chat");
        System.out.println("\nReady to accept requests");
        System.out.println("Test with Postman or curl\n");
    }

    private static void setupShutdownHook(Connection connection) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down server...");

            stop();

            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                    System.out.println("Database connection closed");
                }
            } catch (SQLException e) {
                System.err.println("Error closing database: " + e.getMessage());
            }

            System.out.println("Goodbye");
        }));
    }
}
