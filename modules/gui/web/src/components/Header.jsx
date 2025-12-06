import './Header.css';

export default function Header({ userData, onLogout }) {
  const getUserDisplayName = () => {
    if (userData) {
      if (userData.imie && userData.nazwisko) {
        return `${userData.imie} ${userData.nazwisko}`;
      }
      return userData.email || 'Użytkownik';
    }
    const stored = localStorage.getItem('user_data');
    if (stored) {
      try {
        const data = JSON.parse(stored);
        if (data.imie && data.nazwisko) {
          return `${data.imie} ${data.nazwisko}`;
        }
        return data.email || 'Użytkownik';
      } catch (e) {
        return 'Użytkownik';
      }
    }
    return 'Użytkownik';
  };

  const getRoleDisplayName = () => {
    const role = localStorage.getItem('user_role');
    switch (role) {
      case 'admin': return 'Administrator';
      case 'engineer': return 'Inżynier';
      case 'user': return 'Najemca';
      default: return 'Użytkownik';
    }
  };

  return (
    <header className="app-header">
      <div className="header-content">
        <div className="header-user-info">
          <span className="user-name">{getUserDisplayName()}</span>
          <span className="user-role">{getRoleDisplayName()}</span>
        </div>
        <button className="header-logout-btn" onClick={onLogout}>
          Wyloguj
        </button>
      </div>
    </header>
  );
}

