import { useState, useEffect } from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import './Monitoring.css';
import { handleApiError, getApiBaseUrl } from '../utils/api';
import { showToast } from './ToastContainer';

const API_URL = getApiBaseUrl();

export default function Monitoring({ userRole }) {
  const [devices, setDevices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedDevice, setSelectedDevice] = useState(null);
  const [deviceHistory, setDeviceHistory] = useState(null);

  useEffect(() => {
    loadDevices();
    // Odświeżaj co 10 sekund
    const interval = setInterval(loadDevices, 10000);
    return () => clearInterval(interval);
  }, []);

  const loadDevices = async () => {
    try {
      // TODO: Implementować endpoint API
      // Na razie mock data
      setDevices([
        {
          id: 1,
          type: 'Czujnik Temperatury',
          location: 'Pokój 101, Piętro 1',
          status: 'DZIAŁA',
          currentValue: '22.5°C',
          lastUpdate: new Date().toISOString()
        },
        {
          id: 2,
          type: 'Oświetlenie LED',
          location: 'Pokój 102, Piętro 1',
          status: 'DZIAŁA',
          currentValue: '49%',
          lastUpdate: new Date(Date.now() - 60000).toISOString()
        },
        {
          id: 3,
          type: 'Klimatyzator',
          location: 'Serwerownia, Piętro 0',
          status: 'BŁĄD',
          currentValue: 'N/A',
          lastUpdate: new Date(Date.now() - 300000).toISOString()
        },
        {
          id: 4,
          type: 'Czujnik Temperatury',
          location: 'Hala Główna, Piętro 1',
          status: 'DZIAŁA',
          currentValue: '18.2°C',
          lastUpdate: new Date(Date.now() - 30000).toISOString()
        }
      ]);
      setLoading(false);
    } catch (error) {
      console.error('Błąd ładowania urządzeń:', error);
      handleApiError(error, showToast);
      setLoading(false);
    }
  };

  const handleAddDevice = () => {
    // TODO: Implementować formularz dodawania urządzenia
    alert('Funkcja dodawania urządzenia - do implementacji');
  };

  const loadDeviceHistory = async (deviceId) => {
    try {
      // TODO: Implementować endpoint API
      // Mock data - historia odczytów
      const mockHistory = [];
      for (let i = 23; i >= 0; i--) {
        const time = new Date(Date.now() - i * 60 * 60 * 1000);
        mockHistory.push({
          time: time.toLocaleTimeString('pl-PL', { hour: '2-digit', minute: '2-digit' }),
          value: Math.floor(Math.random() * 10) + 18
        });
      }
      setDeviceHistory(mockHistory);
    } catch (error) {
      console.error('Błąd ładowania historii urządzenia:', error);
      handleApiError(error, showToast);
    }
  };

  const handleDeviceClick = (device) => {
    setSelectedDevice(device);
    loadDeviceHistory(device.id);
  };

  const getStatusClass = (status) => {
    switch (status) {
      case 'DZIAŁA': return 'status-ok';
      case 'BŁĄD': return 'status-error';
      default: return 'status-unknown';
    }
  };

  if (loading) {
    return <div className="monitoring">Ładowanie urządzeń...</div>;
  }

  return (
    <div className="monitoring">
      <div className="monitoring-header">
        <h2>Monitoring i Sprzęt</h2>
        {userRole === 'admin' && (
          <button className="btn-add-device" onClick={handleAddDevice}>
            + Dodaj Urządzenie
          </button>
        )}
      </div>

      <div className="devices-table-container">
        <table className="devices-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Typ</th>
              <th>Lokalizacja</th>
              <th>Status</th>
              <th>Aktualny Odczyt</th>
              <th>Ostatnia Aktualizacja</th>
              <th>Akcje</th>
            </tr>
          </thead>
          <tbody>
            {devices.map(device => (
              <tr 
                key={device.id}
                className={selectedDevice?.id === device.id ? 'selected' : ''}
                onClick={() => setSelectedDevice(device)}
              >
                <td>{device.id}</td>
                <td>{device.type}</td>
                <td>{device.location}</td>
                <td>
                  <span className={`status-badge ${getStatusClass(device.status)}`}>
                    {device.status}
                  </span>
                </td>
                <td><strong>{device.currentValue}</strong></td>
                <td>{new Date(device.lastUpdate).toLocaleString('pl-PL')}</td>
                <td>
                  <button 
                    className="btn-view"
                    onClick={(e) => {
                      e.stopPropagation();
                      handleDeviceClick(device);
                    }}
                  >
                    Podgląd
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {selectedDevice && (
        <div className="device-details">
          <div className="details-header">
            <h3>Szczegóły Urządzenia #{selectedDevice.id}</h3>
            <button className="btn-close" onClick={() => {
              setSelectedDevice(null);
              setDeviceHistory(null);
            }}>×</button>
          </div>
          <div className="details-content">
            <div className="detail-item">
              <label>Typ:</label>
              <span>{selectedDevice.type}</span>
            </div>
            <div className="detail-item">
              <label>Lokalizacja:</label>
              <span>{selectedDevice.location}</span>
            </div>
            <div className="detail-item">
              <label>Status:</label>
              <span className={`status-badge ${getStatusClass(selectedDevice.status)}`}>
                {selectedDevice.status}
              </span>
            </div>
            <div className="detail-item">
              <label>Aktualny Odczyt:</label>
              <span className="current-value">{selectedDevice.currentValue}</span>
            </div>
            <div className="detail-item">
              <label>Ostatnia Aktualizacja:</label>
              <span>{new Date(selectedDevice.lastUpdate).toLocaleString('pl-PL')}</span>
            </div>
          </div>
          
          {deviceHistory && deviceHistory.length > 0 && (
            <div className="device-history">
              <h4>Historia Odczytów (Ostatnie 24h)</h4>
              <ResponsiveContainer width="100%" height={300}>
                <LineChart data={deviceHistory}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="time" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Line 
                    type="monotone" 
                    dataKey="value" 
                    stroke="#8884d8" 
                    name="Wartość"
                    strokeWidth={2}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

