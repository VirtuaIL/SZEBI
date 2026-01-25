import { useState, useEffect } from 'react';
import './ControlPanel.css';
import { apiRequest, handleApiError, getApiBaseUrl } from '../utils/api';
import { showToast } from './ToastContainer';

const API_URL = getApiBaseUrl();

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
      setLoading(true);
      // Pobierz listę urządzeń z API
      const devicesData = await apiRequest('/devices');

      // Mapuj dane z API do formatu używanego w komponencie
      const mappedDevices = devicesData.map(device => {
        // Określ typ urządzenia na podstawie metryki
        let type = 'other';
        let value = 0;
        let min = 16;
        let max = 30;
        let currentTemp = null;

        // Parsuj parametry pracy (JSON)
        let params = {};
        try {
          if (device.parametryPracy) {
            params = typeof device.parametryPracy === 'string'
              ? JSON.parse(device.parametryPracy)
              : device.parametryPracy;
          }
        } catch (e) {
          console.warn('Błąd parsowania parametrów pracy:', e);
        }

        // Określ typ i wartość na podstawie metryki i parametrów
        const metric = device.metryka || device.metric || '';
        if (metric.toLowerCase().includes('temperatura') || metric.toLowerCase().includes('temp')) {
          type = 'hvac';
          value = params.temperatura_C || params.set_temp || params.wartosc || 22;
          min = device.minRange || 16;
          max = device.maxRange || 30;
          currentTemp = device.currentValue || null;
        } else if (metric.toLowerCase().includes('jasnosc') || metric.toLowerCase().includes('brightness') || metric.toLowerCase().includes('light')) {
          type = 'light';
          value = params.jasnosc_procent || params.wartosc || 50;
          min = 0;
          max = 100;
        } else if (metric.toLowerCase().includes('ventilation') || metric.toLowerCase().includes('wentylacja')) {
          type = 'ventilation';
          value = params.ventilation_level || params.wartosc || 2;
          min = 0;
          max = device.maxRange || 5;
        } else {
          type = 'other';
          value = params.wartosc || params.value || 0;
        }

        return {
          id: device.id,
          name: device.name || `${device.producer || ''} ${device.model || ''}`.trim() || `Urządzenie ${device.id}`,
          type: type,
          value: value,
          currentTemp: currentTemp,
          min: min,
          max: max,
          enabled: device.aktywny !== false,
          location: device.location || 'Nieznana',
          power: device.powerW || 0
        };
      });

      setDevices(mappedDevices);
      setLoading(false);
    } catch (error) {
      console.error('Błąd ładowania urządzeń:', error);
      handleApiError(error, showToast);
      setLoading(false);
      setDevices([]); // Fallback do pustej listy
    }
  };

  const loadOZEStatus = async () => {
    try {
      // Użyj endpointu z DashboardController
      const ozeData = await apiRequest('/dashboard/oze-status?buildingId=1');
      setOzeStatus({
        production: ozeData.production || 0,
        grid: ozeData.grid || 0
      });
    } catch (error) {
      console.error('Błąd ładowania statusu OZE:', error);
      handleApiError(error, showToast);
      setOzeStatus({ production: 0, grid: 0 }); // Fallback
    }
  };

  const handleDeviceControl = async (deviceId, newValue) => {
    try {
      const device = devices.find(d => d.id === deviceId);
      if (!device) return;

      // Przygotuj dane do wysłania w zależności od typu urządzenia
      let controlData = { value: newValue };

      if (device.type === 'hvac') {
        controlData = { temperature: newValue, value: newValue };
      } else if (device.type === 'light') {
        controlData = { brightness: newValue, value: newValue };
      } else if (device.type === 'ventilation') {
        controlData = { ventilation: newValue, value: newValue };
      }

      // Wyślij żądanie sterowania
      await apiRequest(`/devices/${deviceId}/control`, {
        method: 'POST',
        body: JSON.stringify(controlData)
      });

      // Aktualizacja lokalnego stanu
      setDevices(devices.map(device =>
        device.id === deviceId ? { ...device, value: newValue } : device
      ));

      showToast('Urządzenie zostało zaktualizowane', 'success');
    } catch (error) {
      console.error('Błąd sterowania urządzeniem:', error);
      handleApiError(error, showToast);
    }
  };

  const handleToggle = async (deviceId) => {
    try {
      const device = devices.find(d => d.id === deviceId);
      if (!device) return;

      const newEnabled = !device.enabled;

      // Wyślij żądanie sterowania z parametrem enabled
      await apiRequest(`/devices/${deviceId}/control`, {
        method: 'POST',
        body: JSON.stringify({ enabled: newEnabled, active: newEnabled })
      });

      // Aktualizacja lokalnego stanu
      setDevices(devices.map(device =>
        device.id === deviceId ? { ...device, enabled: newEnabled } : device
      ));

      showToast(`Urządzenie ${newEnabled ? 'włączone' : 'wyłączone'}`, 'success');
    } catch (error) {
      console.error('Błąd przełączania urządzenia:', error);
      handleApiError(error, showToast);
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
                <div className="device-title">
                  <h4>{device.name}</h4>
                  <span className="device-details-text">
                    {device.location} • {device.power} W
                  </span>
                </div>
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

