import { useState } from 'react';
import './AdminPanel.css';

export default function AdminPanel({ onLogout }) {
  const [isMenuOpen, setIsMenuOpen] = useState(false);

  const toggleMenu = () => {
    setIsMenuOpen(!isMenuOpen);
  };

  return (
    <div className="admin-container">
      
      {/* --- NAGŁÓWEK (Przyklejony do góry) --- */}
      <nav className="navbar-admin">
        <h1 className="nav-logo-admin">Panel Administratora</h1>
        
        {/* Przycisk Hamburgera */}
        <div className="menu" onClick={toggleMenu}>
          ☰
        </div>

        {/* --- ROZWIJANE MENU --- */}
        {/* Wyświetla się tylko gdy isMenuOpen === true */}
        {isMenuOpen && (
          <div className="dropdown-menu-admin">
            <ul>
              <li>Ustawienia</li>
              <li>Użytkownicy</li>
              <li>Logi systemu</li>
              <hr />
              <li onClick={onLogout} className="logout-option">
                Wyloguj się
              </li>
            </ul>
          </div>
        )}
      </nav>

      {/* --- TREŚĆ GŁÓWNA --- */}
      <main className="content">
        <div className="info-box-admin">
          <h2>Witaj w panelu Administratora</h2>
          <p>Masz dostęp do konfiguracji systemu i zarządzania użytkownikami.</p>
        </div>
        
        {/* Tu możesz dodawać kolejne klocki, wykresy itp. */}
      </main>

    </div>
  );
}