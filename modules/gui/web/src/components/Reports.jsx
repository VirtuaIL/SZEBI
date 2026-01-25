import { useState, useEffect } from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, BarChart, Bar } from 'recharts';
import './Reports.css';
import { getApiBaseUrl } from '../utils/api';

const API_URL = getApiBaseUrl();

export default function Reports({ userRole }) {
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [selectedZone, setSelectedZone] = useState('all');
  const [documentType, setDocumentType] = useState('report'); // 'report' or 'analysis'
  const [reportType, setReportType] = useState('energy');
  const [medium, setMedium] = useState('');
  const [reportData, setReportData] = useState(null);
  const [anomalies, setAnomalies] = useState([]);
  const [savedReports, setSavedReports] = useState([]);
  const [currentReportId, setCurrentReportId] = useState(null);
  const [loading, setLoading] = useState(false);
  const [analysisData, setAnalysisData] = useState(null);

  // Generator state
  const [generators, setGenerators] = useState([]);
  const [generatorName, setGeneratorName] = useState('');
  const [generatorPeriodDays, setGeneratorPeriodDays] = useState(1);
  const [generatorPeriodMonths, setGeneratorPeriodMonths] = useState(0);
  const [generatorPeriodYears, setGeneratorPeriodYears] = useState(0);

  // Quick generator mode (z głównego formularza)
  const [createAsGenerator, setCreateAsGenerator] = useState(false);

  useEffect(() => {
    fetchSavedReports();
    fetchGenerators();
  }, []);

  const fetchGenerators = async () => {
    try {
      const response = await fetch(`${API_URL}/reports/generators`);
      if (response.ok) {
        const data = await response.json();
        setGenerators(data);
      }
    } catch (error) {
      console.error('Błąd pobierania generatorów:', error);
    }
  };

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
    // Jeśli "Utwórz jako generator" jest zaznaczony
    if (createAsGenerator) {
      if (!generatorPeriodDays && !generatorPeriodMonths && !generatorPeriodYears) {
        alert('Proszę podać okres generowania (dni/miesiące/lata)');
        return;
      }

      setLoading(true);
      try {
        const endpoint = documentType === 'analysis'
          ? `${API_URL}/reports/analysis/addGenerator`
          : `${API_URL}/reports/addGenerator`;

        const generatorNameAuto = `Generator ${documentType === 'analysis' ? 'analiz' : 'raportów'} - ${reportType}`;

        // Użyj dat z formularza lub domyślnych (ostatnie 7 dni)
        const generatorDateFrom = dateFrom || new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString().split('T')[0];
        const generatorDateTo = dateTo || new Date().toISOString().split('T')[0];

        const response = await fetch(endpoint, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            name: generatorName || generatorNameAuto,
            dateFrom: generatorDateFrom,
            dateTo: generatorDateTo,
            periodDays: generatorPeriodDays,
            periodMonths: generatorPeriodMonths,
            periodYears: generatorPeriodYears
          }),
        });

        if (!response.ok) throw new Error('Błąd dodawania generatora');

        const result = await response.json();
        if (result.success) {
          alert('Generator został utworzony i będzie działał cyklicznie.');
          fetchGenerators();
          // Reset wartości generatora
          setCreateAsGenerator(false);
          setGeneratorName('');
          setGeneratorPeriodDays(1);
          setGeneratorPeriodMonths(0);
          setGeneratorPeriodYears(0);
        }
      } catch (error) {
        console.error('Błąd:', error);
        alert('Błąd podczas tworzenia generatora: ' + error.message);
      } finally {
        setLoading(false);
      }
      return;
    }

    // Standardowe generowanie jednorazowe - wymagane daty
    if (!dateFrom || !dateTo) {
      alert('Proszę wybrać zakres dat');
      return;
    }

    setLoading(true);
    setReportData(null);
    setAnomalies([]);
    setAnalysisData(null);
    setCurrentReportId(null);

    try {
      if (documentType === 'analysis') {
        // Generate Analysis
        const response = await fetch(`${API_URL}/reports/analysis`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            dateFrom,
            dateTo,
            medium,
            buildingId: 1
          }),
        });

        if (!response.ok) throw new Error('Błąd generowania analizy');

        const analysis = await response.json();
        setAnalysisData(analysis);
        fetchSavedReports();
        alert('Analiza została wygenerowana i zapisana.');
      } else {
        // Generate Report
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

        if (!response.ok) throw new Error('Błąd generowania raportu' + response.status);

        const result = await response.json();
        if (result.success && result.report) {
          setCurrentReportId(result.report.id);
          fetchSavedReports();
          processPreviewData({
            data: result.report.content.data || result.report.content,
            reportType: result.report.type
          });
          alert('Raport został wygenerowany i zapisany.');
        }
      }
    } catch (error) {
      console.error('Błąd:', error);
      alert('Błąd podczas generowania: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  const processPreviewData = (previewData) => {
    if (!previewData || !previewData.data) {
      setReportData([]);
      setAnomalies([]);
      return;
    }

    const data = previewData.data;
    const type = previewData.reportType;

    // Handle Report structure: Map<ConfigurationType, List<Double>>
    if (data && typeof data === 'object' && !Array.isArray(data) && Object.keys(data).length > 0) {
      // Convert map structure to array for charting
      const chartData = [];

      for (const [metricName, values] of Object.entries(data)) {
        if (Array.isArray(values)) {
          values.forEach((value, i) => {
            chartData.push({
              time: `${metricName}-${i}`,
              value: value,
              metric: metricName,
              isAnomaly: false
            });
          });
        }
      }

      setReportData(chartData);
      setAnomalies([]);
      return;
    }

    // Handle array-based structures
    if (!Array.isArray(data)) {
      console.error('Invalid data structure in report:', data);
      setReportData([]);
      setAnomalies([]);
      return;
    }

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
        // Dane mogą być w report.content.data lub bezpośrednio w report.content
        const reportData = report.content?.data || report.content;
        processPreviewData({
          data: reportData,
          reportType: report.type
        });
      }
    } catch (error) {
      console.error('Błąd ładowania raportu:', error);
    }
  };

  const handleDeleteGenerator = async (serviceKey, generatorId) => {
    if (!confirm('Czy na pewno chcesz usunąć ten generator?')) {
      return;
    }

    try {
      const response = await fetch(`${API_URL}/reports/generators/${serviceKey}/${generatorId}`, {
        method: 'DELETE'
      });

      if (!response.ok) throw new Error('Błąd usuwania generatora');

      const result = await response.json();
      if (result.success) {
        alert('Generator został usunięty.');
        fetchGenerators();
      }
    } catch (error) {
      console.error('Błąd:', error);
      alert('Błąd podczas usuwania generatora: ' + error.message);
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

    // Check if we have metric-based data
    const hasMetrics = reportData.length > 0 && reportData[0].metric;

    if (hasMetrics) {
      // Group data by metric for better visualization
      const metrics = [...new Set(reportData.map(d => d.metric))];
      const colors = ['#007bff', '#28a745', '#dc3545', '#ffc107', '#17a2b8', '#6f42c1'];

      return (
        <div>
          <ResponsiveContainer width="100%" height={350}>
            <BarChart data={reportData.slice(0, 50)}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="time" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Bar dataKey="value" fill="#007bff" />
            </BarChart>
          </ResponsiveContainer>
          <p style={{ marginTop: '10px', fontSize: '12px', color: '#666' }}>
            Wyświetlono pierwsze 50 punktów danych. Metryki: {metrics.join(', ')}
          </p>
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
          <label>Typ dokumentu:</label>
          <select value={documentType} onChange={(e) => setDocumentType(e.target.value)}>
            <option value="report">Raport (dane surowe)</option>
            <option value="analysis">Analiza (statystyki i rekomendacje)</option>
          </select>
        </div>

        {documentType === 'report' && (
          <div className="filter-group">
            <label>Typ raportu:</label>
            <select value={reportType} onChange={(e) => setReportType(e.target.value)}>
              <option value="energy">Zużycie energii</option>
              <option value="alerts">Historia alarmów</option>
              <option value="devices">Stan urządzeń</option>
              <option value="costs">Koszty</option>
            </select>
          </div>
        )}

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

        {!createAsGenerator && (
          <>
            <div className="filter-group">
              <label>Data od:</label>
              <input type="date" value={dateFrom} onChange={(e) => setDateFrom(e.target.value)} />
            </div>

            <div className="filter-group">
              <label>Data do:</label>
              <input type="date" value={dateTo} onChange={(e) => setDateTo(e.target.value)} />
            </div>
          </>
        )}

        {documentType === 'report' && (
          <div className="filter-group">
            <label>Strefa:</label>
            <select value={selectedZone} onChange={(e) => setSelectedZone(e.target.value)}>
              <option value="all">Cały budynek</option>
              <option value="floor1">Piętro 1</option>
              <option value="floor2">Piętro 2</option>
              <option value="room101">Pokój 101</option>
            </select>
          </div>
        )}
      </div>

      {/* Opcja tworzenia jako generator */}
      <div style={{ marginTop: '15px', padding: '15px', background: '#f8f9fa', borderRadius: '6px' }}>
        <label style={{ display: 'flex', alignItems: 'center', cursor: 'pointer', fontWeight: 'bold' }}>
          <input
            type="checkbox"
            checked={createAsGenerator}
            onChange={(e) => setCreateAsGenerator(e.target.checked)}
            style={{ marginRight: '10px', width: '18px', height: '18px' }}
          />
          ⚙️ Utwórz jako generator cykliczny
        </label>

        {createAsGenerator && (
          <div style={{ marginTop: '15px' }}>
            <p style={{ fontSize: '13px', color: '#666', marginBottom: '15px', padding: '10px', background: '#fff3cd', borderRadius: '4px', border: '1px solid #ffc107' }}>
              ℹ️ Generator będzie automatycznie tworzył {documentType === 'analysis' ? 'analizy' : 'raporty'} według określonego harmonogramu.
              Zakres dat zostanie ustawiony dynamicznie przy każdym uruchomieniu (ostatnie 7 dni).
            </p>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '15px' }}>
              <div className="filter-group">
                <label>Nazwa generatora (opcjonalnie):</label>
                <input
                  type="text"
                  value={generatorName}
                  onChange={(e) => setGeneratorName(e.target.value)}
                  placeholder="Automatyczna nazwa"
                />
              </div>

              <div className="filter-group">
                <label>Powtarzaj co (dni):</label>
                <input
                  type="number"
                  min="0"
                  value={generatorPeriodDays}
                  onChange={(e) => setGeneratorPeriodDays(parseInt(e.target.value) || 0)}
                />
              </div>

              <div className="filter-group">
                <label>Powtarzaj co (miesiące):</label>
                <input
                  type="number"
                  min="0"
                  value={generatorPeriodMonths}
                  onChange={(e) => setGeneratorPeriodMonths(parseInt(e.target.value) || 0)}
                />
              </div>

              <div className="filter-group">
                <label>Powtarzaj co (lata):</label>
                <input
                  type="number"
                  min="0"
                  value={generatorPeriodYears}
                  onChange={(e) => setGeneratorPeriodYears(parseInt(e.target.value) || 0)}
                />
              </div>
            </div>
          </div>
        )}
      </div>

      <div style={{ marginTop: '15px', display: 'flex', justifyContent: 'center' }}>
        <button
          className="btn-generate"
          onClick={handleGenerate}
          disabled={loading || (!createAsGenerator && (!dateFrom || !dateTo))}
          style={{ padding: '12px 40px', fontSize: '16px' }}
        >
          {loading ? 'Generowanie...' : createAsGenerator ? '⚙️ Utwórz Generator' : (documentType === 'analysis' ? '🔍 Generuj Analizę' : '💾 Generuj Raport')}
        </button>
      </div>

      {analysisData && documentType === 'analysis' && (
        <div className="analysis-results">
          <h3>📊 Wyniki Analizy</h3>
          <div className="analysis-summary" style={{ background: '#f8f9fa', padding: '15px', borderRadius: '8px', marginBottom: '20px' }}>
            <p><strong>Typ analizy:</strong> {analysisData.analysisType}</p>
            <p><strong>Liczba analizowanych metryk:</strong> {analysisData.metricsCount}</p>
            <p><strong>Ogólna ocena systemu:</strong> <span className="quality-badge" style={{
              padding: '4px 12px',
              borderRadius: '4px',
              background: analysisData.overallAssessment?.startsWith('EXCELLENT') ? '#28a745' :
                analysisData.overallAssessment?.startsWith('GOOD') ? '#17a2b8' :
                  analysisData.overallAssessment?.startsWith('FAIR') ? '#ffc107' : '#dc3545',
              color: 'white',
              fontWeight: 'bold'
            }}>{analysisData.overallAssessment}</span></p>
          </div>

          <h4 style={{ marginTop: '30px', marginBottom: '15px' }}>📊 Wizualizacja statystyk:</h4>
          {analysisData.metrics && (() => {
            // Przygotuj dane do wykresu porównawczego wszystkich metryk
            const metricsComparisonData = Object.entries(analysisData.metrics).map(([key, metric]) => ({
              name: metric.configurationType,
              'Min': metric.descriptiveStatistics?.min || 0,
              'Średnia': metric.descriptiveStatistics?.average || 0,
              'Max': metric.descriptiveStatistics?.max || 0,
              'Mediana': metric.descriptiveStatistics?.median || 0
            }));

            return (
              <div style={{ marginBottom: '30px' }}>
                <ResponsiveContainer width="100%" height={400}>
                  <BarChart data={metricsComparisonData}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="name" />
                    <YAxis />
                    <Tooltip />
                    <Legend />
                    <Bar dataKey="Min" fill="#17a2b8" />
                    <Bar dataKey="Średnia" fill="#007bff" />
                    <Bar dataKey="Mediana" fill="#28a745" />
                    <Bar dataKey="Max" fill="#dc3545" />
                  </BarChart>
                </ResponsiveContainer>
                <p style={{ textAlign: 'center', fontSize: '12px', color: '#666', marginTop: '10px' }}>
                  Porównanie statystyk opisowych dla wszystkich metryk
                </p>
              </div>
            );
          })()}

          <h4 style={{ marginTop: '30px', marginBottom: '15px' }}>Szczegółowa analiza metryk:</h4>

          {analysisData.metrics && Object.entries(analysisData.metrics).map(([key, metric]) => (
            <div key={key} className="metric-card" style={{
              border: '1px solid #ddd',
              borderRadius: '8px',
              padding: '20px',
              marginBottom: '20px',
              background: 'white'
            }}>
              <h4 style={{
                borderBottom: '2px solid #007bff',
                paddingBottom: '10px',
                marginBottom: '15px',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center'
              }}>
                <span>{metric.configurationType}</span>
                <span style={{ fontSize: '14px', color: '#666', fontWeight: 'normal' }}>
                  Typ urządzenia: {metric.deviceType}
                </span>
              </h4>

              {/* Wykresy dla tej metryki */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', marginBottom: '20px' }}>
                {/* Wykres statystyk opisowych */}
                <div style={{ background: '#f8f9fa', padding: '15px', borderRadius: '6px' }}>
                  <h5 style={{ color: '#007bff', marginBottom: '10px', textAlign: 'center' }}>📈 Statystyki opisowe</h5>
                  <ResponsiveContainer width="100%" height={200}>
                    <BarChart data={[
                      { name: 'Min', value: metric.descriptiveStatistics?.min },
                      { name: 'P25', value: metric.percentiles?.p25 },
                      { name: 'Mediana', value: metric.descriptiveStatistics?.median },
                      { name: 'Średnia', value: metric.descriptiveStatistics?.average },
                      { name: 'P75', value: metric.percentiles?.p75 },
                      { name: 'Max', value: metric.descriptiveStatistics?.max }
                    ]}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="name" />
                      <YAxis />
                      <Tooltip formatter={(value) => `${value} ${metric.unit}`} />
                      <Bar dataKey="value" fill="#007bff" />
                    </BarChart>
                  </ResponsiveContainer>
                  <div style={{ marginTop: '10px', fontSize: '12px' }}>
                    <p>Zakres: <strong>{metric.descriptiveStatistics?.range} {metric.unit}</strong></p>
                    <p>Odchylenie std: <strong>{metric.descriptiveStatistics?.standardDeviation} {metric.unit}</strong></p>
                    <p>Próbek: <strong>{metric.sampleCount}</strong></p>
                  </div>
                </div>

                {/* Wykres zgodności */}
                <div style={{ background: '#f8f9fa', padding: '15px', borderRadius: '6px' }}>
                  <h5 style={{ color: '#28a745', marginBottom: '10px', textAlign: 'center' }}>✅ Zgodność z zakresami</h5>
                  <ResponsiveContainer width="100%" height={200}>
                    <BarChart data={[
                      {
                        name: 'Zgodność',
                        'W optymalnym': metric.rangeCompliance?.inOptimalRangePercentage,
                        'Poza typowym': metric.rangeCompliance?.outOfTypicalRangePercentage,
                        'Inne': 100 - (metric.rangeCompliance?.inOptimalRangePercentage || 0) - (metric.rangeCompliance?.outOfTypicalRangePercentage || 0)
                      }
                    ]} layout="vertical">
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis type="number" domain={[0, 100]} />
                      <YAxis type="category" dataKey="name" />
                      <Tooltip formatter={(value) => `${value}%`} />
                      <Legend />
                      <Bar dataKey="W optymalnym" stackId="a" fill="#28a745" />
                      <Bar dataKey="Inne" stackId="a" fill="#ffc107" />
                      <Bar dataKey="Poza typowym" stackId="a" fill="#dc3545" />
                    </BarChart>
                  </ResponsiveContainer>
                  <div style={{ marginTop: '10px', fontSize: '12px' }}>
                    <p>Typowy zakres: <strong>{metric.referenceRanges?.typical?.min}-{metric.referenceRanges?.typical?.max} {metric.unit}</strong></p>
                    <p>Optymalny zakres: <strong>{metric.referenceRanges?.optimal?.min}-{metric.referenceRanges?.optimal?.max} {metric.unit}</strong></p>
                  </div>
                </div>
              </div>

              {/* Szczegółowe wartości numeryczne */}
              <div className="metric-stats" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '20px', marginBottom: '20px' }}>
                <div className="stat-group" style={{ background: '#f8f9fa', padding: '15px', borderRadius: '6px' }}>
                  <h5 style={{ color: '#007bff', marginBottom: '10px' }}>📊 Wartości liczbowe</h5>
                  <p>Min: <strong>{metric.descriptiveStatistics?.min} {metric.unit}</strong></p>
                  <p>P25: <strong>{metric.percentiles?.p25} {metric.unit}</strong></p>
                  <p>Mediana: <strong>{metric.descriptiveStatistics?.median} {metric.unit}</strong></p>
                  <p>Średnia: <strong>{metric.descriptiveStatistics?.average} {metric.unit}</strong></p>
                  <p>P75: <strong>{metric.percentiles?.p75} {metric.unit}</strong></p>
                  <p>Max: <strong>{metric.descriptiveStatistics?.max} {metric.unit}</strong></p>
                </div>

                <div className="stat-group" style={{ background: '#f8f9fa', padding: '15px', borderRadius: '6px' }}>
                  <h5 style={{ color: '#28a745', marginBottom: '10px' }}>✅ Zgodność (liczby)</h5>
                  <p>Poza typowym: <strong style={{ color: metric.rangeCompliance?.outOfTypicalRangePercentage > 10 ? '#dc3545' : '#28a745' }}>
                    {metric.rangeCompliance?.outOfTypicalRangeCount} / {metric.sampleCount} ({metric.rangeCompliance?.outOfTypicalRangePercentage}%)
                  </strong></p>
                  <p>W optymalnym: <strong style={{ color: metric.rangeCompliance?.inOptimalRangePercentage > 70 ? '#28a745' : '#ffc107' }}>
                    {metric.rangeCompliance?.inOptimalRangeCount} / {metric.sampleCount} ({metric.rangeCompliance?.inOptimalRangePercentage}%)
                  </strong></p>
                </div>
              </div>

              <div className="metric-assessment" style={{
                padding: '15px',
                borderRadius: '6px',
                marginBottom: '15px',
                background: metric.qualityAssessment?.startsWith('EXCELLENT') ? '#d4edda' :
                  metric.qualityAssessment?.startsWith('GOOD') ? '#d1ecf1' :
                    metric.qualityAssessment?.startsWith('FAIR') ? '#fff3cd' : '#f8d7da'
              }}>
                <p><strong>🎯 Ocena jakości:</strong> {metric.qualityAssessment}</p>
              </div>

              {metric.recommendations && metric.recommendations.length > 0 && (
                <div className="metric-recommendations" style={{
                  background: '#fff3cd',
                  padding: '15px',
                  borderRadius: '6px',
                  borderLeft: '4px solid #ffc107'
                }}>
                  <h5 style={{ color: '#856404', marginBottom: '10px' }}>💡 Rekomendacje:</h5>
                  <ul style={{ marginLeft: '20px' }}>
                    {metric.recommendations.map((rec, i) => (
                      <li key={i} style={{ marginBottom: '8px', color: '#856404' }}>{rec}</li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {reportData && documentType === 'report' && (
        <div className="report-visualization">
          <h3>Podgląd wyników {currentReportId && `(Raport #${currentReportId})`}</h3>

          {renderChartByType()}

          {anomalies.length > 0 && documentType === 'report' && (
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

      <div className="generator-section" style={{ marginTop: '40px' }}>
        <h3 style={{ marginBottom: '20px' }}>⚙️ Aktywne Generatory Cykliczne</h3>

        {generators.length > 0 ? (
          <table className="reports-table">
            <thead>
              <tr>
                <th>Nazwa</th>
                <th>Typ</th>
                <th>Okres</th>
                <th>Liczba schematów</th>
                <th>Akcje</th>
              </tr>
            </thead>
            <tbody>
              {generators.map((gen) => (
                <tr key={gen.id}>
                  <td>{gen.name}</td>
                  <td>
                    <span style={{
                      padding: '4px 8px',
                      borderRadius: '4px',
                      background: gen.type === 'analysis' ? '#17a2b8' : '#007bff',
                      color: 'white',
                      fontSize: '12px'
                    }}>
                      {gen.type === 'analysis' ? 'Analiza' : 'Raport'}
                    </span>
                  </td>
                  <td>
                    {gen.periods && gen.periods.length > 0 && (
                      <span>
                        {gen.periods[0].years > 0 && `${gen.periods[0].years}r `}
                        {gen.periods[0].months > 0 && `${gen.periods[0].months}m `}
                        {gen.periods[0].days > 0 && `${gen.periods[0].days}d`}
                      </span>
                    )}
                  </td>
                  <td>{gen.schemeCount}</td>
                  <td>
                    <button
                      className="btn-load"
                      onClick={() => handleDeleteGenerator(gen.serviceKey, gen.id)}
                      style={{ background: '#dc3545', color: 'white' }}
                    >
                      Usuń
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <p style={{ textAlign: 'center', color: '#666', padding: '20px' }}>
            Brak aktywnych generatorów. Dodaj pierwszy generator klikając przycisk powyżej.
          </p>
        )}
      </div>

      <div className="export-section" style={{ marginTop: '40px' }}>
        <h3>Eksportuj wybrany raport</h3>
        <div className="export-buttons">
          <button className="btn-export" onClick={() => handleExport('JSON')} disabled={!currentReportId}>Pobierz JSON</button>
          <button className="btn-export" onClick={() => handleExport('XML')} disabled={!currentReportId}>Pobierz XML</button>
        </div>
      </div>
    </div>
  );
}
