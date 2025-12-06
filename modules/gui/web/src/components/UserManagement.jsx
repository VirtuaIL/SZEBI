import { useState } from 'react';
import './UserManagement.css';

const API_URL = 'http://localhost:8080/api';

export default function UserManagement({ userRole }) {
  const [showAddForm, setShowAddForm] = useState(false);
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    firstName: '',
    lastName: '',
    phone: '',
    role: 'user'
  });

  const handleAddUser = async (e) => {
    e.preventDefault();
    try {
      // TODO: Implementować endpoint API
      console.log('Dodawanie użytkownika:', formData);
      alert('Użytkownik został dodany (funkcja do implementacji)');
      setShowAddForm(false);
      setFormData({ email: '', password: '', firstName: '', lastName: '', phone: '', role: 'user' });
    } catch (error) {
      console.error('Błąd dodawania użytkownika:', error);
      alert('Błąd podczas dodawania użytkownika');
    }
  };

  return (
    <div className="user-management">
      <div className="user-management-header">
        <h2>Zarządzanie Użytkownikami</h2>
        <button 
          className="btn-add-user"
          onClick={() => setShowAddForm(!showAddForm)}
        >
          + Dodaj Nowego Użytkownika
        </button>
      </div>

      {showAddForm && (
        <div className="add-user-form">
          <h3>Dodaj Nowego Użytkownika</h3>
          <form onSubmit={handleAddUser}>
            <div className="form-group">
              <label>Email:</label>
              <input
                type="email"
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                required
              />
            </div>
            <div className="form-group">
              <label>Hasło:</label>
              <input
                type="password"
                value={formData.password}
                onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                required
              />
            </div>
            <div className="form-group">
              <label>Imię:</label>
              <input
                type="text"
                value={formData.firstName}
                onChange={(e) => setFormData({ ...formData, firstName: e.target.value })}
                required
              />
            </div>
            <div className="form-group">
              <label>Nazwisko:</label>
              <input
                type="text"
                value={formData.lastName}
                onChange={(e) => setFormData({ ...formData, lastName: e.target.value })}
                required
              />
            </div>
            <div className="form-group">
              <label>Telefon:</label>
              <input
                type="tel"
                value={formData.phone}
                onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
              />
            </div>
            <div className="form-group">
              <label>Rola:</label>
              <select
                value={formData.role}
                onChange={(e) => setFormData({ ...formData, role: e.target.value })}
                required
              >
                <option value="user">Najemca</option>
                <option value="engineer">Inżynier</option>
                <option value="admin">Administrator</option>
              </select>
            </div>
            <div className="form-actions">
              <button type="submit" className="btn-submit">Dodaj Użytkownika</button>
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

      <div className="users-list">
        <h3>Lista Użytkowników</h3>
        <p className="info-text">Lista użytkowników - do implementacji (endpoint API)</p>
      </div>
    </div>
  );
}


