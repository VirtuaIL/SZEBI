import { useState, useEffect } from 'react';
import { LineChart, Line, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import './Dashboard.css';
import { apiRequest, handleApiError } from '../utils/api';
import { showToast } from './ToastContainer';

const API_URL = 'http://localhost:8080/api';

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
      // TODO: Implementować endpoint API
      // Na razie mock data
      const mockEnergyData = generateMockEnergyData();
      const mockForecastData = generateMockForecastData();
      const mockAnomalies = [
        { time: '14:30', value: 1250, type: 'high' },
        { time: '18:45', value: 980, type: 'low' }
      ];

      setEnergyData(mockEnergyData);
      setForecastData(mockForecastData);
      setAnomalies(mockAnomalies);
      setLoading(false);
    } catch (error) {
      console.error('Błąd ładowania danych dashboard:', error);
      handleApiError(error, showToast);
      setLoading(false);
    }
  };

  const generateMockEnergyData = () => {
    const data = [];
    const hours = timeRange === 'day' ? 24 : timeRange === 'week' ? 7 : 30;
    const label = timeRange === 'day' ? 'hour' : timeRange === 'week' ? 'day' : 'day';

    for (let i = 0; i < hours; i++) {
      // Konwersja W na kWh (zakładając że to wartość godzinowa)
      const rzeczywisteW = Math.floor(Math.random() * 500) + 800;
      const prognozowaneW = Math.floor(Math.random() * 500) + 750;
      data.push({
        [label]: timeRange === 'day' ? `${i}:00` : `Dzień ${i + 1}`,
        rzeczywiste: parseFloat((rzeczywisteW / 1000).toFixed(2)), // kWh
        prognozowane: parseFloat((prognozowaneW / 1000).toFixed(2)) // kWh
      });
    }
    return data;
  };

  const generateMockForecastData = () => {
    return [
      { name: 'Sieć', value: 1200 },
      { name: 'OZE', value: 350 }
    ];
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
                {energyData.reduce((sum, d) => sum + (d.rzeczywiste || 0), 0).toFixed(2)}
              </div>
              <div className="stat-label">Całkowite zużycie (kWh)</div>
            </div>
            <div className="stat-item">
              <div className="stat-value">
                {(energyData.reduce((sum, d) => sum + (d.rzeczywiste || 0), 0) / energyData.length).toFixed(2)}
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
                <strong>{energyData.reduce((sum, d) => sum + (d.rzeczywiste || 0), 0).toFixed(2)} kWh</strong>
              </div>
              <div className="cost-item">
                <span>Szacowany koszt:</span>
                <strong>
                  {(energyData.reduce((sum, d) => sum + (d.rzeczywiste || 0), 0) * 0.85).toFixed(2)} PLN
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

