export default function UserPanel({ onLogout }) {
  return (
    <div style={{ padding: '20px', backgroundColor: '#e8f5e9', height: '100vh' }}>
      <h1>👤 Panel Użytkownika</h1>
      <p>Podgląd podstawowych danych i zgłaszanie awarii.</p>
      <button onClick={onLogout}>Wyloguj</button>
    </div>
  );
}