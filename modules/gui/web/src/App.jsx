import { useState, useEffect } from 'react'; // useEffect opcjonalny, tu zrobimy to prościej
import './App.css';
import Login from './Panels/LoginPanel'; // Pamiętaj o dobrej ścieżce do Twojego pliku!
import AdminPanel from './Panels/AdminPanel';
import EngineerPanel from './Panels/EngineerPanel';
import UserPanel from './Panels/UserPanel';

function App() {
  // 1. INICJALIZACJA: Sprawdzamy LocalStorage przy pierwszym uruchomieniu
  // Jeśli coś tam jest, używamy tego. Jeśli nie, ustawiamy null.
  const [userRole, setUserRole] = useState(() => {
    return localStorage.getItem('user_role'); 
  });

  // Funkcja pomocnicza: Zaloguj i zapisz
  const handleLogin = (role) => {
    setUserRole(role);                 // Zmień stan w React (odświeży widok)
    localStorage.setItem('user_role', role); // Zapisz trwale w przeglądarce
  };

  // Funkcja pomocnicza: Wyloguj i wyczyść
  const handleLogout = () => {
    setUserRole(null);                 // Wyczyść stan
    localStorage.removeItem('user_role');    // Usuń z pamięci przeglądarki
  };

  // --- Reszta kodu bez zmian (tylko podmieniamy funkcje) ---

  if (!userRole) {
    // Przekazujemy naszą nową funkcję handleLogin
    return <Login onLogin={handleLogin} />;
  }

  switch (userRole) {
    case 'admin':
      return <AdminPanel onLogout={handleLogout} />;
    
    case 'engineer':
      return <EngineerPanel onLogout={handleLogout} />;
    
    case 'user':
      return <UserPanel onLogout={handleLogout} />;
    
    default:
      // Zabezpieczenie: jeśli w storage jest coś dziwnego, czyścimy to
      return (
        <div>
          Nieznana rola! 
          <button onClick={handleLogout}>Wróć do logowania</button>
        </div>
      );
  }
}

export default App;