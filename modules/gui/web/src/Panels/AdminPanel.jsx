import './AdminPanel.css';
export default function AdminPanel({ onLogout }) {
  return (
    <body>
        <header><h1>👑 Panel Administratora</h1></header>

      
      <p>Masz dostęp do konfiguracji systemu i zarządzania użytkownikami.</p>
      <button onClick={onLogout} id="1">Wyloguj</button>




    </body>
  );
}