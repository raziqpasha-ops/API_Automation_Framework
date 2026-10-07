package com.booking.services;

import com.booking.constants.Endpoints;
import com.booking.config.ConfigManager;
import com.booking.pojo.AuthRequest;
import com.booking.pojo.AuthResponse;
import com.booking.spec.SpecFactory;
import io.qameta.allure.Step;
import io.restassured.RestAssured;

/**
 * AuthService wraps ALL token-related API calls behind simple reusable methods.
 *
 * Interview explanation: This is the "service layer" pattern. Tests should READ
 * like business scenarios, not like HTTP plumbing. So the test says
 *     AuthService.createToken();
 * and ALL the details (endpoint, spec, POJO, deserialization) live here.
 * If the auth API changes tomorrow, we fix ONE method — no test changes at all.
 */
public class AuthService {

    /**
     * Creates and returns the auth token using credentials from config.
     * This is a reusable "keyword" — any test in the suite can call it in one line.
     */
    @Step("Create auth token for user")
    public static String createToken() {

        // Build the request payload as a POJO with credentials from ConfigManager.
        // We never hard-code admin/password123 inside a test — config is the source.
        AuthRequest authRequest = new AuthRequest(ConfigManager.getUsername(),
                ConfigManager.getPassword());

        // given().spec(...) injects our shared request spec (base URI + JSON + logging).
        // .body(authRequest) serializes the POJO into JSON automatically via Jackson.
        // .when().post(Endpoints.AUTH) performs the actual HTTP POST to /auth.
        // .as(AuthResponse.class) deserializes the JSON response back into a POJO.
        AuthResponse authResponse =
                RestAssured
                        .given()
                            .spec(SpecFactory.getRequestSpec())
                            .body(authRequest)
                        .when()
                            .post(Endpoints.AUTH)
                        .then()
                            .assertThat()
                            .statusCode(200)          // token endpoint returns 200 on success
                            .extract()
                            .as(AuthResponse.class);  // whole response -> AuthResponse POJO

        // Return just the token string to the caller. Tests store it and pass it
        // to write operations (PUT / PATCH / DELETE) as a Cookie header.
        return authResponse.getToken();
    }
}
