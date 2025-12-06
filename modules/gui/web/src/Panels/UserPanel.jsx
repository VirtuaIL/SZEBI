import { useState } from 'react';
import './UserPanel.css';
import Navigation from '../components/Navigation';
import Header from '../components/Header';
import ControlPanel from '../components/ControlPanel';
import Dashboard from '../components/Dashboard';
import Reports from '../components/Reports';

export default function UserPanel({ onLogout }) {
  const [currentView, setCurrentView] = useState('dashboard');
  const [menuOpen, setMenuOpen] = useState(false);
  const userRole = 'user';
  
  const userData = (() => {
    const stored = localStorage.getItem('user_data');
    if (stored) {
      try {
        return JSON.parse(stored);
      } catch (e) {
        return null;
      }
    }
    return null;
  })();

  const renderView = () => {
    switch (currentView) {
      case 'dashboard':
        return <Dashboard userRole={userRole} />;
      case 'control':
        return <ControlPanel userRole={userRole} />;
      case 'reports':
        return <Reports userRole={userRole} />;
      default:
        return <Dashboard userRole={userRole} />;
    }
  };

  return (
    <div className="user-container">
      <Navigation 
        currentView={currentView}
        onViewChange={setCurrentView}
        userRole={userRole}
        onLogout={onLogout}
        isOpen={menuOpen}
        onToggle={() => setMenuOpen(!menuOpen)}
      />
      <Header 
        userData={userData} 
        onLogout={onLogout}
        onMenuToggle={() => setMenuOpen(!menuOpen)}
      />
      <main className="content">
        {renderView()}
      </main>
    </div>
  );
}