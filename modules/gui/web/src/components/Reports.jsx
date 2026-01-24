import { useState, useEffect } from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, BarChart, Bar } from 'recharts';
import './Reports.css';
import { getApiBaseUrl } from '../utils/api';

const API_URL = getApiBaseUrl();

export default function Reports({ userRole }) {
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [selectedZone, setSelectedZone] = useState('all');
  const [reportType, setReportType] = useState('energy');
  const [medium, setMedium] = useState('');
  const [reportData, setReportData] = useState(null);
  const [anomalies, setAnomalies] = useState([]);
  const [savedReports, setSavedReports] = useState([]);
  const [currentReportId, setCurrentReportId] = useState(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    fetchSavedReports();
  }, []);

  const fetchSavedReports = async () => {
    try {
      const response = await fetch(`${API_URL}/reports`);
      if (response.ok) {
        const data = await response.json();
        setSavedReports(data);
      }
    } catch (error) {
      console.error('Błąd pobierania raportów:', error);
    }
  };

  const handleGenerate = async () => {
    if (!dateFrom || !dateTo) {
      alert('Proszę wybrać zakres dat');
      return;
    }

    setLoading(true);
    setReportData(null);
    setAnomalies([]);
    setCurrentReportId(null);
    
    try {
      const response = await fetch(`${API_URL}/reports/preview`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          type: reportType,
          dateFrom,
          dateTo,
          zone: selectedZone,
          medium,
          buildingId: 1
        }),
      });

      if (!response.ok) throw new Error('Błąd generowania danych');

      const previewData = await response.json();
      processPreviewData(previewData);
    } catch (error) {
      console.error('Błąd:', error);
      alert('Błąd podczas generowania: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  const handleSaveReport = async () => {
    setSaving(true);
    try {
      const response = await fetch(`${API_URL}/reports/generate`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          type: reportType,
          dateFrom,
          dateTo,
          zone: selectedZone,
          medium,
          buildingId: 1
        }),
      });

      if (!response.ok) throw new Error('Błąd zapisu raportu');

      const result = await response.json();
      if (result.success && result.report) {
        setCurrentReportId(result.report.id);
        fetchSavedReports();
        alert('Raport został zapisany.');
      }
    } catch (error) {
      console.error('Błąd zapisu:', error);
      alert('Nie udało się zapisać raportu.');
    } finally {
      setSaving(false);
    }
  };

  const processPreviewData = (previewData) => {
    if (!previewData.data) {
      setReportData([]);
      setAnomalies([]);
      return;
    }

    const data = previewData.data;
    const type = previewData.reportType;

    if (type === 'alerts') {
      setReportData(data.filter(d => d.type !== 'summary'));
      setAnomalies([]);
    } else if (type === 'costs') {
      setReportData(data);
      setAnomalies([]);
    } else {
      const chartData = data.map(d => ({
        time: d.time || d.deviceId || 'N/A',
        value: d.value || d.averageValue || 0,
        count: d.count || d.readingsCount || 1,
        isAnomaly: d.isAnomaly || false
      }));

      const detectedAnomalies = chartData
        .filter(d => d.isAnomaly)
        .map(d => ({
          time: d.time,
          value: d.value,
          type: d.value > 1000 ? 'high' : 'low' 
        }));

      setReportData(chartData);
      setAnomalies(detectedAnomalies);
    }
  };

  const loadSavedReport = async (reportId) => {
    try {
      const response = await fetch(`${API_URL}/reports/${reportId}`);
      if (response.ok) {
        const report = await response.json();
        setCurrentReportId(report.id);
        setReportType(report.type);
        processPreviewData({
            data: report.content.data,
            reportType: report.type
        });
      }
    } catch (error) {
      console.error('Błąd ładowania raportu:', error);
    }
  };

  const handleExport = async (format) => {
    if (!currentReportId) {
      alert('Najpierw zapisz raport');
      return;
    }
    try {
      const response = await fetch(`${API_URL}/reports/${currentReportId}/export/${format.toLowerCase()}`);
      
      if (response.status === 501) {
        const errorData = await response.json();
        alert(errorData.error + (errorData.suggestion ? '\n' + errorData.suggestion : ''));
        return;
      }

      if (!response.ok) throw new Error('Błąd eksportu');

      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `raport_${currentReportId}.${format.toLowerCase()}`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      a.remove();
    } catch (error) {
      alert('Błąd eksportu: ' + error.message);
    }
  };

  const renderChartByType = () => {
    if (!reportData || reportData.length === 0) return <p className="no-data">Brak danych</p>;
    
    if (reportType === 'alerts') {
      return (
        <table className="reports-table">
          <thead>
            <tr><th>Czas</th><th>Urządzenie</th><th>Priorytet</th><th>Status</th></tr>
          </thead>
          <tbody>
            {reportData.map((a, i) => (
              <tr key={i}><td>{a.timestamp}</td><td>{a.deviceName}</td><td>{a.priority}</td><td>{a.status}</td></tr>
            ))}
          </tbody>
        </table>
      );
    }

    if (reportType === 'costs') {
      const cost = reportData[0];
      return (
        <div className="cost-summary">
          <div className="cost-item">
            <span className="label">Koszt całkowity:</span>
            <span className="value">{cost?.totalCostPln} {cost?.currency}</span>
          </div>
          <div className="cost-item">
            <span className="label">Zużycie:</span>
            <span className="value">{cost?.totalEnergyKwh} kWh</span>
          </div>
        </div>
      );
    }

    return (
      <ResponsiveContainer width="100%" height={350}>
        <LineChart data={reportData}>
          <CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="time" /><YAxis /><Tooltip />
          <Line 
            type="monotone" 
            dataKey="value" 
            stroke="#007bff" 
            strokeWidth={2} 
            dot={(props) => {
                const { cx, cy, payload } = props;
                if (payload.isAnomaly) {
                    return <circle cx={cx} cy={cy} r={6} fill="#e74c3c" stroke="#fff" strokeWidth={2} />;
                }
                return <circle cx={cx} cy={cy} r={3} fill="#007bff" />;
            }} 
          />
        </LineChart>
      </ResponsiveContainer>
    );
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
          <select value={medium} onChange={(e) => setMedium(e.target.value)}>
            <option value="power">Prąd</option>
            <option value="temperature">Temperatura</option>
            <option value="humidity">Wilgotność</option>
            <option value="pressure">Ciśnienie</option>
            <option value="luminosity">Jasność</option>
            <option value="co2_level">Poziom CO2</option>
            <option value="noise_level">Poziom hałasu</option>
            <option value="">Wszystkie</option>
          </select>
        </div>

        <div className="filter-group">
          <label>Data od:</label>
          <input type="date" value={dateFrom} onChange={(e) => setDateFrom(e.target.value)} />
        </div>

        <div className="filter-group">
          <label>Data do:</label>
          <input type="date" value={dateTo} onChange={(e) => setDateTo(e.target.value)} />
        </div>

        <div className="filter-group">
          <label>Strefa:</label>
          <select value={selectedZone} onChange={(e) => setSelectedZone(e.target.value)}>
            <option value="all">Cały budynek</option>
            <option value="floor1">Piętro 1</option>
            <option value="floor2">Piętro 2</option>
            <option value="room101">Pokój 101</option>
          </select>
        </div>

        <div className="filter-actions">
          <button className="btn-generate" onClick={handleGenerate} disabled={loading}>
            {loading ? 'Generowanie...' : 'Generuj'}
          </button>
        </div>
      </div>

      {reportData && (
        <div className="report-visualization">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
            <h3>Podgląd wyników</h3>
            {!currentReportId && (
              <button className="btn-save-action" onClick={handleSaveReport} disabled={saving}>
                {saving ? 'Zapisywanie...' : '💾 Zapisz raport w systemie'}
              </button>
            )}
          </div>
          
          {renderChartByType()}

          {anomalies.length > 0 && reportType === 'energy' && (
            <div className="anomalies-list">
              <h4>Wykryte Anomalie ({anomalies.length})</h4>
              <ul>
                {anomalies.map((a, i) => (
                    <li key={i} className={a.type === 'high' ? 'anomaly-high' : 'anomaly-low'}>
                        <strong>{a.time}</strong>: {a.value}W 
                        <span> ({a.type === 'high' ? 'Wysokie zużycie' : 'Niskie zużycie'})</span>
                    </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}

      {savedReports.length > 0 && (
        <div className="saved-reports">
          <h3>Ostatnio zapisane raporty</h3>
          <table className="reports-table">
            <thead>
              <tr><th>ID</th><th>Typ</th><th>Data wyg.</th><th>Akcje</th></tr>
            </thead>
            <tbody>
              {savedReports.slice(0, 5).map(r => (
                <tr key={r.id} className={currentReportId === r.id ? 'selected' : ''}>
                  <td>{r.id}</td><td>{r.type}</td>
                  <td>{new Date(r.generatedAt).toLocaleString()}</td>
                  <td><button className="btn-load" onClick={() => loadSavedReport(r.id)}>Wczytaj</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <div className="export-section">
        <h3>Eksportuj wybrany raport</h3>
        <div className="export-buttons">
          <button className="btn-export" onClick={() => handleExport('PDF')} disabled={!currentReportId}>Pobierz PDF</button>
          <button className="btn-export" onClick={() => handleExport('JSON')} disabled={!currentReportId}>Pobierz JSON</button>
          <button className="btn-export" onClick={() => handleExport('XML')} disabled={!currentReportId}>Pobierz XML</button>
        </div>
      </div>
    </div>
  );
}
