import { useState, useEffect } from 'react';
import './UserManagement.css';

import { getApiBaseUrl } from '../utils/api';
const API_URL = getApiBaseUrl();

export default function UserManagement({ userRole }) {
  const [showAddForm, setShowAddForm] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    firstName: '',
    lastName: '',
    phone: '',
    role: 'user'
  });
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Pobierz listę użytkowników przy ładowaniu
  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetch(`${API_URL}/users`);
      if (response.ok) {
        const data = await response.json();
        setUsers(data);
      } else {
        const errorData = await response.json().catch(() => ({}));
        setError(`Błąd pobierania użytkowników: ${errorData.error || response.status}`);
        console.error('Błąd pobierania użytkowników:', response.status);
      }
    } catch (error) {
      setError('Błąd połączenia z serwerem. Upewnij się, że backend jest uruchomiony.');
      console.error('Błąd połączenia:', error);
    } finally {
      setLoading(false);
    }
  };

  const mapRoleToId = (role) => {
    // 1: Admin, 2: Inżynier, 3: Najemca (wg skryptu SQL/logiki backendu)
    if (role === 'admin') return 1;
    if (role === 'engineer') return 2;
    return 3;
  };

  const mapIdToRole = (rolaId) => {
    if (rolaId === 1) return 'admin';
    if (rolaId === 2) return 'engineer';
    return 'user';
  };

  const resetForm = () => {
    setFormData({ email: '', password: '', firstName: '', lastName: '', phone: '', role: 'user' });
    setEditingUser(null);
    setShowAddForm(false);
  };

  const handleAddUser = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const rolaId = mapRoleToId(formData.role);

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
        resetForm();
        await fetchUsers(); // Odśwież listę
      } else {
        setError(data.error || 'Nie udało się dodać użytkownika');
      }
    } catch (error) {
      console.error('Błąd dodawania użytkownika:', error);
      setError('Wystąpił błąd połączenia z serwerem.');
    } finally {
      setLoading(false);
    }
  };

  const handleEditUser = (user) => {
    setEditingUser(user);
    setFormData({
      email: user.email,
      password: '', // Nie pokazuj hasła
      firstName: user.imie,
      lastName: user.nazwisko,
      phone: user.telefon || '',
      role: user.role || 'user' // user.role już jest stringiem ('admin', 'engineer', 'user')
    });
    setShowAddForm(true);
  };

  const handleUpdateUser = async (e) => {
    e.preventDefault();
    if (!editingUser) return;

    setLoading(true);
    setError(null);
    try {
      const rolaId = mapRoleToId(formData.role);

      const payload = {
        imie: formData.firstName,
        nazwisko: formData.lastName,
        email: formData.email,
        telefon: formData.phone,
        rolaId: rolaId
      };

      // Dodaj hasło tylko jeśli zostało podane
      if (formData.password && formData.password.trim() !== '') {
        payload.password = formData.password;
      }

      const response = await fetch(`${API_URL}/users/${editingUser.id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload)
      });

      const data = await response.json();

      if (response.ok) {
        alert('Użytkownik został pomyślnie zaktualizowany!');
        resetForm();
        await fetchUsers(); // Odśwież listę
      } else {
        setError(data.error || 'Nie udało się zaktualizować użytkownika');
      }
    } catch (error) {
      console.error('Błąd aktualizacji użytkownika:', error);
      setError('Wystąpił błąd połączenia z serwerem.');
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteUser = async (userId) => {
    if (!window.confirm('Czy na pewno chcesz usunąć tego użytkownika? Ta operacja jest nieodwracalna.')) {
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const response = await fetch(`${API_URL}/users/${userId}`, {
        method: 'DELETE',
        headers: {
          'Content-Type': 'application/json',
        }
      });

      const data = await response.json();

      if (response.ok) {
        alert('Użytkownik został pomyślnie usunięty!');
        await fetchUsers(); // Odśwież listę
      } else {
        setError(data.error || 'Nie udało się usunąć użytkownika');
      }
    } catch (error) {
      console.error('Błąd usuwania użytkownika:', error);
      setError('Wystąpił błąd połączenia z serwerem.');
    } finally {
      setLoading(false);
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

      {error && (
        <div className="error-message" style={{ 
          background: '#f8d7da', 
          color: '#721c24', 
          padding: '10px', 
          borderRadius: '4px', 
          marginBottom: '20px' 
        }}>
          {error}
        </div>
      )}

      {showAddForm && (
        <div className="add-user-form">
          <h3>{editingUser ? 'Edytuj Użytkownika' : 'Dodaj Nowego Użytkownika'}</h3>
          <form onSubmit={editingUser ? handleUpdateUser : handleAddUser}>
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
                required={!editingUser}
                placeholder={editingUser ? 'Zostaw puste, aby nie zmieniać hasła' : ''}
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
              <button 
                type="submit" 
                className="btn-submit"
                disabled={loading}
              >
                {loading ? 'Zapisywanie...' : (editingUser ? 'Zaktualizuj Użytkownika' : 'Dodaj Użytkownika')}
              </button>
              <button
                type="button"
                className="btn-cancel"
                onClick={resetForm}
                disabled={loading}
              >
                Anuluj
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="users-list">
        <h3>Lista Użytkowników</h3>
        {loading && !users.length ? (
          <p className="info-text">Ładowanie użytkowników...</p>
        ) : users.length === 0 ? (
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
                <th>Akcje</th>
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
                  <td>
                    <div style={{ display: 'flex', gap: '8px' }}>
                      <button
                        className="btn-edit"
                        onClick={() => handleEditUser(user)}
                        disabled={loading}
                        style={{
                          padding: '5px 10px',
                          background: '#28a745',
                          color: 'white',
                          border: 'none',
                          borderRadius: '4px',
                          cursor: loading ? 'not-allowed' : 'pointer',
                          fontSize: '12px'
                        }}
                      >
                        Edytuj
                      </button>
                      <button
                        className="btn-delete"
                        onClick={() => handleDeleteUser(user.id)}
                        disabled={loading}
                        style={{
                          padding: '5px 10px',
                          background: '#dc3545',
                          color: 'white',
                          border: 'none',
                          borderRadius: '4px',
                          cursor: loading ? 'not-allowed' : 'pointer',
                          fontSize: '12px'
                        }}
                      >
                        Usuń
                      </button>
                    </div>
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


