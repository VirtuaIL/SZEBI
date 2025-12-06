import { useState, useEffect, useRef } from 'react';
import './AlertsCenter.css';
import { showToast } from './ToastContainer';
import { handleApiError, getApiBaseUrl } from '../utils/api';

const API_URL = getApiBaseUrl();

export default function AlertsCenter({ userRole }) {
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [sortField, setSortField] = useState('timestamp');
  const [sortDirection, setSortDirection] = useState('desc');
  const previousAlertsRef = useRef([]);

  useEffect(() => {
    loadAlerts();
    // Odświeżaj co 30 sekund (zgodnie z wymaganiami)
    const interval = setInterval(loadAlerts, 30000);
    return () => clearInterval(interval);
  }, []);

  const loadAlerts = async () => {
    try {
      // TODO: Implementować endpoint API
      // Na razie mock data
      const newAlerts = [
        {
          id: 1,
          device: 'Czujnik temperatury - Pokój 101',
          message: 'Temperatura przekroczyła próg alarmowy!',
          priority: 'CRITICAL',
          status: 'NOWY',
          location: 'Pokój 101, Piętro 1',
          timestamp: new Date().toISOString()
        },
        {
          id: 2,
          device: 'Oświetlenie - Pokój 102',
          message: 'Wysoki poziom wilgotności',
          priority: 'WARNING',
          status: 'NOWY',
          location: 'Pokój 102, Piętro 1',
          timestamp: new Date(Date.now() - 120000).toISOString()
        },
        {
          id: 3,
          device: 'Klimatyzator - Serwerownia',
          message: 'Utracono i odzyskano komunikację',
          priority: 'INFO',
          status: 'ROZWIAZANY',
          location: 'Serwerownia, Piętro 0',
          timestamp: new Date(Date.now() - 3600000).toISOString()
        }
      ];

      // Wykryj nowe alarmy i pokaż toast
      if (previousAlertsRef.current.length > 0) {
        const previousIds = new Set(previousAlertsRef.current.map(a => a.id));
        const newAlertsOnly = newAlerts.filter(a => !previousIds.has(a.id) && a.status === 'NOWY');
        
        newAlertsOnly.forEach(alert => {
          const toastType = alert.priority === 'CRITICAL' ? 'error' : 
                           alert.priority === 'WARNING' ? 'warning' : 'info';
          showToast(`🚨 ${alert.device}: ${alert.message}`, toastType, 5000);
        });
      }

      previousAlertsRef.current = newAlerts;
      setAlerts(newAlerts);
      setLoading(false);
    } catch (error) {
      console.error('Błąd ładowania alarmów:', error);
      handleApiError(error, showToast);
      setLoading(false);
    }
  };

  const handleAcknowledge = async (alertId) => {
    try {
      // TODO: Implementować endpoint API
      setAlerts(alerts.map(alert =>
        alert.id === alertId ? { ...alert, status: 'W TRAKCIE' } : alert
      ));
      showToast('Alarm został potwierdzony', 'success', 3000);
    } catch (error) {
      console.error('Błąd potwierdzania alarmu:', error);
      handleApiError(error, showToast);
    }
  };

  const handleResolve = async (alertId) => {
    try {
      // TODO: Implementować endpoint API
      setAlerts(alerts.map(alert =>
        alert.id === alertId ? { ...alert, status: 'ROZWIAZANY' } : alert
      ));
      showToast('Alarm został rozwiązany', 'success', 3000);
    } catch (error) {
      console.error('Błąd rozwiązywania alarmu:', error);
      handleApiError(error, showToast);
    }
  };

  const getPriorityClass = (priority) => {
    switch (priority) {
      case 'CRITICAL': return 'priority-critical';
      case 'WARNING': return 'priority-warning';
      case 'INFO': return 'priority-info';
      default: return '';
    }
  };

  const getStatusClass = (status) => {
    switch (status) {
      case 'NOWY': return 'status-new';
      case 'POTWIERDZONY':
      case 'W TRAKCIE': return 'status-acknowledged';
      case 'ROZWIAZANY': return 'status-resolved';
      default: return '';
    }
  };

  const handleSort = (field) => {
    if (sortField === field) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortDirection('asc');
    }
  };

  const sortedAlerts = [...alerts].sort((a, b) => {
    let aVal, bVal;
    switch (sortField) {
      case 'timestamp':
        aVal = new Date(a.timestamp);
        bVal = new Date(b.timestamp);
        break;
      case 'device':
        aVal = a.device.toLowerCase();
        bVal = b.device.toLowerCase();
        break;
      case 'priority':
        const priorityOrder = { 'CRITICAL': 3, 'WARNING': 2, 'INFO': 1 };
        aVal = priorityOrder[a.priority] || 0;
        bVal = priorityOrder[b.priority] || 0;
        break;
      default:
        return 0;
    }
    
    if (aVal < bVal) return sortDirection === 'asc' ? -1 : 1;
    if (aVal > bVal) return sortDirection === 'asc' ? 1 : -1;
    return 0;
  });

  if (loading) {
    return <div className="alerts-center">Ładowanie alarmów...</div>;
  }

  const activeAlerts = sortedAlerts.filter(a => a.status !== 'ROZWIAZANY');
  const resolvedAlerts = sortedAlerts.filter(a => a.status === 'ROZWIAZANY');

  return (
    <div className="alerts-center">
      <h2>Centrum Powiadomień</h2>

      <div className="alerts-stats">
        <div className="stat-card critical">
          <span className="stat-number">{alerts.filter(a => a.priority === 'CRITICAL' && a.status === 'NOWY').length}</span>
          <span className="stat-label">Krytyczne</span>
        </div>
        <div className="stat-card warning">
          <span className="stat-number">{alerts.filter(a => a.priority === 'WARNING' && a.status === 'NOWY').length}</span>
          <span className="stat-label">Ostrzeżenia</span>
        </div>
        <div className="stat-card active">
          <span className="stat-number">{activeAlerts.length}</span>
          <span className="stat-label">Aktywne</span>
        </div>
      </div>

      <div className="alerts-section">
        <div className="alerts-section-header">
          <h3>Aktywne Alarmy</h3>
          <div className="sort-controls">
            <span>Sortuj według:</span>
            <button 
              className={sortField === 'timestamp' ? 'active' : ''}
              onClick={() => handleSort('timestamp')}
            >
              Data {sortField === 'timestamp' && (sortDirection === 'asc' ? '↑' : '↓')}
            </button>
            <button 
              className={sortField === 'device' ? 'active' : ''}
              onClick={() => handleSort('device')}
            >
              Urządzenie {sortField === 'device' && (sortDirection === 'asc' ? '↑' : '↓')}
            </button>
            <button 
              className={sortField === 'priority' ? 'active' : ''}
              onClick={() => handleSort('priority')}
            >
              Priorytet {sortField === 'priority' && (sortDirection === 'asc' ? '↑' : '↓')}
            </button>
          </div>
        </div>
        {activeAlerts.length === 0 ? (
          <p className="no-alerts">Brak aktywnych alarmów</p>
        ) : (
          <div className="alerts-list">
            {sortedAlerts.filter(a => a.status !== 'ROZWIAZANY').map(alert => (
              <div key={alert.id} className={`alert-card ${getPriorityClass(alert.priority)}`}>
                <div className="alert-header">
                  <div className="alert-info">
                    <h4>{alert.device}</h4>
                    <p className="alert-message">{alert.message}</p>
                    <div className="alert-meta">
                      <span className="alert-location">📍 {alert.location}</span>
                      <span className="alert-time">
                        {new Date(alert.timestamp).toLocaleString('pl-PL')}
                      </span>
                    </div>
                  </div>
                  <div className="alert-badges">
                    <span className={`priority-badge ${getPriorityClass(alert.priority)}`}>
                      {alert.priority}
                    </span>
                    <span className={`status-badge ${getStatusClass(alert.status)}`}>
                      {alert.status}
                    </span>
                  </div>
                </div>
                {userRole === 'engineer' || userRole === 'admin' ? (
                  <div className="alert-actions">
                    {alert.status === 'NOWY' && (
                      <button 
                        className="btn-acknowledge"
                        onClick={() => handleAcknowledge(alert.id)}
                      >
                        Potwierdź (W trakcie)
                      </button>
                    )}
                    {alert.status !== 'ROZWIAZANY' && (
                      <button 
                        className="btn-resolve"
                        onClick={() => handleResolve(alert.id)}
                      >
                        Rozwiąż
                      </button>
                    )}
                  </div>
                ) : null}
              </div>
            ))}
          </div>
        )}
      </div>

      {resolvedAlerts.length > 0 && (
        <div className="alerts-section">
          <h3>Rozwiązane Alarmy</h3>
          <div className="alerts-list">
            {resolvedAlerts.map(alert => (
              <div key={alert.id} className={`alert-card resolved ${getPriorityClass(alert.priority)}`}>
                <div className="alert-header">
                  <div className="alert-info">
                    <h4>{alert.device}</h4>
                    <p className="alert-message">{alert.message}</p>
                    <div className="alert-meta">
                      <span className="alert-location">📍 {alert.location}</span>
                      <span className="alert-time">
                        {new Date(alert.timestamp).toLocaleString('pl-PL')}
                      </span>
                    </div>
                  </div>
                  <span className={`status-badge ${getStatusClass(alert.status)}`}>
                    {alert.status}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

