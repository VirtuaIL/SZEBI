export default function EngineerPanel({ onLogout }) {
  return (
    <div style={{ padding: '20px', backgroundColor: '#e3f2fd', height: '100vh' }}>
      <h1>⚙️ Panel Inżyniera</h1>
      <p>Tu są wykresy, parametry maszyn i logi techniczne.</p>
      <button onClick={onLogout}>Wyloguj</button>
    </div>
  );
}