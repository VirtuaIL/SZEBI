import { useState, useEffect } from 'react';
import './App.css';
import Login from './Panels/LoginPanel';
import AdminPanel from './Panels/AdminPanel';
import EngineerPanel from './Panels/EngineerPanel';
import UserPanel from './Panels/UserPanel';
import ToastContainer from './components/ToastContainer';
import ErrorBoundary from './components/ErrorBoundary';

function App() {
  // 1. INICJALIZACJA: Sprawdzamy LocalStorage przy pierwszym uruchomieniu
  // Jeśli coś tam jest, używamy tego. Jeśli nie, ustawiamy null.
  const [userRole, setUserRole] = useState(() => {
    return localStorage.getItem('user_role'); 
  });

  // Funkcja pomocnicza: Zaloguj i zapisz
  const handleLogin = (role, userData = null) => {
    setUserRole(role);                 // Zmień stan w React (odświeży widok)
    localStorage.setItem('user_role', role); // Zapisz trwale w przeglądarce
    if (userData) {
      localStorage.setItem('user_data', JSON.stringify(userData)); // Zapisz dane użytkownika
    }
  };

  // Funkcja pomocnicza: Wyloguj i wyczyść
  const handleLogout = () => {
    setUserRole(null);                 // Wyczyść stan
    localStorage.removeItem('user_role');    // Usuń z pamięci przeglądarki
  };

  return (
    <ErrorBoundary>
      {!userRole ? (
        <Login onLogin={handleLogin} />
      ) : (
        <>
          {userRole === 'admin' && <AdminPanel onLogout={handleLogout} />}
          {userRole === 'engineer' && <EngineerPanel onLogout={handleLogout} />}
          {userRole === 'user' && <UserPanel onLogout={handleLogout} />}
          {userRole !== 'admin' && userRole !== 'engineer' && userRole !== 'user' && (
            <div>
              Nieznana rola! 
              <button onClick={handleLogout}>Wróć do logowania</button>
            </div>
          )}
        </>
      )}
      <ToastContainer />
    </ErrorBoundary>
  );
}

export default App;