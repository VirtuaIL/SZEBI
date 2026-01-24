import { useState, useEffect } from 'react';
import './UserManagement.css';

import { getApiBaseUrl } from '../utils/api';
const API_URL = getApiBaseUrl();

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
  const [users, setUsers] = useState([]);

  // Pobierz listę użytkowników przy ładowaniu
  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    try {
      const response = await fetch(`${API_URL}/users`);
      if (response.ok) {
        const data = await response.json();
        setUsers(data);
      } else {
        console.error('Błąd pobierania użytkowników:', response.status);
      }
    } catch (error) {
      console.error('Błąd połączenia:', error);
    }
  };

  const handleAddUser = async (e) => {
    e.preventDefault();
    try {
      // Mapowanie roli tekstowej na ID (Backend oczekuje int rolaId)
      // 1: Admin, 2: Inżynier, 3: Najemca (wg skryptu SQL/logiki backendu)
      let rolaId = 3;
      if (formData.role === 'admin') rolaId = 1;
      else if (formData.role === 'engineer') rolaId = 2;

      const payload = {
        email: formData.email,
        password: formData.password,
        imie: formData.firstName,
        nazwisko: formData.lastName,
        telefon: formData.phone,
        rolaId: rolaId
      };

      const response = await fetch(`${API_URL}/users`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload)
      });

      const data = await response.json();

      if (response.ok) {
        alert('Użytkownik został pomyślnie dodany!');
        setShowAddForm(false);
        setFormData({ email: '', password: '', firstName: '', lastName: '', phone: '', role: 'user' });
      } else {
        alert(`Błąd: ${data.error || 'Nie udało się dodać użytkownika'}`);
      }
    } catch (error) {
      console.error('Błąd dodawania użytkownika:', error);
      alert('Wystąpił błąd połączenia z serwerem.');
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
        {users.length === 0 ? (
          <p className="no-users">Brak użytkowników</p>
        ) : (
          <table className="users-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Imię i Nazwisko</th>
                <th>Email</th>
                <th>Telefon</th>
                <th>Rola</th>
              </tr>
            </thead>
            <tbody>
              {users.map(user => (
                <tr key={user.id}>
                  <td>{user.id}</td>
                  <td>{user.imie} {user.nazwisko}</td>
                  <td>{user.email}</td>
                  <td>{user.telefon || '-'}</td>
                  <td>
                    <span className={`role-badge role-${user.role}`}>
                      {user.role === 'admin' ? 'Administrator' :
                        user.role === 'engineer' ? 'Inżynier' : 'Najemca'}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}


