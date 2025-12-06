import { useState } from 'react';
import './Settings.css';

export default function Settings({ userRole }) {
  const [settings, setSettings] = useState({
    preferredMinTemp: 18,
    preferredMaxTemp: 24,
    maxEnergyUsage: 1500,
    timeOpen: '08:00',
    timeClose: '20:00',
    priorityComfort: 7
  });

  const handleSave = async () => {
    try {
      // TODO: Implementować endpoint API
      console.log('Zapisywanie ustawień:', settings);
      alert('Ustawienia zostały zapisane (funkcja do implementacji)');
    } catch (error) {
      console.error('Błąd zapisywania ustawień:', error);
      alert('Błąd podczas zapisywania ustawień');
    }
  };

  return (
    <div className="settings">
      <h2>Ustawienia Globalne</h2>

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

        <button className="btn-save" onClick={handleSave}>
          Zapisz Ustawienia
        </button>
      </div>
    </div>
  );
}

