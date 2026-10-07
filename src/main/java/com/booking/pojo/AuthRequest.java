package com.booking.pojo;

/**
 * AuthRequest POJO models the login request body:
 *      { "username": "admin", "password": "password123" }
 *
 * Interview explanation: For the token call we COULD have written the JSON by hand,
 * but hand-written JSON strings break silently and are hard to maintain. Using a
 * POJO keeps the request type-safe and consistent with the rest of the framework.
 */
public class AuthRequest {

    private String username;
    private String password;

    public AuthRequest() { }

    // Convenience constructor so a test can build the login payload in one line.
    public AuthRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
