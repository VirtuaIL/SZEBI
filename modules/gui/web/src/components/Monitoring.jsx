import { useState, useEffect, useRef, useCallback } from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import './Monitoring.css';
import { apiRequest, handleApiError, getApiBaseUrl } from '../utils/api';
import { showToast } from './ToastContainer';

const API_URL = getApiBaseUrl();

export default function Monitoring({ userRole }) {
  const [devices, setDevices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedDevice, setSelectedDevice] = useState(null);
  const [deviceHistory, setDeviceHistory] = useState(null);
  
  // Refs do przechowywania aktualnych wartości (aby uniknąć problemów z zależnościami w useEffect)
  const devicesRef = useRef(devices);
  const selectedDeviceRef = useRef(selectedDevice);
  
  // Aktualizuj refs gdy zmieniają się wartości
  useEffect(() => {
    devicesRef.current = devices;
  }, [devices]);
  
  useEffect(() => {
    selectedDeviceRef.current = selectedDevice;
  }, [selectedDevice]);

  const formatValue = useCallback((value, metricLabel) => {
    if (value === null || value === undefined) return 'N/A';
    if (metricLabel && metricLabel.includes('temperatura')) {
      return `${value.toFixed(1)}°C`;
    } else if (metricLabel && (metricLabel.includes('jasnosc') || metricLabel.includes('procent'))) {
      return `${Math.round(value)}%`;
    } else {
      return value.toFixed(2);
    }
  }, []);

  // Agreguj dane - grupowanie odczytów co określony interwał czasowy
  const aggregateReadings = useCallback((readings, intervalMinutes = 15) => {
    if (!readings || readings.length === 0) return [];

    // Sortuj odczyty po czasie
    const sorted = [...readings].sort((a, b) => {
      const timeA = new Date(a.timestamp).getTime();
      const timeB = new Date(b.timestamp).getTime();
      return timeA - timeB;
    });

    // Grupuj odczyty w przedziały czasowe
    const grouped = new Map();
    const intervalMs = intervalMinutes * 60 * 1000;

    sorted.forEach(reading => {
      const time = new Date(reading.timestamp).getTime();
      const intervalKey = Math.floor(time / intervalMs) * intervalMs;
      
      if (!grouped.has(intervalKey)) {
        grouped.set(intervalKey, {
          timestamp: intervalKey,
          values: [],
          count: 0
        });
      }
      
      const group = grouped.get(intervalKey);
      if (reading.value !== null && reading.value !== undefined) {
        group.values.push(reading.value);
        group.count++;
      }
    });

    // Oblicz średnią dla każdej grupy
    const aggregated = Array.from(grouped.values())
      .map(group => ({
        time: new Date(group.timestamp).toLocaleTimeString('pl-PL', { 
          hour: '2-digit', 
          minute: '2-digit' 
        }),
        timestamp: group.timestamp,
        value: group.values.length > 0 
          ? group.values.reduce((sum, val) => sum + val, 0) / group.values.length 
          : 0,
        count: group.count
      }))
      .sort((a, b) => a.timestamp - b.timestamp);

    // Jeśli nadal jest za dużo punktów, zmniejsz interwał lub ogranicz liczbę
    if (aggregated.length > 100) {
      // Zwiększ interwał do 30 minut
      return aggregateReadings(readings, 30);
    }

    return aggregated;
  }, []);

  const loadDeviceHistory = useCallback(async (deviceId) => {
    try {
      // Pobierz historię odczytów z ostatnich 24h
      const now = new Date();
      const yesterday = new Date(now.getTime() - 24 * 60 * 60 * 1000);
      
      const from = yesterday.toISOString().replace('Z', '');
      const to = now.toISOString().replace('Z', '');
      
      const response = await apiRequest(`/devices/${deviceId}/readings?from=${from}&to=${to}`);
      
      // Jeśli jest za dużo odczytów, agreguj je
      let mappedHistory;
      if (response.length > 50) {
        // Agreguj dane co 15 minut
        mappedHistory = aggregateReadings(response, 15);
      } else {
        // Mapuj wszystkie odczyty
        mappedHistory = response.map(reading => ({
          time: reading.time || new Date(reading.timestamp).toLocaleTimeString('pl-PL', { 
            hour: '2-digit', 
            minute: '2-digit' 
          }),
          value: reading.value || 0
        }));
      }
      
      setDeviceHistory(mappedHistory);
    } catch (error) {
      console.error('Błąd ładowania historii urządzenia:', error);
      handleApiError(error, showToast);
    }
  }, [aggregateReadings]);

  // Odśwież wartość dla pojedynczego urządzenia (używa refs)
  const refreshDeviceValue = useCallback(async (deviceId) => {
    try {
      const currentDevices = devicesRef.current;
      const device = currentDevices.find(d => d.id === deviceId);
      if (!device) return;

      const response = await apiRequest(`/devices/${deviceId}/read`, {
        method: 'POST'
      });

      const updatedDevice = {
        ...device,
        currentValue: formatValue(response.value, device.metricLabel),
        lastUpdate: response.timestamp
      };

      // Zaktualizuj w liście
      setDevices(currentDevices.map(d => d.id === deviceId ? updatedDevice : d));

      // Zaktualizuj wybrane urządzenie jeśli jest otwarte
      const currentSelected = selectedDeviceRef.current;
      if (currentSelected && currentSelected.id === deviceId) {
        setSelectedDevice(updatedDevice);
      }
    } catch (error) {
      console.log(`Nie można odczytać wartości dla urządzenia ${deviceId}:`, error);
    }
  }, [formatValue]);

  // Odśwież wartości dla wszystkich urządzeń (używa refs aby uniknąć problemów z zależnościami)
  const refreshAllDeviceValues = useCallback(async (devicesToRefresh = null) => {
    const devicesList = devicesToRefresh || devicesRef.current;
    if (devicesList.length === 0) return;

    // Odczytuj wartości równolegle dla wszystkich urządzeń
    const refreshPromises = devicesList.map(async (device) => {
      try {
        const response = await apiRequest(`/devices/${device.id}/read`, {
          method: 'POST'
        });
        return {
          ...device,
          currentValue: formatValue(response.value, device.metricLabel),
          lastUpdate: response.timestamp
        };
      } catch (error) {
        // Jeśli nie można odczytać, zachowaj poprzednią wartość
        console.log(`Nie można odczytać wartości dla urządzenia ${device.id}:`, error);
        return device;
      }
    });

    try {
      const updatedDevices = await Promise.all(refreshPromises);
      setDevices(updatedDevices);
      
      // Zaktualizuj też wybrane urządzenie jeśli jest otwarte
      const currentSelected = selectedDeviceRef.current;
      if (currentSelected) {
        const updatedSelected = updatedDevices.find(d => d.id === currentSelected.id);
        if (updatedSelected) {
          setSelectedDevice(updatedSelected);
        }
      }
    } catch (error) {
      console.error('Błąd podczas odświeżania wartości:', error);
    }
  }, [formatValue]);

  const loadDevices = useCallback(async () => {
    try {
      const response = await apiRequest('/devices');
      
      // Mapuj odpowiedź z API na format używany przez GUI
      const mappedDevices = response.map(device => ({
        id: device.id,
        type: device.type || 'Nieznany typ',
        location: device.location || 'Nieznana lokalizacja',
        status: device.status || 'DZIAŁA',
        currentValue: device.currentValue || 'Ładowanie...',
        lastUpdate: device.lastUpdate || new Date().toISOString(),
        pokojId: device.pokojId,
        aktywny: device.aktywny,
        producer: device.producer,
        model: device.model,
        metricLabel: device.metricLabel
      }));
      
      setDevices(mappedDevices);
      setLoading(false);
      
      // Po załadowaniu listy, odśwież wartości dla wszystkich urządzeń
      refreshAllDeviceValues(mappedDevices);
    } catch (error) {
      console.error('Błąd ładowania urządzeń:', error);
      handleApiError(error, showToast);
      setLoading(false);
    }
  }, [refreshAllDeviceValues, formatValue]);

  useEffect(() => {
    loadDevices();
    // Odświeżaj listę urządzeń co 30 sekund
    const devicesInterval = setInterval(() => {
      loadDevices();
    }, 30000);
    
    return () => {
      clearInterval(devicesInterval);
    };
  }, [loadDevices]);

  // Osobny efekt do odświeżania wartości (uruchamia się po załadowaniu urządzeń)
  useEffect(() => {
    if (devices.length === 0) return;
    
    // Odświeżaj wartości urządzeń co 15 sekund
    const valuesInterval = setInterval(() => {
      refreshAllDeviceValues();
    }, 15000);
    
    // Odśwież wartości od razu po załadowaniu
    refreshAllDeviceValues();
    
    return () => {
      clearInterval(valuesInterval);
    };
  }, [devices.length, refreshAllDeviceValues]);

  // Automatyczne odświeżanie wartości dla wybranego urządzenia
  useEffect(() => {
    if (!selectedDevice) return;
    
    // Odświeżaj wartość wybranego urządzenia co 10 sekund
    const selectedInterval = setInterval(() => {
      refreshDeviceValue(selectedDevice.id);
    }, 10000);
    
    // Odświeżaj historię co 30 sekund
    const historyInterval = setInterval(() => {
      if (selectedDeviceRef.current) {
        loadDeviceHistory(selectedDeviceRef.current.id);
      }
    }, 30000);
    
    return () => {
      clearInterval(selectedInterval);
      clearInterval(historyInterval);
    };
  }, [selectedDevice, refreshDeviceValue, loadDeviceHistory]);

  const handleDeviceClick = useCallback(async (device) => {
    setSelectedDevice(device);
    
    // Spróbuj pobrać aktualną wartość dla wybranego urządzenia
    try {
      const readingResponse = await apiRequest(`/devices/${device.id}/read`, {
        method: 'POST'
      });
      
      // Zaktualizuj wybrane urządzenie z aktualną wartością
      const updatedDevice = {
        ...device,
        currentValue: formatValue(readingResponse.value, device.metricLabel),
        lastUpdate: readingResponse.timestamp
      };
      setSelectedDevice(updatedDevice);
      
      // Zaktualizuj również w liście urządzeń
      setDevices(prevDevices => prevDevices.map(d =>
        d.id === device.id ? updatedDevice : d
      ));
    } catch (error) {
      // Jeśli nie można odczytać, po prostu pokaż szczegóły bez aktualnej wartości
      console.log('Nie można odczytać aktualnej wartości:', error);
    }
    
    // Załaduj historię odczytów
    loadDeviceHistory(device.id);
  }, [formatValue, loadDeviceHistory]);

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
              <button 
                className="btn-refresh"
                onClick={async () => {
                  await refreshDeviceValue(selectedDevice.id);
                  showToast('Odczyt zaktualizowany', 'success', 2000);
                }}
                title="Ręczne odświeżenie (automatyczne co 10 sekund)"
              >
                Odśwież
              </button>
            </div>
            <div className="detail-item">
              <label>Ostatnia Aktualizacja:</label>
              <span>{new Date(selectedDevice.lastUpdate).toLocaleString('pl-PL')}</span>
            </div>
          </div>
          
          {deviceHistory && deviceHistory.length > 0 && (
            <div className="device-history">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
                <h4>Historia Odczytów (Ostatnie 24h)</h4>
                <div style={{ fontSize: '0.85em', color: '#666' }}>
                  {deviceHistory.length} punktów danych
                  {deviceHistory.length > 50 && ' (zagregowane co 15-30 min)'}
                </div>
              </div>
              <ResponsiveContainer width="100%" height={350}>
                <LineChart 
                  data={deviceHistory}
                  margin={{ top: 5, right: 20, left: 10, bottom: 60 }}
                >
                  <CartesianGrid strokeDasharray="3 3" stroke="#e0e0e0" />
                  <XAxis 
                    dataKey="time" 
                    angle={-45}
                    textAnchor="end"
                    height={80}
                    interval="preserveStartEnd"
                    tick={{ fontSize: 11 }}
                    tickCount={10}
                  />
                  <YAxis 
                    domain={['auto', 'auto']}
                    tick={{ fontSize: 11 }}
                  />
                  <Tooltip 
                    contentStyle={{ 
                      backgroundColor: 'rgba(255, 255, 255, 0.95)',
                      border: '1px solid #ccc',
                      borderRadius: '4px'
                    }}
                    formatter={(value) => [typeof value === 'number' ? value.toFixed(2) : value, 'Wartość']}
                    labelFormatter={(label) => `Czas: ${label}`}
                  />
                  <Legend />
                  <Line 
                    type="monotone" 
                    dataKey="value" 
                    stroke="#8884d8" 
                    strokeWidth={2}
                    dot={deviceHistory.length < 50 ? { r: 3 } : false}
                    activeDot={{ r: 5 }}
                    name="Wartość"
                    animationDuration={300}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
          )}
          {deviceHistory && deviceHistory.length === 0 && (
            <div className="device-history">
              <h4>Historia Odczytów (Ostatnie 24h)</h4>
              <div style={{ padding: '40px', textAlign: 'center', color: '#999' }}>
                Brak danych historycznych dla tego urządzenia
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

