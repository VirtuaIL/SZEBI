import { useState } from 'react';
import './LoginPanel.css';

// Automatyczne wykrywanie adresu API
const getApiUrl = () => {
  const hostname = window.location.hostname;
  const protocol = window.location.protocol;
  return `${protocol}//${hostname}:8080/api/login`;
};
const API_URL = getApiUrl();

function Login({ onLogin }) {
  const [emailInput, setEmailInput] = useState('');
  const [passwordInput, setPasswordInput] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const response = await fetch(API_URL, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          email: emailInput,
          password: passwordInput
        })
      });

      const data = await response.json();

      if (response.ok && data.success) {
        // Logowanie udane - przekaż rolę do App.jsx
        onLogin(data.role, data);
      } else {
        // Błąd logowania
        setError(data.error || 'Błędne dane logowania');
        setPasswordInput('');
      }
    } catch (err) {
      console.error('Błąd podczas logowania:', err);
      setError('Nie można połączyć się z serwerem. Upewnij się, że backend jest uruchomiony.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-box">
        <h2>System Logowania</h2>
        <form onSubmit={handleSubmit}>
          <div className="input-group">
            <label>Email:</label>
            <input 
              type="email" 
              value={emailInput}
              onChange={(e) => setEmailInput(e.target.value)}
              required
              disabled={loading}
            />
          </div>
         <div className="input-group">
           <label>Hasło:</label>
            <input 
             type="password" 
             value={passwordInput}
             onChange={(e) => setPasswordInput(e.target.value)}
             required
             disabled={loading}
           />
         </div>
          {error && <p className="error-msg">{error}</p>}
          <button type="submit" className="login-btn" disabled={loading}>
            {loading ? 'LOGOWANIE...' : 'ZALOGUJ'}
          </button>
          <p style={{ fontSize: '12px', color: '#666', marginTop: '10px' }}>
            Przykładowe konta:<br/>
            admin@szebi.com / admin123<br/>
            inzynier@szebi.com / inzynier123<br/>
            natalia.nowak@szebi.com / natalia123
          </p>
        </form>
      </div>
    </div>
  );
}

export default Login;