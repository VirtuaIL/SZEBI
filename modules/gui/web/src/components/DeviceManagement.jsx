import { useState, useEffect } from 'react';
import './DeviceManagement.css';

import { getApiBaseUrl } from '../utils/api';
const API_URL = getApiBaseUrl();

export default function DeviceManagement({ userRole }) {
  const [showAddForm, setShowAddForm] = useState(false);
  const [models, setModels] = useState([]);
  const [devices, setDevices] = useState([]); // Nowy stan dla urządzeń
  const [formData, setFormData] = useState({
    modelId: '',
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

  // Pobierz konfigurację, modele i urządzenia przy ładowaniu
  useEffect(() => {
    fetchGlobalConfig();
    fetchModels();
    fetchDevices();
  }, []);

  const fetchDevices = async () => {
    try {
      const response = await fetch(`${API_URL}/devices`);
      if (response.ok) {
        const data = await response.json();
        setDevices(Array.isArray(data) ? data : []);
      }
    } catch (error) {
      console.error('Błąd pobierania urządzeń:', error);
    }
  };

  const fetchModels = async () => {
    try {
      const response = await fetch(`${API_URL}/models`);
      if (response.ok) {
        const data = await response.json();
        setModels(Array.isArray(data) ? data : []);
      }
    } catch (error) {
      console.error('Błąd pobierania modeli:', error);
    }
  };

  // ... rest of functions ...

  // Update render to include table
  // I will invoke a larger replace to cover the render part too, or do it in two chunks?
  // Since I can't put two chunks in one `replace_file_content` unless using `multi_replace`.
  // I'll use multi_replace.

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
      // Znajdź wybrany model
      const selectedModel = models.find(m => m.id === parseInt(formData.modelId));

      // Mapuj pola z formularza na format API
      const payload = {
        modelId: parseInt(formData.modelId),
        name: selectedModel ? `${selectedModel.nazwaProducenta} ${selectedModel.nazwaModelu}` : undefined,
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
          modelId: '',
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
              <label>Model Urządzenia:</label>
              <select
                value={formData.modelId}
                onChange={(e) => setFormData({ ...formData, modelId: e.target.value })}
                required
                style={{
                  width: '100%',
                  padding: '8px',
                  borderRadius: '4px',
                  border: '1px solid #ddd'
                }}
              >
                <option value="">Wybierz model...</option>
                {models.map(model => (
                  <option key={model.id} value={model.id}>
                    {model.nazwaProducenta} {model.nazwaModelu} ({model.nazwaTypu})
                  </option>
                ))}
              </select>
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

      <div className="device-list" style={{ marginBottom: '30px' }}>
        <h3>Lista Urządzeń</h3>
        {devices.length === 0 ? (
          <p>Brak urządzeń w systemie.</p>
        ) : (
          <table className="devices-table" style={{ width: '100%', borderCollapse: 'collapse', marginTop: '10px' }}>
            <thead>
              <tr style={{ background: '#f5f5f5', textAlign: 'left' }}>
                <th style={{ padding: '10px', border: '1px solid #ddd' }}>ID</th>
                <th style={{ padding: '10px', border: '1px solid #ddd' }}>Nazwa</th>
                <th style={{ padding: '10px', border: '1px solid #ddd' }}>Typ</th>
                <th style={{ padding: '10px', border: '1px solid #ddd' }}>Pokój</th>
                <th style={{ padding: '10px', border: '1px solid #ddd' }}>Status</th>
              </tr>
            </thead>
            <tbody>
              {devices.map(device => (
                <tr key={device.id} style={{ borderBottom: '1px solid #ddd' }}>
                  <td style={{ padding: '10px', border: '1px solid #ddd' }}>{device.id}</td>
                  <td style={{ padding: '10px', border: '1px solid #ddd' }}>{device.name}</td>
                  <td style={{ padding: '10px', border: '1px solid #ddd' }}>{device.type}</td>
                  <td style={{ padding: '10px', border: '1px solid #ddd' }}>{device.location}</td>
                  <td style={{ padding: '10px', border: '1px solid #ddd' }}>
                    <span style={{
                      padding: '4px 8px',
                      borderRadius: '4px',
                      background: device.status === 'DZIAŁA' ? '#d4edda' : '#f8d7da',
                      color: device.status === 'DZIAŁA' ? '#155724' : '#721c24'
                    }}>
                      {device.status}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

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

