package org.example.alerts;

public class NotificationService {

    public void wyslijPush(String tresc, String odbiorca) {
        // Wymaganie niefunkcjonalne: czas reakcji < 30s [cite: 17]
        System.out.println(">>> [PUSH] Do: " + odbiorca + " | Treść: " + tresc);
    }

    public void wyslijEmail(String temat, String tresc) {
        System.out.println(">>> [EMAIL] Temat: " + temat + " | Treść: " + tresc);
    }
}