import { useState } from 'react';
import './Navigation.css';

export default function Navigation({ currentView, onViewChange, userRole, onLogout, isOpen, onToggle }) {
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

  const handleItemClick = (itemId) => {
    onViewChange(itemId);
    // Zamknij menu na mobile po kliknięciu
    if (window.innerWidth <= 768 && onToggle) {
      onToggle();
    }
  };

  return (
    <>
      {/* Overlay dla mobile */}
      {isOpen && <div className="nav-overlay" onClick={onToggle}></div>}
      
      <nav className={`navigation ${isOpen ? 'nav-open' : ''}`}>
        <div className="nav-header">
          <h2>SZEBI</h2>
          <button className="nav-close-btn" onClick={onToggle}>✕</button>
        </div>
        <ul className="nav-menu">
          {menuItems.map(item => (
            <li 
              key={item.id}
              className={currentView === item.id ? 'active' : ''}
              onClick={() => handleItemClick(item.id)}
            >
              <span className="nav-icon">{item.icon}</span>
              <span className="nav-label">{item.label}</span>
            </li>
          ))}
        </ul>
      </nav>
    </>
  );
}

