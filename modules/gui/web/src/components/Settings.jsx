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

  // Forecast module states
  const [forecastConfig, setForecastConfig] = useState({
    modelParameters: {},
    processParameters: {},
    preprocessingParameters: {},
    weatherParameters: {}
  });
  const [modelInfo, setModelInfo] = useState(null);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);

  // Pobierz preferencje przy ładowaniu komponentu
  useEffect(() => {
    fetchSettings();
    fetchForecastConfig();
    fetchModelInfo();
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

  const fetchForecastConfig = async () => {
    try {
      const response = await fetch(`${API_URL}/forecasts/config`);
      if (response.ok) {
        const data = await response.json();
        // Remove success/message wrapper if present, or just use data directly if it matches structure
        // Controller returns: { modelParameters: {...}, processParameters: {...}, ... } directly inside response? 
        // Checking controller: yes, direct keys in root object.
        setForecastConfig({
          modelParameters: data.modelParameters || {},
          processParameters: data.processParameters || {},
          preprocessingParameters: data.preprocessingParameters || {},
          weatherParameters: data.weatherParameters || {}
        });
      }
    } catch (error) {
      console.error('Błąd pobierania konfiguracji prognoz:', error);
    }
  };

  const fetchModelInfo = async () => {
    try {
      const response = await fetch(`${API_URL}/forecasts/model`);
      if (response.ok) {
        const data = await response.json();
        setModelInfo(data);
      }
    } catch (error) {
      console.error('Błąd pobierania info o modelu:', error);
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

      // Save forecast config
      const forecastResponse = await fetch(`${API_URL}/forecasts/config`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(forecastConfig)
      });

      if (response.ok && forecastResponse.ok) {
        setSuccess(true);
        setTimeout(() => setSuccess(false), 3000);
      } else {
        let errorMsg = 'Nie udało się zapisać ustawień';
        if (!response.ok) {
          const errorData = await response.json().catch(() => ({}));
          errorMsg = errorData.error || errorMsg;
        } else if (!forecastResponse.ok) {
          errorMsg = 'Nie udało się zapisać konfiguracji prognoz';
        }
        setError(errorMsg);
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

        <h3>Konfiguracja Modułu Prognoz (AI)</h3>

        {/* Model Parameters */}
        <div className="setting-group">
          <label>Minimalna dokładność modelu:</label>
          <input
            type="number"
            step="0.01"
            value={forecastConfig.modelParameters?.minAccuracyThreshold || ''}
            onChange={(e) => setForecastConfig({
              ...forecastConfig,
              modelParameters: { ...forecastConfig.modelParameters, minAccuracyThreshold: parseFloat(e.target.value) }
            })}
          />
        </div>

        <div className="setting-group">
          <label>Podział trening/walidacja (0-1):</label>
          <input
            type="number"
            step="0.1"
            max="1"
            min="0"
            value={forecastConfig.modelParameters?.trainValidationSplit || ''}
            onChange={(e) => setForecastConfig({
              ...forecastConfig,
              modelParameters: { ...forecastConfig.modelParameters, trainValidationSplit: parseFloat(e.target.value) }
            })}
          />
        </div>

        {/* Process Parameters */}
        <div className="setting-group">
          <label>Horyzont czasowy prognozy (godziny):</label>
          <input
            type="number"
            value={forecastConfig.processParameters?.defaultForecastHorizon || ''}
            onChange={(e) => setForecastConfig({
              ...forecastConfig,
              processParameters: { ...forecastConfig.processParameters, defaultForecastHorizon: parseInt(e.target.value) }
            })}
          />
        </div>

        <div className="setting-group">
          <label>Automatyczny retrening:</label>
          <div className="checkbox-wrapper" style={{ marginTop: '10px' }}>
            <input
              type="checkbox"
              checked={forecastConfig.processParameters?.enableAutoRetraining || false}
              onChange={(e) => setForecastConfig({
                ...forecastConfig,
                processParameters: { ...forecastConfig.processParameters, enableAutoRetraining: e.target.checked }
              })}
              style={{ width: 'auto', marginRight: '10px' }}
            />
            <span>Włączony</span>
          </div>
        </div>

        <div className="setting-group">
          <label>Interwał retreningu (godziny):</label>
          <input
            type="number"
            value={forecastConfig.processParameters?.retrainingIntervalHours || ''}
            onChange={(e) => setForecastConfig({
              ...forecastConfig,
              processParameters: { ...forecastConfig.processParameters, retrainingIntervalHours: parseInt(e.target.value) }
            })}
          />
        </div>

        {/* Info o Modelu */}
        {modelInfo && (
          <div className="model-info-panel" style={{
            marginTop: '30px',
            padding: '15px',
            border: '1px solid #ddd',
            borderRadius: '8px',
            backgroundColor: '#f9f9f9'
          }}>
            <h4 style={{ marginTop: 0 }}>Status Modelu AI</h4>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
              <div>
                <strong>Wytrenowany:</strong> {modelInfo.trained ? 'TAK' : 'NIE'}
              </div>
              <div>
                <strong>Data treningu:</strong> {modelInfo.trainingTimestamp || '-'}
              </div>
              <div>
                <strong>Próbki treningowe:</strong> {modelInfo.trainingSamplesCount || 0}
              </div>
              {modelInfo.metrics && (
                <>
                  <div>
                    <strong>Błąd MAPE:</strong> {modelInfo.metrics.mape ? modelInfo.metrics.mape.toFixed(2) + '%' : '-'}
                  </div>
                  <div>
                    <strong>RMSE:</strong> {modelInfo.metrics.rmse ? modelInfo.metrics.rmse.toFixed(2) : '-'}
                  </div>
                </>
              )}
            </div>
          </div>
        )}

        <br />

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

