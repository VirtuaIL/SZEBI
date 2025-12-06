import { useState } from 'react';
import './AdminPanel.css';
import Navigation from '../components/Navigation';
import Header from '../components/Header';
import ControlPanel from '../components/ControlPanel';
import AlertsCenter from '../components/AlertsCenter';
import Monitoring from '../components/Monitoring';
import Dashboard from '../components/Dashboard';
import Reports from '../components/Reports';
import DeviceManagement from '../components/DeviceManagement';
import UserManagement from '../components/UserManagement';
import Settings from '../components/Settings';

export default function AdminPanel({ onLogout }) {
  const [currentView, setCurrentView] = useState('dashboard');
  const userRole = 'admin';
  
  // Pobierz dane użytkownika z localStorage
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
      case 'monitoring':
        return <Monitoring userRole={userRole} />;
      case 'alerts':
        return <AlertsCenter userRole={userRole} />;
      case 'reports':
        return <Reports userRole={userRole} />;
      case 'devices':
        return <DeviceManagement userRole={userRole} />;
      case 'users':
        return <UserManagement userRole={userRole} />;
      case 'settings':
        return <Settings userRole={userRole} />;
      default:
        return <Dashboard userRole={userRole} />;
    }
  };

  return (
    <div className="admin-container">
      <Navigation 
        currentView={currentView}
        onViewChange={setCurrentView}
        userRole={userRole}
        onLogout={onLogout}
      />
      <Header userData={userData} onLogout={onLogout} />
      <main className="content">
        {renderView()}
      </main>
    </div>
  );
}