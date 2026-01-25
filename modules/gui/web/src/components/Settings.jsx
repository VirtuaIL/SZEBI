import { useState, useEffect } from 'react';
import './Settings.css';
import { getApiBaseUrl } from '../utils/api';

const API_URL = getApiBaseUrl();

export default function Settings({ userRole }) {
  const [settings, setSettings] = useState({
    preferredMinTemp: 18,
    preferredMaxTemp: 24,
    maxEnergyUsage: 1500,
    timeOpen: '08:00',
    timeClose: '20:00',
    priorityComfort: 7
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);

  // Pobierz preferencje przy ładowaniu komponentu
  useEffect(() => {
    fetchSettings();
  }, []);

  const fetchSettings = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetch(`${API_URL}/optimization/config`);
      if (response.ok) {
        const data = await response.json();
        setSettings({
          preferredMinTemp: data.preferredMinTemp || 18,
          preferredMaxTemp: data.preferredMaxTemp || 24,
          maxEnergyUsage: data.maxEnergyUsage || 1500,
          timeOpen: data.timeOpen || '08:00',
          timeClose: data.timeClose || '20:00',
          priorityComfort: data.priorityComfort || 7
        });
      } else {
        const errorData = await response.json().catch(() => ({}));
        setError(`Błąd pobierania ustawień: ${errorData.error || response.status}`);
      }
    } catch (error) {
      console.error('Błąd połączenia:', error);
      setError('Błąd połączenia z serwerem. Upewnij się, że backend jest uruchomiony.');
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    setLoading(true);
    setError(null);
    setSuccess(false);
    
    try {
      const response = await fetch(`${API_URL}/optimization/config`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(settings)
      });

      if (response.ok) {
        setSuccess(true);
        setTimeout(() => setSuccess(false), 3000); // Ukryj komunikat sukcesu po 3 sekundach
      } else {
        const errorData = await response.json().catch(() => ({}));
        setError(errorData.error || 'Nie udało się zapisać ustawień');
      }
    } catch (error) {
      console.error('Błąd zapisywania ustawień:', error);
      setError('Wystąpił błąd połączenia z serwerem.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="settings">
      <h2>Ustawienia Globalne</h2>

      {error && (
        <div className="error-message" style={{ 
          background: '#f8d7da', 
          color: '#721c24', 
          padding: '10px', 
          borderRadius: '4px', 
          marginBottom: '20px' 
        }}>
          {error}
        </div>
      )}

      {success && (
        <div className="success-message" style={{ 
          background: '#d4edda', 
          color: '#155724', 
          padding: '10px', 
          borderRadius: '4px', 
          marginBottom: '20px' 
        }}>
          Ustawienia zostały pomyślnie zapisane!
        </div>
      )}

      {loading && !settings.preferredMinTemp && (
        <p style={{ color: '#666', fontStyle: 'italic' }}>Ładowanie ustawień...</p>
      )}

      <div className="settings-form">
        <div className="setting-group">
          <label>Temperatura komfortowa - minimum:</label>
          <input
            type="number"
            value={settings.preferredMinTemp}
            onChange={(e) => setSettings({ ...settings, preferredMinTemp: parseFloat(e.target.value) })}
            min="16"
            max="25"
          />
          <span>°C</span>
        </div>

        <div className="setting-group">
          <label>Temperatura komfortowa - maximum:</label>
          <input
            type="number"
            value={settings.preferredMaxTemp}
            onChange={(e) => setSettings({ ...settings, preferredMaxTemp: parseFloat(e.target.value) })}
            min="18"
            max="30"
          />
          <span>°C</span>
        </div>

        <div className="setting-group">
          <label>Maksymalne zużycie energii:</label>
          <input
            type="number"
            value={settings.maxEnergyUsage}
            onChange={(e) => setSettings({ ...settings, maxEnergyUsage: parseFloat(e.target.value) })}
            min="0"
          />
          <span>W</span>
        </div>

        <div className="setting-group">
          <label>Godzina otwarcia:</label>
          <input
            type="time"
            value={settings.timeOpen}
            onChange={(e) => setSettings({ ...settings, timeOpen: e.target.value })}
          />
        </div>

        <div className="setting-group">
          <label>Godzina zamknięcia:</label>
          <input
            type="time"
            value={settings.timeClose}
            onChange={(e) => setSettings({ ...settings, timeClose: e.target.value })}
          />
        </div>

        <div className="setting-group">
          <label>Priorytet komfortu (1-10):</label>
          <div className="range-input-group">
            <input
              type="range"
              min="1"
              max="10"
              value={settings.priorityComfort}
              onChange={(e) => setSettings({ ...settings, priorityComfort: parseInt(e.target.value) })}
            />
            <span>{settings.priorityComfort}/10</span>
            <span className="range-hint">
              {settings.priorityComfort <= 3 ? 'Oszczędność' : 
               settings.priorityComfort <= 7 ? 'Równowaga' : 'Komfort'}
            </span>
          </div>
        </div>

        <button 
          className="btn-save" 
          onClick={handleSave}
          disabled={loading}
        >
          {loading ? 'Zapisywanie...' : 'Zapisz Ustawienia'}
        </button>
      </div>
    </div>
  );
}

