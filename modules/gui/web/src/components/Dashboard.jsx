import { useState, useEffect } from 'react';
import { LineChart, Line, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import './Dashboard.css';
import { apiRequest, handleApiError, getApiBaseUrl } from '../utils/api';
import { showToast } from './ToastContainer';

const API_URL = getApiBaseUrl();

export default function Dashboard({ userRole }) {
  const [energyData, setEnergyData] = useState([]);
  const [forecastData, setForecastData] = useState([]);
  const [anomalies, setAnomalies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [timeRange, setTimeRange] = useState('day'); // day, week, month

  useEffect(() => {
    loadDashboardData();
    const interval = setInterval(loadDashboardData, 60000); // Odświeżaj co minutę
    return () => clearInterval(interval);
  }, [timeRange]);

  const loadDashboardData = async () => {
    try {
      setLoading(true);
      const buildingId = 1; // Domyślny buildingId, można później pobrać z kontekstu użytkownika
      
      // Pobierz agregowane dane z dashboardu
      const response = await apiRequest(`/dashboard/summary?buildingId=${buildingId}&range=${timeRange}`);
      
      if (response.energy && response.energy.data) {
        // Przekształć dane energii do formatu wykresu
        const energyChartData = response.energy.data.map(item => ({
          [timeRange === 'day' ? 'hour' : 'day']: item[timeRange === 'day' ? 'hour' : 'day'],
          rzeczywiste: item.rzeczywiste || 0,
          prognozowane: item.prognozowane || 0
        }));
        setEnergyData(energyChartData);
      } else {
        setEnergyData([]);
      }

      // Ustaw dane OZE
      if (response.ozeStatus) {
        setForecastData([
          { name: 'Sieć', value: response.ozeStatus.grid || 0 },
          { name: 'OZE', value: response.ozeStatus.production || 0 }
        ]);
      } else {
        setForecastData([{ name: 'Sieć', value: 0 }, { name: 'OZE', value: 0 }]);
      }

      // Ustaw anomalie
      if (response.anomalies && Array.isArray(response.anomalies)) {
        setAnomalies(response.anomalies);
      } else {
        setAnomalies([]);
      }

      setLoading(false);
    } catch (error) {
      console.error('Błąd ładowania danych dashboard:', error);
      handleApiError(error, showToast);
      setLoading(false);
      // Fallback do pustych danych w przypadku błędu
      setEnergyData([]);
      setForecastData([{ name: 'Sieć', value: 0 }, { name: 'OZE', value: 0 }]);
      setAnomalies([]);
    }
  };


  if (loading) {
    return <div className="dashboard">Ładowanie dashboardu...</div>;
  }

  // Połącz dane rzeczywiste z prognozowanymi dla wykresu
  const chartData = energyData.map((item, index) => ({
    ...item,
    anomaly: anomalies.find(a => a.time === item.hour || index === 5 || index === 12) ? true : false
  }));

  return (
    <div className="dashboard">
      <div className="dashboard-header">
        <h2>Dashboard</h2>
        <div className="time-range-selector">
          <button 
            className={timeRange === 'day' ? 'active' : ''}
            onClick={() => setTimeRange('day')}
          >
            Dzień
          </button>
          <button 
            className={timeRange === 'week' ? 'active' : ''}
            onClick={() => setTimeRange('week')}
          >
            Tydzień
          </button>
          <button 
            className={timeRange === 'month' ? 'active' : ''}
            onClick={() => setTimeRange('month')}
          >
            Miesiąc
          </button>
        </div>
      </div>

      <div className="dashboard-grid">
        {/* Wykres zużycia energii */}
        <div className="dashboard-card chart-card">
          <h3>Zużycie Energii</h3>
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey={timeRange === 'day' ? 'hour' : 'day'} />
              <YAxis label={{ value: 'kWh', angle: -90, position: 'insideLeft' }} />
              <Tooltip formatter={(value) => `${value} kWh`} />
              <Legend />
              <Line 
                type="monotone" 
                dataKey="rzeczywiste" 
                stroke="#8884d8" 
                name="Rzeczywiste (kWh)"
                strokeWidth={2}
              />
              <Line 
                type="monotone" 
                dataKey="prognozowane" 
                stroke="#82ca9d" 
                name="Prognozowane (kWh)"
                strokeWidth={2}
                strokeDasharray="5 5"
              />
            </LineChart>
          </ResponsiveContainer>
          {anomalies.length > 0 && (
            <div className="anomalies-info">
              <p className="anomaly-note">
                <span className="anomaly-dot"></span>
                Czerwone kropki oznaczają wykryte anomalie
              </p>
            </div>
          )}
        </div>

        {/* Podział źródła energii */}
        <div className="dashboard-card">
          <h3>Źródło Energii</h3>
          <ResponsiveContainer width="100%" height={250}>
            <BarChart data={forecastData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" />
              <YAxis />
              <Tooltip />
              <Bar dataKey="value" fill="#8884d8" />
            </BarChart>
          </ResponsiveContainer>
          <div className="energy-summary">
            <div className="summary-item">
              <span>Sieć:</span>
              <strong>{forecastData[0]?.value || 0} W</strong>
            </div>
            <div className="summary-item">
              <span>OZE:</span>
              <strong>{forecastData[1]?.value || 0} W</strong>
            </div>
          </div>
        </div>

        {/* Statystyki szybkie */}
        <div className="dashboard-card stats-card">
          <h3>Statystyki</h3>
          <div className="stats-grid">
            <div className="stat-item">
              <div className="stat-value">
                {energyData.length > 0 
                  ? energyData.reduce((sum, d) => sum + (d.rzeczywiste || 0), 0).toFixed(2)
                  : '0.00'}
              </div>
              <div className="stat-label">Całkowite zużycie (kWh)</div>
            </div>
            <div className="stat-item">
              <div className="stat-value">
                {energyData.length > 0
                  ? (energyData.reduce((sum, d) => sum + (d.rzeczywiste || 0), 0) / energyData.length).toFixed(2)
                  : '0.00'}
              </div>
              <div className="stat-label">Średnie zużycie (kWh)</div>
            </div>
            <div className="stat-item">
              <div className="stat-value">{anomalies.length}</div>
              <div className="stat-label">Wykryte anomalie</div>
            </div>
          </div>
        </div>

        {/* Koszty (dla Najemcy) */}
        {userRole === 'user' && (
          <div className="dashboard-card cost-card">
            <h3>Koszty Energii</h3>
            <div className="cost-info">
              <div className="cost-item">
                <span>Dzisiejsze zużycie:</span>
                <strong>
                  {energyData.length > 0
                    ? energyData.reduce((sum, d) => sum + (d.rzeczywiste || 0), 0).toFixed(2)
                    : '0.00'} kWh
                </strong>
              </div>
              <div className="cost-item">
                <span>Szacowany koszt:</span>
                <strong>
                  {energyData.length > 0
                    ? (energyData.reduce((sum, d) => sum + (d.rzeczywiste || 0), 0) * 0.85).toFixed(2)
                    : '0.00'} PLN
                </strong>
              </div>
              <div className="cost-item">
                <span>Średnia cena/kWh:</span>
                <strong>0.85 PLN</strong>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

