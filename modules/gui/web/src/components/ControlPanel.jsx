import { useState, useEffect } from 'react';
import './ControlPanel.css';
import { handleApiError } from '../utils/api';
import { showToast } from './ToastContainer';

const API_URL = 'http://localhost:8080/api';

export default function ControlPanel() {
  const [devices, setDevices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [ozeStatus, setOzeStatus] = useState({ production: 0, grid: 100 });

  useEffect(() => {
    loadDevices();
    loadOZEStatus();
  }, []);

  const loadDevices = async () => {
    try {
      // TODO: Implementować endpoint API
      // Na razie mock data
      setDevices([
        { id: 1, name: 'Klimatyzator - Pokój 101', type: 'hvac', value: 22, currentTemp: 19.5, min: 16, max: 30, enabled: true },
        { id: 2, name: 'Oświetlenie - Pokój 102', type: 'light', value: 50, enabled: true },
        { id: 3, name: 'Wentylacja - Serwerownia', type: 'ventilation', value: 2, max: 5, enabled: true }
      ]);
      setLoading(false);
    } catch (error) {
      console.error('Błąd ładowania urządzeń:', error);
      handleApiError(error, showToast);
      setLoading(false);
    }
  };

  const loadOZEStatus = async () => {
    try {
      // TODO: Implementować endpoint API
      setOzeStatus({ production: 350, grid: 1200 });
    } catch (error) {
      console.error('Błąd ładowania statusu OZE:', error);
      handleApiError(error, showToast);
    }
  };

  const handleDeviceControl = async (deviceId, newValue) => {
    try {
      // TODO: Implementować endpoint API do sterowania
      console.log(`Sterowanie urządzeniem ${deviceId}: ${newValue}`);
      
      // Aktualizacja lokalnego stanu
      setDevices(devices.map(device => 
        device.id === deviceId ? { ...device, value: newValue } : device
      ));
    } catch (error) {
      console.error('Błąd sterowania urządzeniem:', error);
      handleApiError(error, showToast);
    }
  };

  const handleToggle = async (deviceId) => {
    try {
      const device = devices.find(d => d.id === deviceId);
      const newEnabled = !device.enabled;
      
      // TODO: Implementować endpoint API
      setDevices(devices.map(device => 
        device.id === deviceId ? { ...device, enabled: newEnabled } : device
      ));
    } catch (error) {
      console.error('Błąd przełączania urządzenia:', error);
    }
  };

  if (loading) {
    return <div className="control-panel">Ładowanie...</div>;
  }

  return (
    <div className="control-panel">
      <h2>Panel Sterowania</h2>

      {/* Status OZE */}
      <div className="oze-status">
        <h3>
          <span className="oze-icon">☀️</span> Status OZE
          {ozeStatus.production > 0 && (
            <span className="oze-active-badge">Aktywne</span>
          )}
        </h3>
        <div className="oze-info">
          <div className="oze-item">
            <span>Produkcja OZE:</span>
            <strong>{ozeStatus.production} W</strong>
          </div>
          <div className="oze-item">
            <span>Pobór z sieci:</span>
            <strong>{ozeStatus.grid} W</strong>
          </div>
          <div className="oze-chart">
            <div className="oze-bar">
              <div 
                className="oze-production" 
                style={{ width: `${(ozeStatus.production / (ozeStatus.production + ozeStatus.grid)) * 100}%` }}
              >
                OZE: {ozeStatus.production}W
              </div>
              <div 
                className="oze-grid" 
                style={{ width: `${(ozeStatus.grid / (ozeStatus.production + ozeStatus.grid)) * 100}%` }}
              >
                Sieć: {ozeStatus.grid}W
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Urządzenia */}
      <div className="devices-section">
        <h3>Urządzenia</h3>
        <div className="devices-grid">
          {devices.map(device => (
            <div key={device.id} className="device-card">
              <div className="device-header">
                <h4>{device.name}</h4>
                <label className="toggle-switch">
                  <input
                    type="checkbox"
                    checked={device.enabled}
                    onChange={() => handleToggle(device.id)}
                  />
                  <span className="slider"></span>
                </label>
              </div>

              {device.type === 'hvac' && (
                <div className="device-control">
                  <div className="temp-info">
                    <label>
                      Temperatura docelowa: <strong>{device.value}°C</strong>
                    </label>
                    {device.currentTemp !== undefined && (
                      <label className="current-temp">
                        Aktualna temperatura: <strong>{device.currentTemp}°C</strong>
                      </label>
                    )}
                  </div>
                  <input
                    type="range"
                    min={device.min || 16}
                    max={device.max || 30}
                    value={device.value}
                    onChange={(e) => handleDeviceControl(device.id, parseInt(e.target.value))}
                    disabled={!device.enabled}
                  />
                  <div className="range-labels">
                    <span>{device.min || 16}°C</span>
                    <span>{device.max || 30}°C</span>
                  </div>
                </div>
              )}

              {device.type === 'light' && (
                <div className="device-control">
                  <label>
                    Jasność: <strong>{device.value}%</strong>
                  </label>
                  <input
                    type="range"
                    min="0"
                    max="100"
                    value={device.value}
                    onChange={(e) => handleDeviceControl(device.id, parseInt(e.target.value))}
                    disabled={!device.enabled}
                  />
                </div>
              )}

              {device.type === 'ventilation' && (
                <div className="device-control">
                  <label>
                    Poziom nawiewu: <strong>{device.value}/{device.max}</strong>
                  </label>
                  <div className="ventilation-buttons">
                    <button 
                      className={device.value === 0 ? 'active' : ''}
                      onClick={() => handleDeviceControl(device.id, 0)}
                      disabled={!device.enabled}
                    >
                      Niski
                    </button>
                    <button 
                      className={device.value === Math.floor((device.max || 5) / 2) ? 'active' : ''}
                      onClick={() => handleDeviceControl(device.id, Math.floor((device.max || 5) / 2))}
                      disabled={!device.enabled}
                    >
                      Średni
                    </button>
                    <button 
                      className={device.value === (device.max || 5) ? 'active' : ''}
                      onClick={() => handleDeviceControl(device.id, device.max || 5)}
                      disabled={!device.enabled}
                    >
                      Wysoki
                    </button>
                  </div>
                  <input
                    type="range"
                    min="0"
                    max={device.max || 5}
                    value={device.value}
                    onChange={(e) => handleDeviceControl(device.id, parseInt(e.target.value))}
                    disabled={!device.enabled}
                  />
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

