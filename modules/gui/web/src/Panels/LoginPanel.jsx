import { useState } from 'react';
import './LoginPanel.css';

// SYMULACJA BAZY DANYCH
const USERS = [
  { login: 'admin', pass: 'admin123', role: 'admin' },
  { login: 'inz',   pass: 'inz123',   role: 'engineer' },
  { login: 'user',  pass: 'user123',  role: 'user' }
];

function Login({ onLogin }) {
  const [loginInput, setLoginInput] = useState('');
  const [passwordInput, setPasswordInput] = useState('');
  const [error, setError] = useState('');

  const handleSubmit = (e) => {
    e.preventDefault();
    
    const foundUser = USERS.find(u => u.login === loginInput && u.pass === passwordInput);

    if (foundUser) {
        onLogin(foundUser.role);
    } else {
        setError('Błędne dane! Spróbuj: admin/admin123, inz/inz123, user/user123');
        setLoginInput('');
        setPasswordInput('');
    }
  };

  return (
    <div className="login-container">
      <div className="login-box">
        <h2>System Logowania</h2>
        <form onSubmit={handleSubmit}>
          <div className="input-group">
            <label>Login:</label>
            <input 
              type="text" 
              value={loginInput}
               onChange={(e) => setLoginInput(e.target.value)}
             />
          </div>
         <div className="input-group">
           <label>Hasło:</label>
            <input 
             type="password" 
             value={passwordInput}
             onChange={(e) => setPasswordInput(e.target.value)}
           />
         </div>
          {error && <p className="error-msg">{error}</p>}
          <button type="submit" className="login-btn">ZALOGUJ</button>
        </form>
      </div>
    </div>
  );
}

export default Login;