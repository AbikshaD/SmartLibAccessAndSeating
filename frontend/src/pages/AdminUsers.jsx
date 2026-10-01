import { useEffect, useState } from 'react';
import { userApi } from '../api/userApi';
import ErrorMessage from '../components/ErrorMessage';
import LoadingState from '../components/LoadingState';

const emptyForm = {
  studentId: '',
  name: '',
  email: '',
  password: '',
  role: 'STUDENT',
};

export default function AdminUsers() {
  const [users, setUsers] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadUsers = async () => {
    try {
      const data = await userApi.getAllUsers();
      setUsers(data || []);
    } catch (err) {
      setError(err.message || 'Unable to load users.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadUsers();
  }, []);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  };

  const resetForm = () => {
    setForm(emptyForm);
    setEditingId(null);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');

    try {
      if (editingId) {
        await userApi.updateUser(editingId, form);
      } else if (form.role === 'ADMIN') {
        await userApi.createAdmin(form);
      } else {
        await userApi.createUser(form);
      }
      resetForm();
      await loadUsers();
    } catch (err) {
      setError(err.message || 'Unable to save user.');
    }
  };

  const handleEdit = (userItem) => {
    setEditingId(userItem.studentId);
    setForm({
      studentId: userItem.studentId,
      name: userItem.name,
      email: userItem.email,
      password: '',
      role: userItem.role || 'STUDENT',
    });
  };

  const handleDelete = async (studentId) => {
    const confirmed = window.confirm('Are you sure you want to delete this user?');
    if (!confirmed) return;

    try {
      await userApi.deleteUser(studentId);
      await loadUsers();
    } catch (err) {
      setError(err.message || 'Unable to delete user.');
    }
  };

  if (loading) return <LoadingState message="Loading users..." />;

  return (
    <div className="page-container">
      <header className="page-header">
        <div>
          <p className="eyebrow">User management</p>
          <h1>All Users</h1>
        </div>
      </header>

      <ErrorMessage message={error} />

      <section className="panel-card form-panel">
        <h2>{editingId ? 'Update user' : 'Create user'}</h2>
        <form className="stack-form" onSubmit={handleSubmit}>
          <div className="two-column-form">
            <label>
              Student ID
              <input name="studentId" value={form.studentId} onChange={handleChange} required />
            </label>
            <label>
              Name
              <input name="name" value={form.name} onChange={handleChange} required />
            </label>
            <label>
              Email
              <input name="email" type="email" value={form.email} onChange={handleChange} required />
            </label>
            <label>
              Role
              <select name="role" value={form.role} onChange={handleChange}>
                <option value="STUDENT">STUDENT</option>
                <option value="ADMIN">ADMIN</option>
              </select>
            </label>
          </div>

          <label>
            Password
            <input
              name="password"
              type="password"
              value={form.password}
              onChange={handleChange}
              placeholder={editingId ? 'Leave blank to keep current password' : 'Enter password'}
            />
          </label>

          <div className="card-actions">
            <button type="submit" className="primary-button">
              {editingId ? 'Save changes' : 'Create user'}
            </button>
            {editingId && (
              <button type="button" className="secondary" onClick={resetForm}>
                Cancel
              </button>
            )}
          </div>
        </form>
      </section>

      <section className="panel-card table-panel">
        <table>
          <thead>
            <tr>
              <th>Student ID</th>
              <th>Name</th>
              <th>Email</th>
              <th>Role</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {users.map((userItem) => (
              <tr key={userItem.id || userItem.studentId}>
                <td>{userItem.studentId}</td>
                <td>{userItem.name}</td>
                <td>{userItem.email}</td>
                <td>{userItem.role}</td>
                <td>
                  <div className="inline-actions">
                    <button type="button" className="secondary" onClick={() => handleEdit(userItem)}>
                      Edit
                    </button>
                    <button type="button" className="danger" onClick={() => handleDelete(userItem.studentId)}>
                      Delete
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  );
}
