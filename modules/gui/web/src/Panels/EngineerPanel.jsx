import { useState } from 'react';
import './EngineerPanel.css';

export default function AdminPanel({ onLogout }) {
  const [isMenuOpen, setIsMenuOpen] = useState(false);

  const toggleMenu = () => {
    setIsMenuOpen(!isMenuOpen);
  };

  return (
    <div className="engineer-container">
      
      {/* --- NAGŁÓWEK (Przyklejony do góry) --- */}
      <nav className="navbar-engineer">
        <h1 className="nav-logo-engineer">Panel Inżyniera</h1>
        
        {/* Przycisk Hamburgera */}
        <div className="menu" onClick={toggleMenu}>
          ☰
        </div>

        {/* --- ROZWIJANE MENU --- */}
        {/* Wyświetla się tylko gdy isMenuOpen === true */}
        {isMenuOpen && (
          <div className="dropdown-menu-engineer">
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
        <div className="info-box-engineer">
          <h2>Witaj w panelu Inżyniera</h2>
          <p>Będziesz miał dostęp do sporej ilośći rzeczy</p>
        </div>
        
        {/* Tu możesz dodawać kolejne klocki, wykresy itp. */}
      </main>

    </div>
  );
}