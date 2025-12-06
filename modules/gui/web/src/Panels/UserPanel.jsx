import { useState } from 'react';
import './UserPanel.css';

export default function AdminPanel({ onLogout }) {
  const [isMenuOpen, setIsMenuOpen] = useState(false);

  const toggleMenu = () => {
    setIsMenuOpen(!isMenuOpen);
  };

  return (
    <div className="user-container">
      
      {/* --- NAGŁÓWEK (Przyklejony do góry) --- */}
      <nav className="navbar-user">
        <h1 className="nav-logo-user">Panel Użytkownika</h1>
        
        {/* Przycisk Hamburgera */}
        <div className="menu" onClick={toggleMenu}>
          ☰
        </div>

        {/* --- ROZWIJANE MENU --- */}
        {/* Wyświetla się tylko gdy isMenuOpen === true */}
        {isMenuOpen && (
          <div className="dropdown-menu-user">
            <ul>
              <li>Ustawienia</li>
              <li>Profil</li>
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
        <div className="info-box">
          <h2>Witaj w panelu Użytkownika</h2>
          <p>Nie masz na razie dostępu do niczego ;P</p>
        </div>
        
        {/* Tu możesz dodawać kolejne klocki, wykresy itp. */}
      </main>

    </div>
  );
}