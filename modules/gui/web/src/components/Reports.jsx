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
  const [medium, setMedium] = useState('all');
  const [generating, setGenerating] = useState(false);
  const [reportData, setReportData] = useState(null);
  const [anomalies, setAnomalies] = useState([]);
  const [savedReports, setSavedReports] = useState([]);
  const [currentReportId, setCurrentReportId] = useState(null);
  const [loading, setLoading] = useState(false);

  // Pobierz zapisane raporty przy zaladowaniu
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
      console.error('Blad pobierania raportow:', error);
    }
  };

  const handleGenerate = async () => {
    setGenerating(true);
    try {
      const response = await fetch(`${API_URL}/reports/generate`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          type: reportType,
          dateFrom: dateFrom,
          dateTo: dateTo,
          zone: selectedZone,
          medium: medium,
          buildingId: 1
        }),
      });

      if (!response.ok) {
        throw new Error('Blad generowania raportu');
      }

      const result = await response.json();

      if (result.success && result.report) {
        setCurrentReportId(result.report.id);
        processReportData(result.report);
        fetchSavedReports(); // Odswiez liste
        alert('Raport zostal wygenerowany i zapisany');
      }
    } catch (error) {
      console.error('Blad generowania raportu:', error);
      alert('Blad podczas generowania raportu: ' + error.message);
    } finally {
      setGenerating(false);
    }
  };

  const handlePreview = async () => {
    setLoading(true);
    try {
      const response = await fetch(`${API_URL}/reports/preview`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          type: reportType,
          dateFrom: dateFrom,
          dateTo: dateTo,
          zone: selectedZone,
          medium: medium,
          buildingId: 1
        }),
      });

      if (!response.ok) {
        throw new Error('Blad podgladu raportu');
      }

      const previewData = await response.json();
      processPreviewData(previewData);
    } catch (error) {
      console.error('Blad podgladu:', error);
      alert('Blad podczas generowania podgladu: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  const processReportData = (report) => {
    if (!report.content || !report.content.data) {
      setReportData([]);
      setAnomalies([]);
      return;
    }

    const data = report.content.data;

    if (report.type === 'alerts') {
      // Dla alertow - pierwszym elementem jest summary
      const summary = data.find(d => d.type === 'summary');
      const alertsList = data.filter(d => d.type !== 'summary');
      setReportData(alertsList);
      setAnomalies([]); // Alerty nie maja anomalii w tym sensie
    } else if (report.type === 'costs') {
      setReportData(data);
      setAnomalies([]);
    } else {
      // energy, devices
      const chartData = data.map(d => ({
        time: d.time || d.deviceId || 'N/A',
        value: d.value || d.averageValue || 0,
        count: d.count || d.readingsCount || 1,
        isAnomaly: d.isAnomaly || false
      }));

      const anomalyData = chartData.filter(d => d.isAnomaly).map(d => ({
        time: d.time,
        value: d.value,
        type: d.value > 1000 ? 'high' : 'low'
      }));

      setReportData(chartData);
      setAnomalies(anomalyData);
    }
  };

  const processPreviewData = (previewData) => {
    if (!previewData.data) {
      setReportData([]);
      setAnomalies([]);
      return;
    }

    const data = previewData.data;

    if (previewData.reportType === 'alerts') {
      const alertsList = data.filter(d => d.type !== 'summary');
      setReportData(alertsList);
      setAnomalies([]);
    } else if (previewData.reportType === 'costs') {
      setReportData(data);
      setAnomalies([]);
    } else {
      const chartData = data.map(d => ({
        time: d.time || d.deviceId || 'N/A',
        value: d.value || d.averageValue || 0,
        count: d.count || d.readingsCount || 1,
        isAnomaly: d.isAnomaly || false
      }));

      const anomalyData = chartData.filter(d => d.isAnomaly).map(d => ({
        time: d.time,
        value: d.value,
        type: d.value > 1000 ? 'high' : 'low'
      }));

      setReportData(chartData);
      setAnomalies(anomalyData);
    }
  };

  const loadSavedReport = async (reportId) => {
    try {
      const response = await fetch(`${API_URL}/reports/${reportId}`);
      if (response.ok) {
        const report = await response.json();
        setCurrentReportId(report.id);
        setReportType(report.type);
        processReportData(report);
      }
    } catch (error) {
      console.error('Blad ladowania raportu:', error);
    }
  };

  const handleExport = async (format) => {
    if (!currentReportId) {
      alert('Najpierw wygeneruj raport');
      return;
    }

    try {
      const response = await fetch(`${API_URL}/reports/${currentReportId}/export/${format.toLowerCase()}`);

      if (response.status === 501) {
        const error = await response.json();
        alert(error.error + '\n' + (error.suggestion || ''));
        return;
      }

      if (!response.ok) {
        throw new Error('Blad eksportu');
      }

      // Pobierz plik
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
      console.error('Blad eksportu:', error);
      alert('Blad podczas eksportu: ' + error.message);
    }
  };

  const renderChartByType = () => {
    if (!reportData || reportData.length === 0) {
      return <p className="no-data">Brak danych do wyswietlenia</p>;
    }

    if (reportType === 'alerts') {
      return (
        <div className="alerts-report">
          <h4>Lista alarmow ({reportData.length})</h4>
          <table className="alerts-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Czas</th>
                <th>Urzadzenie</th>
                <th>Priorytet</th>
                <th>Status</th>
                <th>Tresc</th>
              </tr>
            </thead>
            <tbody>
              {reportData.map((alert, index) => (
                <tr key={index} className={`priority-${alert.priority?.toLowerCase()}`}>
                  <td>{alert.id}</td>
                  <td>{alert.timestamp}</td>
                  <td>{alert.deviceName}</td>
                  <td>{alert.priority}</td>
                  <td>{alert.status}</td>
                  <td>{alert.message}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      );
    }

    if (reportType === 'costs') {
      const costData = reportData[0];
      return (
        <div className="costs-report">
          <h4>Podsumowanie kosztow</h4>
          <div className="cost-summary">
            <div className="cost-item">
              <span className="label">Calkowite zuzycie energii:</span>
              <span className="value">{costData?.totalEnergyKwh || 0} kWh</span>
            </div>
            <div className="cost-item">
              <span className="label">Stawka za kWh:</span>
              <span className="value">{costData?.ratePerKwh || 0} {costData?.currency || 'PLN'}</span>
            </div>
            <div className="cost-item total">
              <span className="label">Calkowity koszt:</span>
              <span className="value">{costData?.totalCostPln || 0} {costData?.currency || 'PLN'}</span>
            </div>
          </div>
        </div>
      );
    }

    if (reportType === 'devices') {
      return (
        <div className="devices-report">
          <h4>Aktywnosc urzadzen</h4>
          <ResponsiveContainer width="100%" height={400}>
            <BarChart data={reportData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="time" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Bar dataKey="value" fill="#8884d8" name="Srednia wartosc" />
              <Bar dataKey="count" fill="#82ca9d" name="Liczba odczytow" />
            </BarChart>
          </ResponsiveContainer>
        </div>
      );
    }

    // Default: energy report
    return (
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
            name="Zuzycie energii (W)"
            strokeWidth={2}
            dot={(props) => {
              const { cx, cy, payload } = props;
              if (payload.isAnomaly) {
                return (
                  <circle cx={cx} cy={cy} r={6} fill="#e74c3c" stroke="#fff" strokeWidth={2} />
                );
              }
              return <circle cx={cx} cy={cy} r={3} fill="#8884d8" />;
            }}
          />
        </LineChart>
      </ResponsiveContainer>
    );
  };

  return (
    <div className="reports">
      <h2>Generator Raportow</h2>

      <div className="report-filters">
        <div className="filter-group">
          <label>Typ raportu:</label>
          <select value={reportType} onChange={(e) => setReportType(e.target.value)}>
            <option value="energy">Zuzycie energii</option>
            <option value="alerts">Historia alarmow</option>
            <option value="devices">Stan urzadzen</option>
            <option value="costs">Koszty</option>
          </select>
        </div>

        <div className="filter-group">
          <label>Medium:</label>
          <select value={medium} onChange={(e) => setMedium(e.target.value)}>
            <option value="power">Prad</option>
            <option value="temp">Temperatura</option>
            <option value="humidity">Wilgotnosc</option>
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
            <option value="all">Caly budynek</option>
            <option value="floor1">Pietro 1</option>
            <option value="floor2">Pietro 2</option>
            <option value="room101">Pokoj 101</option>
            <option value="room102">Pokoj 102</option>
          </select>
        </div>

        <div className="filter-actions">
          <button
            className="btn-preview"
            onClick={handlePreview}
            disabled={loading || !dateFrom || !dateTo}
          >
            {loading ? 'Ladowanie...' : 'Podglad'}
          </button>
          <button
            className="btn-generate"
            onClick={handleGenerate}
            disabled={generating || !dateFrom || !dateTo}
          >
            {generating ? 'Generowanie...' : 'Generuj i Zapisz'}
          </button>
        </div>
      </div>

      {/* Wizualizacja */}
      {reportData && reportData.length > 0 && (
        <div className="report-visualization">
          <h3>Wizualizacja {reportType === 'energy' && anomalies.length > 0 ? 'z Wykrytymi Anomaliami' : ''}</h3>
          {renderChartByType()}

          {anomalies.length > 0 && reportType === 'energy' && (
            <div className="anomalies-list">
              <h4>Wykryte Anomalie ({anomalies.length})</h4>
              <ul>
                {anomalies.map((anomaly, index) => (
                  <li key={index} className={anomaly.type === 'high' ? 'anomaly-high' : 'anomaly-low'}>
                    <strong>{anomaly.time}</strong>: {anomaly.value}W
                    ({anomaly.type === 'high' ? 'Wysokie zuzycie' : 'Niskie zuzycie'})
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}

      {/* Zapisane raporty */}
      {savedReports.length > 0 && (
        <div className="saved-reports">
          <h3>Zapisane Raporty</h3>
          <table className="reports-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Typ</th>
                <th>Data wygenerowania</th>
                <th>Opis</th>
                <th>Akcje</th>
              </tr>
            </thead>
            <tbody>
              {savedReports.slice(0, 10).map((report) => (
                <tr key={report.id} className={currentReportId === report.id ? 'selected' : ''}>
                  <td>{report.id}</td>
                  <td>{report.type}</td>
                  <td>{new Date(report.generatedAt).toLocaleString('pl-PL')}</td>
                  <td>{report.description}</td>
                  <td>
                    <button onClick={() => loadSavedReport(report.id)}>Wczytaj</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <div className="export-section">
        <h3>Eksport Raportu</h3>
        <div className="export-buttons">
          <button className="btn-export" onClick={() => handleExport('PDF')} disabled={!currentReportId}>
            Eksportuj PDF
          </button>
          <button className="btn-export" onClick={() => handleExport('JSON')} disabled={!currentReportId}>
            Eksportuj JSON
          </button>
          <button className="btn-export" onClick={() => handleExport('XML')} disabled={!currentReportId}>
            Eksportuj XML
          </button>
        </div>
      </div>
    </div>
  );
}