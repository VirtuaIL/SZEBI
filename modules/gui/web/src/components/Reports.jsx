import { useState, useEffect } from 'react';
import { LineChart, Line, ScatterChart, Scatter, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import './Reports.css';
import { getApiBaseUrl } from '../utils/api';

const API_URL = getApiBaseUrl();

export default function Reports({ userRole }) {
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [selectedZone, setSelectedZone] = useState('all');
  const [reportType, setReportType] = useState('energy');
  const [generating, setGenerating] = useState(false);
  const [reportData, setReportData] = useState(null);
  const [anomalies, setAnomalies] = useState([]);

  const handleGenerate = async () => {
    setGenerating(true);
    try {
      // TODO: Implementować endpoint API
      console.log('Generowanie raportu:', { dateFrom, dateTo, selectedZone, reportType });
      
      // Mock data z anomaliami
      const mockData = [];
      const mockAnomalies = [];
      for (let i = 0; i < 24; i++) {
        const value = Math.floor(Math.random() * 500) + 800;
        const isAnomaly = Math.random() < 0.1; // 10% szansy na anomalię
        
        mockData.push({
          time: `${i}:00`,
          value: value,
          isAnomaly: isAnomaly
        });
        
        if (isAnomaly) {
          mockAnomalies.push({
            time: `${i}:00`,
            value: value,
            type: value > 1000 ? 'high' : 'low'
          });
        }
      }
      
      setReportData(mockData);
      setAnomalies(mockAnomalies);
      alert('Raport został wygenerowany');
    } catch (error) {
      console.error('Błąd generowania raportu:', error);
      alert('Błąd podczas generowania raportu');
    } finally {
      setGenerating(false);
    }
  };

  const handleExport = (format) => {
    // TODO: Implementować eksport
    alert(`Eksport do ${format} - do implementacji`);
  };

  return (
    <div className="reports">
      <h2>Generator Raportów</h2>

      <div className="report-filters">
        <div className="filter-group">
          <label>Typ raportu:</label>
          <select value={reportType} onChange={(e) => setReportType(e.target.value)}>
            <option value="energy">Zużycie energii</option>
            <option value="alerts">Historia alarmów</option>
            <option value="devices">Stan urządzeń</option>
            <option value="costs">Koszty</option>
          </select>
        </div>

        <div className="filter-group">
          <label>Medium:</label>
          <select>
            <option value="power">Prąd</option>
            <option value="temp">Temperatura</option>
            <option value="humidity">Wilgotność</option>
            <option value="all">Wszystkie</option>
          </select>
        </div>

        <div className="filter-group">
          <label>Zakres dat - od:</label>
          <input 
            type="date" 
            value={dateFrom}
            onChange={(e) => setDateFrom(e.target.value)}
          />
        </div>

        <div className="filter-group">
          <label>Zakres dat - do:</label>
          <input 
            type="date" 
            value={dateTo}
            onChange={(e) => setDateTo(e.target.value)}
          />
        </div>

        <div className="filter-group">
          <label>Strefa:</label>
          <select value={selectedZone} onChange={(e) => setSelectedZone(e.target.value)}>
            <option value="all">Cały budynek</option>
            <option value="floor1">Piętro 1</option>
            <option value="floor2">Piętro 2</option>
            <option value="room101">Pokój 101</option>
            <option value="room102">Pokój 102</option>
          </select>
        </div>

        <div className="filter-actions">
          <button 
            className="btn-generate"
            onClick={handleGenerate}
            disabled={generating || !dateFrom || !dateTo}
          >
            {generating ? 'Generowanie...' : 'Generuj Raport'}
          </button>
        </div>
      </div>

      {/* Wizualizacja z anomaliami */}
      {reportData && reportData.length > 0 && (
        <div className="report-visualization">
          <h3>Wizualizacja z Wykrytymi Anomaliami</h3>
          <ResponsiveContainer width="100%" height={400}>
            <LineChart data={reportData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="time" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Line 
                type="monotone" 
                dataKey="value" 
                stroke="#8884d8" 
                name="Zużycie energii (W)"
                strokeWidth={2}
              />
              {/* Punkty anomalii */}
              {anomalies.map((anomaly, index) => (
                <Scatter
                  key={index}
                  data={[{ time: anomaly.time, value: anomaly.value }]}
                  fill={anomaly.type === 'high' ? '#e74c3c' : '#f39c12'}
                />
              ))}
            </LineChart>
          </ResponsiveContainer>
          {anomalies.length > 0 && (
            <div className="anomalies-list">
              <h4>Wykryte Anomalie ({anomalies.length})</h4>
              <ul>
                {anomalies.map((anomaly, index) => (
                  <li key={index} className={anomaly.type === 'high' ? 'anomaly-high' : 'anomaly-low'}>
                    <strong>{anomaly.time}</strong>: {anomaly.value}W 
                    ({anomaly.type === 'high' ? 'Wysokie zużycie' : 'Niskie zużycie'})
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}

      <div className="export-section">
        <h3>Eksport Raportu</h3>
        <div className="export-buttons">
          <button className="btn-export" onClick={() => handleExport('PDF')}>
            📄 Eksportuj PDF
          </button>
          <button className="btn-export" onClick={() => handleExport('JSON')}>
            📋 Eksportuj JSON
          </button>
          <button className="btn-export" onClick={() => handleExport('XML')}>
            📄 Eksportuj XML
          </button>
        </div>
      </div>
    </div>
  );
}

