import { useState, useEffect } from 'react';
import './DeviceManagement.css';

import { getApiBaseUrl } from '../utils/api';
const API_URL = getApiBaseUrl();

export default function DeviceManagement({ userRole }) {
  const [showAddForm, setShowAddForm] = useState(false);
  const [formData, setFormData] = useState({
    deviceId: '',
    producerName: '',
    modelName: '',
    minRange: '',
    maxRange: '',
    metricLabel: '',
    powerW: '',
    roomId: ''
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);

  // Konfiguracja globalna
  const [globalConfig, setGlobalConfig] = useState({
    preferredMinTemp: 18,
    preferredMaxTemp: 24,
    priorityComfort: 7
  });
  const [configLoading, setConfigLoading] = useState(false);

  // Pobierz konfigurację globalną przy ładowaniu
  useEffect(() => {
    fetchGlobalConfig();
  }, []);

  const fetchGlobalConfig = async () => {
    try {
      const response = await fetch(`${API_URL}/optimization/config`);
      if (response.ok) {
        const data = await response.json();
        setGlobalConfig({
          preferredMinTemp: data.preferredMinTemp || 18,
          preferredMaxTemp: data.preferredMaxTemp || 24,
          priorityComfort: data.priorityComfort || 7
        });
      }
    } catch (error) {
      console.error('Błąd pobierania konfiguracji:', error);
    }
  };

  const handleAddDevice = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setSuccess(false);

    try {
      // Mapuj pola z formularza na format API
      const payload = {
        deviceId: formData.deviceId || undefined,
        producerName: formData.producerName,
        modelName: formData.modelName,
        name: `${formData.producerName} ${formData.modelName}`.trim(),
        minRange: formData.minRange ? parseFloat(formData.minRange) : undefined,
        maxRange: formData.maxRange ? parseFloat(formData.maxRange) : undefined,
        metricLabel: formData.metricLabel,
        powerW: formData.powerW ? parseFloat(formData.powerW) : undefined,
        roomId: formData.roomId ? parseInt(formData.roomId) : undefined
      };

      const response = await fetch(`${API_URL}/devices`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload)
      });

      const data = await response.json();

      if (response.ok) {
        setSuccess(true);
        setTimeout(() => setSuccess(false), 3000);
        setShowAddForm(false);
        setFormData({ 
          deviceId: '', 
          producerName: '', 
          modelName: '', 
          minRange: '', 
          maxRange: '', 
          metricLabel: '', 
          powerW: '', 
          roomId: '' 
        });
      } else {
        setError(data.error || 'Nie udało się dodać urządzenia');
      }
    } catch (error) {
      console.error('Błąd dodawania urządzenia:', error);
      setError('Wystąpił błąd połączenia z serwerem.');
    } finally {
      setLoading(false);
    }
  };

  const handleSaveGlobalConfig = async () => {
    setConfigLoading(true);
    setError(null);
    setSuccess(false);

    try {
      const response = await fetch(`${API_URL}/optimization/config`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(globalConfig)
      });

      if (response.ok) {
        setSuccess(true);
        setTimeout(() => setSuccess(false), 3000);
      } else {
        const errorData = await response.json().catch(() => ({}));
        setError(errorData.error || 'Nie udało się zapisać konfiguracji');
      }
    } catch (error) {
      console.error('Błąd zapisywania konfiguracji:', error);
      setError('Wystąpił błąd połączenia z serwerem.');
    } finally {
      setConfigLoading(false);
    }
  };

  return (
    <div className="device-management">
      <div className="device-management-header">
        <h2>Zarządzanie Urządzeniami</h2>
        <button 
          className="btn-add-device"
          onClick={() => setShowAddForm(!showAddForm)}
        >
          + Dodaj Nowe Urządzenie
        </button>
      </div>

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
          Operacja zakończona sukcesem!
        </div>
      )}

      {showAddForm && (
        <div className="add-device-form">
          <h3>Dodaj Nowe Urządzenie</h3>
          <form onSubmit={handleAddDevice}>
            <div className="form-group">
              <label>ID Urządzenia:</label>
              <input
                type="text"
                value={formData.deviceId}
                onChange={(e) => setFormData({ ...formData, deviceId: e.target.value })}
                placeholder="Nadawane automatycznie lub wpisz ręcznie"
              />
            </div>
            <div className="form-group">
              <label>Nazwa Producenta:</label>
              <input
                type="text"
                value={formData.producerName}
                onChange={(e) => setFormData({ ...formData, producerName: e.target.value })}
                placeholder="np. Siemens"
                required
              />
            </div>
            <div className="form-group">
              <label>Nazwa Modelu:</label>
              <input
                type="text"
                value={formData.modelName}
                onChange={(e) => setFormData({ ...formData, modelName: e.target.value })}
                placeholder="np. Climatix T1"
                required
              />
            </div>
            <div className="form-row">
              <div className="form-group">
                <label>Zakres Min:</label>
                <input
                  type="number"
                  step="0.1"
                  value={formData.minRange}
                  onChange={(e) => setFormData({ ...formData, minRange: e.target.value })}
                  placeholder="np. -20.0"
                />
              </div>
              <div className="form-group">
                <label>Zakres Max:</label>
                <input
                  type="number"
                  step="0.1"
                  value={formData.maxRange}
                  onChange={(e) => setFormData({ ...formData, maxRange: e.target.value })}
                  placeholder="np. 50.0"
                />
              </div>
            </div>
            <div className="form-group">
              <label>Jednostka (Etykieta Metryki):</label>
              <input
                type="text"
                value={formData.metricLabel}
                onChange={(e) => setFormData({ ...formData, metricLabel: e.target.value })}
                placeholder="np. temperatura_C, jasnosc_procent"
                required
              />
            </div>
            <div className="form-group">
              <label>Moc (W):</label>
              <input
                type="number"
                step="0.1"
                value={formData.powerW}
                onChange={(e) => setFormData({ ...formData, powerW: e.target.value })}
                placeholder="np. 9.0"
              />
            </div>
            <div className="form-group">
              <label>ID Pokoju:</label>
              <input
                type="number"
                value={formData.roomId}
                onChange={(e) => setFormData({ ...formData, roomId: e.target.value })}
                required
              />
            </div>
            <div className="form-actions">
              <button 
                type="submit" 
                className="btn-submit"
                disabled={loading}
              >
                {loading ? 'Dodawanie...' : 'Dodaj Urządzenie'}
              </button>
              <button 
                type="button" 
                className="btn-cancel"
                onClick={() => {
                  setShowAddForm(false);
                  setError(null);
                }}
                disabled={loading}
              >
                Anuluj
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="device-config">
        <h3>Konfiguracja Globalna</h3>
        <div className="config-form">
          <div className="form-group">
            <label>Temperatura komfortowa (min):</label>
            <input 
              type="number" 
              value={globalConfig.preferredMinTemp}
              onChange={(e) => setGlobalConfig({ ...globalConfig, preferredMinTemp: parseFloat(e.target.value) })}
              min="16" 
              max="25" 
            />
            <span>°C</span>
          </div>
          <div className="form-group">
            <label>Temperatura komfortowa (max):</label>
            <input 
              type="number" 
              value={globalConfig.preferredMaxTemp}
              onChange={(e) => setGlobalConfig({ ...globalConfig, preferredMaxTemp: parseFloat(e.target.value) })}
              min="18" 
              max="30" 
            />
            <span>°C</span>
          </div>
          <div className="form-group">
            <label>Priorytet (Oszczędność vs Komfort):</label>
            <div className="range-input-group">
              <input 
                type="range" 
                min="1" 
                max="10" 
                value={globalConfig.priorityComfort}
                onChange={(e) => setGlobalConfig({ ...globalConfig, priorityComfort: parseInt(e.target.value) })}
              />
              <span>{globalConfig.priorityComfort}/10</span>
              <span className="range-hint">
                {globalConfig.priorityComfort <= 3 ? 'Oszczędność' : 
                 globalConfig.priorityComfort <= 7 ? 'Równowaga' : 'Komfort'}
              </span>
            </div>
          </div>
          <button 
            className="btn-save-config"
            onClick={handleSaveGlobalConfig}
            disabled={configLoading}
          >
            {configLoading ? 'Zapisywanie...' : 'Zapisz Konfigurację'}
          </button>
        </div>
      </div>
    </div>
  );
}

