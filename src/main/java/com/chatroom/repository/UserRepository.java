package com.chatroom.repository;

import com.chatroom.model.User;
import java.sql.*;
import java.util.Optional;


public class UserRepository {
    private Connection connection;

    public UserRepository(Connection connection) {
        this.connection = connection;
        createTable();
    }

    /**
     * Create users table if it doesn't exist
     * Called automatically when repository is initialized
     */
    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT UNIQUE NOT NULL, " +
                "password TEXT NOT NULL)";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            System.out.println("✅ Users table ready");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create users table", e);
        }
    }

    /**
     * Save a new user to database
     * @param user - User object with username and password
     * @return User object with generated ID
     */
    public User save(User user) {
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";

        System.out.println("💾 Attempting to save user: " + user.getUsername());

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());

            System.out.println("📝 Executing INSERT query...");
            pstmt.executeUpdate();

            // SQLite-specific way to get last inserted ID
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    user.setId(rs.getLong(1));
                    System.out.println("✅ User saved successfully with ID: " + user.getId());
                }
            }

            return user;

        } catch (SQLException e) {
            System.err.println("❌ Database error while saving user");
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
            System.err.println("Message: " + e.getMessage());
            e.printStackTrace();

            if (e.getMessage().contains("UNIQUE constraint failed")) {
                throw new RuntimeException("Username already exists");
            }
            throw new RuntimeException("Failed to save user", e);
        }
    }

    /**
     * Find user by username
     * @param username - Username to search for
     * @return Optional containing User if found, empty otherwise
     */
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT id, username, password FROM users WHERE username = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                User user = new User(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("password")
                );
                return Optional.of(user);
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user", e);
        }
    }

    /**
     * Find user by ID
     * @param id - User ID
     * @return Optional containing User if found
     */
    public Optional<User> findById(Long id) {
        String sql = "SELECT id, username, password FROM users WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                User user = new User(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("password")
                );
                return Optional.of(user);
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user by ID", e);
        }
    }

    /**
     * Check if username already exists
     * @param username - Username to check
     * @return true if exists, false otherwise
     */
    public boolean existsByUsername(String username) {
        return findByUsername(username).isPresent();
    }
}