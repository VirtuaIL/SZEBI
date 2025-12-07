package org.example.alerts;

public class NotificationService {

    public void wyslijPush(String tresc, String odbiorca) {
        System.out.println(">>> [PUSH] Do: " + odbiorca + " | Treść: " + tresc);
    }

    public void wyslijEmail(String temat, String tresc) {
        System.out.println(">>> [EMAIL] Temat: " + temat + " | Treść: " + tresc);
    }
}