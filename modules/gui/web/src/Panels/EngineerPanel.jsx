import { useState } from 'react';
import './EngineerPanel.css';
import Navigation from '../components/Navigation';
import Header from '../components/Header';
import ControlPanel from '../components/ControlPanel';
import AlertsCenter from '../components/AlertsCenter';
import Monitoring from '../components/Monitoring';
import Dashboard from '../components/Dashboard';
import Reports from '../components/Reports';

export default function EngineerPanel({ onLogout }) {
  const [currentView, setCurrentView] = useState('alerts'); // Domyślnie pokazuj alarmy
  const userRole = 'engineer';
  
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
      default:
        return <AlertsCenter userRole={userRole} />;
    }
  };

  return (
    <div className="engineer-container">
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