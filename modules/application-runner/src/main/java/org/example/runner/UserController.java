package org.example.runner;

import io.javalin.Javalin;
import io.javalin.http.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.DTO.Uzytkownik;

public class UserController {
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public UserController(AuthService authService) {
        this.authService = authService;
        this.objectMapper = new ObjectMapper();
    }

    public void setupRoutes(Javalin app) {
        // Endpoint do rejestracji/dodawania użytkownika
        app.post("/api/users", this::handleCreateUser);

        // Endpoint do pobierania listy użytkowników
        app.get("/api/users", this::handleGetAllUsers);

        // Endpoint do aktualizacji użytkownika
        app.put("/api/users/{id}", this::handleUpdateUser);

        // Endpoint do usuwania użytkownika
        app.delete("/api/users/{id}", this::handleDeleteUser);
    }

    private void handleCreateUser(Context ctx) {
        try {
            ObjectNode requestBody = objectMapper.readValue(ctx.body(), ObjectNode.class);

            String imie = requestBody.has("imie") ? requestBody.get("imie").asText() : "";
            String nazwisko = requestBody.has("nazwisko") ? requestBody.get("nazwisko").asText() : "";
            String email = requestBody.has("email") ? requestBody.get("email").asText() : null;
            String password = requestBody.has("password") ? requestBody.get("password").asText() : null;
            String telefon = requestBody.has("telefon") ? requestBody.get("telefon").asText() : "";
            int rolaId = requestBody.has("rolaId") ? requestBody.get("rolaId").asInt() : 1; // Default role: user

            if (email == null || password == null || email.isEmpty() || password.isEmpty()) {
                ctx.status(400);
                ctx.json(createErrorResponse("Email i hasło są wymagane"));
                return;
            }

            try {
                Uzytkownik user = authService.registerUser(imie, nazwisko, email, password, rolaId, telefon);

                ObjectNode response = objectMapper.createObjectNode();
                response.put("success", true);
                response.put("userId", user.getId());
                response.put("message", "Użytkownik został utworzony");

                ctx.status(201);
                ctx.json(response);
            } catch (IllegalArgumentException e) {
                ctx.status(409); // Conflict
                ctx.json(createErrorResponse(e.getMessage()));
            }

        } catch (Exception e) {
            ctx.status(500);
            ctx.json(createErrorResponse("Błąd serwera: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    private void handleGetAllUsers(Context ctx) {
        try {
            java.util.List<Uzytkownik> users = authService.getAllUsers();

            // Mapowanie na prostszy obiekt JSON (bez hasła itp)
            com.fasterxml.jackson.databind.node.ArrayNode usersArray = objectMapper.createArrayNode();

            for (Uzytkownik u : users) {
                ObjectNode userNode = objectMapper.createObjectNode();
                userNode.put("id", u.getId());
                userNode.put("imie", u.getImie());
                userNode.put("nazwisko", u.getNazwisko());
                userNode.put("email", u.getEmail());
                userNode.put("telefon", u.getTelefon());
                // Mapowanie rolaId na tekst
                String rola = "user";
                if (u.getRolaId() == 1)
                    rola = "admin";
                else if (u.getRolaId() == 2)
                    rola = "engineer";
                userNode.put("role", rola);

                usersArray.add(userNode);
            }

            ctx.json(usersArray);
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(createErrorResponse("Błąd pobierania listy użytkowników: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    private void handleUpdateUser(Context ctx) {
        try {
            int userId = Integer.parseInt(ctx.pathParam("id"));
            ObjectNode requestBody = objectMapper.readValue(ctx.body(), ObjectNode.class);

            String imie = requestBody.has("imie") ? requestBody.get("imie").asText() : null;
            String nazwisko = requestBody.has("nazwisko") ? requestBody.get("nazwisko").asText() : null;
            String email = requestBody.has("email") ? requestBody.get("email").asText() : null;
            String password = requestBody.has("password") ? requestBody.get("password").asText() : null;
            String telefon = requestBody.has("telefon") ? requestBody.get("telefon").asText() : null;
            int rolaId = requestBody.has("rolaId") ? requestBody.get("rolaId").asInt() : -1;

            try {
                Uzytkownik updatedUser = authService.updateUser(userId, imie, nazwisko, email, password, rolaId, telefon);

                if (updatedUser != null) {
                    ObjectNode response = objectMapper.createObjectNode();
                    response.put("success", true);
                    response.put("userId", updatedUser.getId());
                    response.put("message", "Użytkownik został zaktualizowany");

                    ctx.status(200);
                    ctx.json(response);
                } else {
                    ctx.status(404);
                    ctx.json(createErrorResponse("Nie udało się zaktualizować użytkownika"));
                }
            } catch (IllegalArgumentException e) {
                ctx.status(400);
                ctx.json(createErrorResponse(e.getMessage()));
            }

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(createErrorResponse("Nieprawidłowe ID użytkownika"));
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(createErrorResponse("Błąd serwera: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    private void handleDeleteUser(Context ctx) {
        try {
            int userId = Integer.parseInt(ctx.pathParam("id"));

            try {
                boolean deleted = authService.deleteUser(userId);

                if (deleted) {
                    ObjectNode response = objectMapper.createObjectNode();
                    response.put("success", true);
                    response.put("message", "Użytkownik został usunięty");

                    ctx.status(200);
                    ctx.json(response);
                } else {
                    ctx.status(404);
                    ctx.json(createErrorResponse("Nie udało się usunąć użytkownika"));
                }
            } catch (IllegalArgumentException e) {
                ctx.status(404);
                ctx.json(createErrorResponse(e.getMessage()));
            }

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(createErrorResponse("Nieprawidłowe ID użytkownika"));
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(createErrorResponse("Błąd serwera: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    private ObjectNode createErrorResponse(String message) {
        ObjectNode error = objectMapper.createObjectNode();
        error.put("error", message);
        return error;
    }
}
