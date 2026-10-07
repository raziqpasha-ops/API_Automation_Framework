package com.booking.pojo;

/**
 * AuthResponse POJO models the token response:
 *      { "token": "abc123" }
 *
 * Interview explanation: Deserializing the token into a typed object (instead of
 * parsing a raw string) means the framework gets compile-time safety. If the API
 * ever renames the field, our deserialization fails loudly at that one line,
 * rather than producing nulls that surface much later in a confusing way.
 */
public class AuthResponse {

    private String token;

    public AuthResponse() { }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
