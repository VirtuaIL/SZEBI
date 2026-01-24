package org.example.runner;

import org.example.DTO.Uzytkownik;
import org.example.DTO.Rola;
import org.example.PostgresDataStorage;
import org.mindrot.jbcrypt.BCrypt;

public class AuthService {
    private final PostgresDataStorage databaseStorage;

    public AuthService(PostgresDataStorage databaseStorage) {
        this.databaseStorage = databaseStorage;
    }

    /**
     * Weryfikuje dane logowania użytkownika
     * @param email Email użytkownika
     * @param password Hasło w formie plaintext
     * @return LoginResult z informacją o użytkowniku i roli, lub null jeśli logowanie nie powiodło się
     */
    public LoginResult authenticate(String email, String password) {
        if (email == null || password == null || email.isEmpty() || password.isEmpty()) {
            return null;
        }

        Uzytkownik user = databaseStorage.getUserByEmail(email);
        if (user == null) {
            return null;
        }

        String hashedPassword = user.getHasloHash();
        
        // Sprawdź czy hasło jest zahashowane (BCrypt) czy plaintext (dla testów)
        boolean passwordValid = false;
        
        if (hashedPassword != null) {
            // Jeśli hash zaczyna się od $2a$, $2b$ lub $2y$ to jest BCrypt
            if (hashedPassword.startsWith("$2")) {
                passwordValid = BCrypt.checkpw(password, hashedPassword);
            } else if (hashedPassword.equals("...hash...") || hashedPassword.isEmpty()) {
                // Dla testów - jeśli w bazie jest placeholder, sprawdź proste hasła
                // admin@szebi.com -> admin123
                // inzynier@szebi.com -> inzynier123
                // natalia.nowak@szebi.com -> natalia.nowak123
                String emailPrefix = email.split("@")[0]; // admin@szebi.com -> admin
                String expectedPassword = emailPrefix + "123";
                passwordValid = password.equals(expectedPassword);
            } else {
                // Proste porównanie dla testów (nie używaj w produkcji!)
                passwordValid = hashedPassword.equals(password);
            }
        }

        if (!passwordValid) {
            return null;
        }

        // Pobierz rolę użytkownika
        Rola rola = databaseStorage.getRoleById(user.getRolaId());
        if (rola == null) {
            return null;
        }

        // Mapuj nazwę roli z bazy na format używany w frontendzie
        String roleName = mapRoleToFrontend(rola.getNazwaRoli());

        return new LoginResult(
            user.getId(),
            user.getEmail(),
            user.getImie(),
            user.getNazwisko(),
            roleName
        );
    }

    /**
     * Mapuje nazwę roli z bazy danych na format używany w frontendzie
     */
    private String mapRoleToFrontend(String nazwaRoli) {
        if (nazwaRoli == null) {
            return "user";
        }
        
        String lower = nazwaRoli.toLowerCase();
        if (lower.contains("admin") || lower.contains("administrator")) {
            return "admin";
        } else if (lower.contains("inżynier") || lower.contains("inzynier") || lower.contains("engineer")) {
            return "engineer";
        } else {
            return "user";
        }
    }

    /**
     * Klasa pomocnicza do zwracania wyniku logowania
     */
    public static class LoginResult {
        private final int userId;
        private final String email;
        private final String imie;
        private final String nazwisko;
        private final String role;

        public LoginResult(int userId, String email, String imie, String nazwisko, String role) {
            this.userId = userId;
            this.email = email;
            this.imie = imie;
            this.nazwisko = nazwisko;
            this.role = role;
        }

        public int getUserId() { return userId; }
        public String getEmail() { return email; }
        public String getImie() { return imie; }
        public String getNazwisko() { return nazwisko; }
        public String getRole() { return role; }
    }
}

