package org.example.runner;

import io.javalin.Javalin;
import io.javalin.http.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AuthController {
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public AuthController(AuthService authService) {
        this.authService = authService;
        this.objectMapper = new ObjectMapper();
    }

    public void setupRoutes(Javalin app) {
        // Endpoint do logowania
        app.post("/api/login", this::handleLogin);
        
        // Endpoint do sprawdzenia statusu (opcjonalnie)
        app.get("/api/health", ctx -> {
            ctx.json(new ObjectNode(objectMapper.getNodeFactory()).put("status", "ok"));
        });
    }

    private void handleLogin(Context ctx) {
        try {
            // Pobierz dane z request body
            ObjectNode requestBody = objectMapper.readValue(ctx.body(), ObjectNode.class);
            String email = requestBody.has("email") ? requestBody.get("email").asText() : null;
            String password = requestBody.has("password") ? requestBody.get("password").asText() : null;

            if (email == null || password == null) {
                ctx.status(400);
                ObjectNode error = objectMapper.createObjectNode();
                error.put("error", "Email i hasło są wymagane");
                ctx.json(error);
                return;
            }

            // Weryfikuj dane logowania
            AuthService.LoginResult result = authService.authenticate(email, password);

            if (result == null) {
                ctx.status(401);
                ObjectNode error = objectMapper.createObjectNode();
                error.put("error", "Błędne dane logowania");
                ctx.json(error);
                return;
            }

            // Zwróć sukces z danymi użytkownika
            ObjectNode response = objectMapper.createObjectNode();
            response.put("success", true);
            response.put("userId", result.getUserId());
            response.put("email", result.getEmail());
            response.put("imie", result.getImie());
            response.put("nazwisko", result.getNazwisko());
            response.put("role", result.getRole());

            ctx.status(200);
            ctx.json(response);

        } catch (Exception e) {
            ctx.status(500);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("error", "Błąd serwera: " + e.getMessage());
            ctx.json(error);
            e.printStackTrace();
        }
    }
}


