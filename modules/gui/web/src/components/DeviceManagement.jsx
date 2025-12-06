import { useState } from 'react';
import './DeviceManagement.css';

const API_URL = 'http://localhost:8080/api';

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

  const handleAddDevice = async (e) => {
    e.preventDefault();
    try {
      // TODO: Implementować endpoint API
      console.log('Dodawanie urządzenia:', formData);
      alert('Urządzenie zostało dodane (funkcja do implementacji)');
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
    } catch (error) {
      console.error('Błąd dodawania urządzenia:', error);
      alert('Błąd podczas dodawania urządzenia');
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
              <button type="submit" className="btn-submit">Dodaj Urządzenie</button>
              <button 
                type="button" 
                className="btn-cancel"
                onClick={() => setShowAddForm(false)}
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
            <input type="number" defaultValue="18" min="16" max="25" />
            <span>°C</span>
          </div>
          <div className="form-group">
            <label>Temperatura komfortowa (max):</label>
            <input type="number" defaultValue="24" min="18" max="30" />
            <span>°C</span>
          </div>
          <div className="form-group">
            <label>Priorytet (Oszczędność vs Komfort):</label>
            <input type="range" min="1" max="10" defaultValue="7" />
            <span>7/10 (Komfort)</span>
          </div>
          <button className="btn-save-config">Zapisz Konfigurację</button>
        </div>
      </div>
    </div>
  );
}

