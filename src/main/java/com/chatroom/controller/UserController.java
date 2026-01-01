package com.chatroom.controller;

import com.chatroom.model.User;
import com.chatroom.service.UserService;
import com.google.gson.Gson;
import spark.Request;
import spark.Response;
import java.util.HashMap;
import java.util.Map;

import static spark.Spark.*;

public class UserController {
    private UserService userService;
    private Gson gson;

    public UserController(UserService userService) {
        this.userService = userService;
        this.gson = new Gson();
        setupRoutes();
    }

    private void setupRoutes() {

        enableCORS();

        post("/api/users/register", this::register);

        post("/api/users/login", this::login);

        get("/api/users/:username", this::getUserByUsername);
    }

    private String register(Request req, Response res) {
        try {
            System.out.println("📥 Registration request received");
            System.out.println("Request body: " + req.body());

            Map<String, String> body = gson.fromJson(req.body(), Map.class);
            String username = body.get("username");
            String password = body.get("password");

            System.out.println("Parsed username: " + username);
            System.out.println("Password length: " + (password != null ? password.length() : "null"));

            User user = userService.registerUser(username, password);

            System.out.println("✅ User registered successfully: " + user.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("message", "User registered successfully");

            res.status(201);

            res.type("application/json");

            return gson.toJson(response);

        } catch (IllegalArgumentException e) {

            System.err.println("❌ Registration validation error: " + e.getMessage());
            res.status(400);

            return createErrorResponse(e.getMessage());

        } catch (Exception e) {

            System.err.println("❌ Unexpected registration error: " + e.getMessage());
            System.err.println("Error type: " + e.getClass().getName());
            e.printStackTrace();

            res.status(500);

            return createErrorResponse("Internal server error: " + e.getMessage());
        }
    }

    private String login(Request req, Response res) {
        try {
            System.out.println("📥 Login request received");
            System.out.println("Request body: " + req.body());

            Map<String, String> body = gson.fromJson(req.body(), Map.class);
            String username = body.get("username");
            String password = body.get("password");

            System.out.println("Login attempt for username: " + username);

            User user = userService.loginUser(username, password);

            System.out.println("✅ Login successful for: " + user.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("message", "Login successful");

            res.status(200);

            res.type("application/json");
            return gson.toJson(response);

        } catch (IllegalArgumentException e) {

            System.err.println("❌ Login error: " + e.getMessage());
            res.status(401);

            return createErrorResponse(e.getMessage());

        } catch (Exception e) {
            System.err.println("❌ Unexpected login error: " + e.getMessage());
            System.err.println("Error type: " + e.getClass().getName());
            e.printStackTrace();
            res.status(500);
            return createErrorResponse("Internal server error: " + e.getMessage());
        }
    }

    private String getUserByUsername(Request req, Response res) {
        String username = req.params(":username");
        System.out.println("📥 Get user request for: " + username);

        return userService.findUserByUsername(username)
                .map(user -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("id", user.getId());
                    response.put("username", user.getUsername());
                    res.status(200);
                    res.type("application/json");
                    System.out.println("✅ User found: " + username);
                    return gson.toJson(response);
                })
                .orElseGet(() -> {
                    res.status(404);

                    System.out.println("❌ User not found: " + username);
                    return createErrorResponse("User not found");
                });
    }

    private String createErrorResponse(String error) {
        Map<String, String> errorMap = new HashMap<>();
        errorMap.put("error", error);
        return gson.toJson(errorMap);
    }

    private void enableCORS() {

        options("/*", (req, res) -> {
            String accessControlRequestHeaders = req.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                res.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }

            String accessControlRequestMethod = req.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                res.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }
            return "OK";
        });

        before((req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            res.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
            res.type("application/json");
        });
    }
}