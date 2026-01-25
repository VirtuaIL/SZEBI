import { useState, useEffect } from 'react';
import { LineChart, Line, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, Cell, ScatterChart, Scatter, ComposedChart } from 'recharts';
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
    let active = true;

    const loadDashboardData = async () => {
      try {
        setLoading(true);
        const buildingId = 1; // Domyślny buildingId, można później pobrać z kontekstu użytkownika

        // Pobierz agregowane dane z dashboardu
        const response = await apiRequest(`/dashboard/summary?buildingId=${buildingId}&range=${timeRange}`);

        // Jeśli komponent został odmontowany lub timeRange się zmienił, ignorujemy wynik
        if (!active) return;

        if (response.energy && response.energy.data) {
          // Przekształć dane energii do formatu wykresu
          const energyChartData = response.energy.data.map(item => ({
            [timeRange === 'day' ? 'hour' : 'day']: item[timeRange === 'day' ? 'hour' : 'day'],
            rzeczywiste: item.rzeczywiste || 0,
            prognozowane: item.prognozowane || 0
          }));

          // Sortuj dane chronologicznie
          energyChartData.sort((a, b) => {
            if (timeRange === 'day') {
              // Porównaj godziny (np. "10:00" vs "13:00")
              // Zabezpieczenie przed undefined
              if (!a.hour || !b.hour) return 0;
              const hourA = parseInt(a.hour.split(':')[0]);
              const hourB = parseInt(b.hour.split(':')[0]);
              return hourA - hourB;
            } else {
              // Porównaj dni
              // Zabezpieczenie przed undefined
              if (!a.day || !b.day) return 0;
              return a.day.localeCompare(b.day);
            }
          });

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
        if (!active) return;
        console.error('Błąd ładowania danych dashboard:', error);
        handleApiError(error, showToast);
        setLoading(false);
        // Fallback do pustych danych w przypadku błędu
        setEnergyData([]);
        setForecastData([{ name: 'Sieć', value: 0 }, { name: 'OZE', value: 0 }]);
        setAnomalies([]);
      }
    };

    loadDashboardData();
    const interval = setInterval(loadDashboardData, 60000); // Odświeżaj co minutę

    return () => {
      active = false;
      clearInterval(interval);
    };
  }, [timeRange]);


  if (loading) {
    return <div className="dashboard">Ładowanie dashboardu...</div>;
  }

  // Helper functions for time conversion
  const timeToMinutes = (timeStr) => {
    if (!timeStr) return 0;
    const [h, m] = timeStr.split(':').map(Number);
    return h * 60 + (m || 0);
  };

  const minutesToTime = (mins) => {
    const h = Math.floor(mins / 60);
    const m = mins % 60;
    return `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}`;
  };

  // Przygotuj dane do wykresu
  let chartData = [];
  let xAxisConfig = {};

  if (timeRange === 'day') {
    // Tryb Dzień: Oś X numeryczna (minuty) dla precyzyjnego umiejscowienia anomalii

    // 1. Dane główne (godzinowe)
    const mainPoints = energyData.map(item => ({
      ...item,
      xValue: timeToMinutes(item.hour),
      originalTime: item.hour,
      isMain: true
    }));

    // 2. Punkty anomalii (z interpolacją)
    const anomalyPoints = anomalies.map(anomaly => {
      if (!anomaly.time) return null;
      const anomalyMins = timeToMinutes(anomaly.time);

      // Znajdź sąsiednie punkty do interpolacji
      // Zakładamy, że mainPoints są posortowane godzinowo (co robimy przy pobieraniu)
      const prevPoint = mainPoints.filter(p => p.xValue <= anomalyMins).pop();
      const nextPoint = mainPoints.find(p => p.xValue > anomalyMins);

      let interpolatedY = 0;
      if (prevPoint && nextPoint) {
        // Liniowa interpolacja
        const ratio = (anomalyMins - prevPoint.xValue) / (nextPoint.xValue - prevPoint.xValue);
        interpolatedY = prevPoint.rzeczywiste + (nextPoint.rzeczywiste - prevPoint.rzeczywiste) * ratio;
      } else if (prevPoint) {
        interpolatedY = prevPoint.rzeczywiste;
      } else if (nextPoint) {
        interpolatedY = nextPoint.rzeczywiste;
      }

      return {
        xValue: anomalyMins,
        anomalyValue: interpolatedY,
        rzeczywiste: null, // Nie chcemy rysować linii dla tego punktu, tylko kropkę
        prognozowane: null,
        isAnomaly: true,
        type: anomaly.type // high/medium
      };
    }).filter(Boolean);

    // Połącz i posortuj
    chartData = [...mainPoints, ...anomalyPoints].sort((a, b) => a.xValue - b.xValue);

    xAxisConfig = {
      dataKey: 'xValue',
      type: 'number',
      domain: ['dataMin', 'dataMax'],
      tickFormatter: minutesToTime,
      allowDuplicatedCategory: false
    };

  } else {
    // Tryb Tydzień/Miesiąc: Oś kategoryczna
    chartData = energyData.map((item) => {
      const hasAnomaly = anomalies.some(a => a.time === item.day);
      return {
        ...item,
        xValue: item.day,
        anomalyValue: hasAnomaly ? item.rzeczywiste : null
      };
    });

    xAxisConfig = {
      dataKey: 'xValue',
      type: 'category',
      tickFormatter: (val) => val
    };
  }

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
            <ComposedChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis
                dataKey={xAxisConfig.dataKey}
                type={xAxisConfig.type}
                tickFormatter={xAxisConfig.tickFormatter}
                domain={xAxisConfig.domain}
                allowDuplicatedCategory={xAxisConfig.allowDuplicatedCategory}
              />
              <YAxis label={{ value: 'kWh', angle: -90, position: 'insideLeft' }} />
              <Tooltip
                labelFormatter={xAxisConfig.tickFormatter}
                content={({ active, payload, label }) => {
                  if (active && payload && payload.length) {
                    // Znajdź czy to punkt anomalii
                    const isAnomalyPoint = payload[0].payload.isAnomaly;
                    const displayLabel = xAxisConfig.type === 'number' ? minutesToTime(label) : label;

                    return (
                      <div className="custom-tooltip" style={{ backgroundColor: 'white', padding: '10px', border: '1px solid #ccc', borderRadius: '4px', boxShadow: '0 2px 4px rgba(0,0,0,0.1)' }}>
                        <p style={{ margin: '0 0 5px 0', fontWeight: 'bold' }}>
                          {timeRange === 'day' ? 'Godzina: ' : 'Data: '} {displayLabel}
                          {isAnomalyPoint && <span style={{ color: 'red', marginLeft: '5px' }}>(Anomalia)</span>}
                        </p>
                        {payload.map((entry, index) => {
                          if (['xValue', 'originalTime', 'isMain', 'isAnomaly', 'anomalyValue', 'type'].includes(entry.dataKey)) return null;
                          if (entry.value === null) return null;
                          return (
                            <p key={index} style={{ margin: '3px 0', color: entry.color }}>
                              {entry.name}: {typeof entry.value === 'number' ? entry.value.toFixed(2) : entry.value} kWh
                            </p>
                          );
                        })}
                        {/* Dodatkowe info dla anomalii */}
                        {isAnomalyPoint && (
                          <p style={{ margin: '3px 0', color: 'red', fontWeight: 'bold' }}>
                            Wykryto anomalię!
                          </p>
                        )}
                      </div>
                    );
                  }
                  return null;
                }}
              />
              <Legend />
              {/* Łączymy tylko punkty główne (isMain) - Recharts połączy null'e jeśli connectNulls={true}, ale my chcemy 
                  żeby linia szła przez interpolowany punkt?
                  Właściwie, jeśli dodaliśmy punkt anomalii do danych i posortowaliśmy, to LineChart przejdzie przez niego.
                  Ale my nie chcemy, żeby wykres "załamywał" się na anomalii jeśli ona lekko odstaje od prostej? 
                  Z interpolacją będzie leżeć idealnie na prostej między punktami, więc nie ma problemu.
                  Jednak dla main points mamy value, dla anomaly points mamy null w 'rzeczywiste'.
                  Żeby linia była ciągła, musimy używać connectNulls={true} ORAZ wstawiać interpolatedY jako 'rzeczywiste' dla punktu anomalii?
                  Nie, lepiej nie zaburzać danych 'rzeczywiste'.
                  
                  Rozwiązanie: Linia rysuje tylko punkty gdzie 'rzeczywiste' != null. Punkty anomalii mają 'rzeczywiste': null.
                  Więc linia ominie te punkty (będzie przerwa) chyba że connectNulls={true}.
                  Jak damy connectNulls={true}, to linia połączy 10:00 z 11:00 ignorując punkt 10:36.
                  Kropka anomalii (Scatter) narysuje się w 10:36 na wysokości interpolatedY.
                  To zadziała idealnie! Kropka będzie na linii łączącej 10:00 i 11:00.
               */}
              <Line
                type="monotone"
                dataKey="rzeczywiste"
                stroke="#8884d8"
                name="Rzeczywiste (kWh)"
                strokeWidth={2}
                connectNulls={true}
                dot={{ r: 4 }}
                activeDot={{ r: 6 }}
              />
              <Line
                type="monotone"
                dataKey="prognozowane"
                stroke="#82ca9d"
                name="Prognozowane (kWh)"
                strokeWidth={2}
                strokeDasharray="5 5"
                connectNulls={true}
                dot={false}
              />
              {/* Czerwone kropki dla anomalii */}
              <Scatter
                dataKey="anomalyValue"
                fill="red"
                shape="circle"
                name="Anomalie"
                legendType="none"
              />
            </ComposedChart>
          </ResponsiveContainer>
          {anomalies.length > 0 && (
            <div className="anomalies-info">
              <p className="anomaly-note">
                <span className="anomaly-dot"></span>
                Punkty oznaczają wykryte anomalie w czasie rzeczywistym
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
              <YAxis label={{ value: 'W', angle: -90, position: 'insideLeft' }} />
              <Tooltip formatter={(value) => `${value} W`} />
              <Bar dataKey="value">
                {forecastData.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={entry.name === 'Sieć' ? '#ff7c7c' : '#82ca9d'} />
                ))}
              </Bar>
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

