package org.example.interfaces;

import org.example.DTO.Uzytkownik;
import org.example.DTO.Rola;
import org.example.DTO.Budynek;
import java.util.List;

public interface IUserData {
    Uzytkownik getUserByEmail(String email);

    Uzytkownik getUserById(int userId);

    Rola getRoleById(int rolaId);

    Uzytkownik saveUser(Uzytkownik user);
}
