import './Navigation.css';

export default function Navigation({ currentView, onViewChange, userRole, onLogout }) {
  const getMenuItems = () => {
    const baseItems = [
      { id: 'dashboard', label: 'Dashboard', icon: '📊' },
      { id: 'control', label: 'Sterowanie', icon: '🎛️' },
      { id: 'monitoring', label: 'Monitoring', icon: '📡' },
      { id: 'alerts', label: 'Alarmy', icon: '🚨' },
      { id: 'reports', label: 'Raporty', icon: '📈' }
    ];

    // Administrator ma dodatkowe opcje
    if (userRole === 'admin') {
      baseItems.push(
        { id: 'devices', label: 'Urządzenia', icon: '🔧' },
        { id: 'users', label: 'Użytkownicy', icon: '👥' },
        { id: 'settings', label: 'Ustawienia', icon: '⚙️' }
      );
    }

    // Najemca nie widzi niektórych opcji
    if (userRole === 'user') {
      return baseItems.filter(item => 
        ['dashboard', 'control', 'reports'].includes(item.id)
      );
    }

    return baseItems;
  };

  const menuItems = getMenuItems();

  return (
    <nav className="navigation">
      <div className="nav-header">
        <h2>SZEBI</h2>
      </div>
      <ul className="nav-menu">
        {menuItems.map(item => (
          <li 
            key={item.id}
            className={currentView === item.id ? 'active' : ''}
            onClick={() => onViewChange(item.id)}
          >
            <span className="nav-icon">{item.icon}</span>
            <span className="nav-label">{item.label}</span>
          </li>
        ))}
      </ul>
    </nav>
  );
}

