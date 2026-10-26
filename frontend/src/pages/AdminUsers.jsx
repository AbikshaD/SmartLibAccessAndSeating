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
  const [submitting, setSubmitting] = useState(false);
  const [busyId, setBusyId] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const loadUsers = async () => {
    try {
      const data = await userApi.getAllUsers();
      setUsers((data || []).filter((userItem) => userItem.role !== 'MAINTENANCE'));
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
    setSuccess('');
    setSubmitting(true);

    try {
      if (editingId) {
        await userApi.updateUser(editingId, form);
      } else if (form.role === 'ADMIN') {
        await userApi.createAdmin(form);
      } else if (form.role === 'MAINTENANCE_ADMIN') {
        await userApi.createMaintenanceAdmin(form);
      } else {
        await userApi.createUser(form);
      }
      resetForm();
      await loadUsers();
      setSuccess(editingId ? 'Account details updated.' : 'Account created.');
    } catch (err) {
      setError(err.message || 'Unable to save user.');
    } finally {
      setSubmitting(false);
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

  const handleActiveChange = async (userItem) => {
    setError('');
    setSuccess('');
    setBusyId(userItem.studentId);
    try {
      const updated = await userApi.setActive(userItem.studentId, !userItem.active);
      setUsers((current) => current.map((entry) => (
        entry.studentId === updated.studentId ? updated : entry
      )));
      setSuccess(`${userItem.name}'s account is now ${updated.active ? 'active' : 'deactivated'}.`);
    } catch (err) {
      setError(err.message || 'Unable to update account status.');
    } finally {
      setBusyId('');
    }
  };

  const handleUnblock = async (studentId) => {
    setError('');
    try {
      await userApi.unblockUser(studentId);
      await loadUsers();
    } catch (err) {
      setError(err.message || 'Unable to enable/unblock user.');
    }
  };

  if (loading) return <LoadingState message="Loading users..." />;

  return (
    <div className="page-container">
      <header className="page-header">
        <div>
          <p className="eyebrow">User management</p>
          <h1>System accounts</h1>
        </div>
      </header>

      <ErrorMessage message={error} />
      {success && <div className="success-box" role="status">{success}</div>}

      <section className="panel-card form-panel">
        <h2>{editingId ? 'Update user' : 'Create user'}</h2>
        <form className="stack-form" onSubmit={handleSubmit}>
          <div className="two-column-form">
            <label>
              Account ID
              <input name="studentId" value={form.studentId} onChange={handleChange} maxLength={100} required />
            </label>
            <label>
              Name
              <input name="name" value={form.name} onChange={handleChange} maxLength={100} required />
            </label>
            <label>
              Email
              <input name="email" type="email" value={form.email} onChange={handleChange} maxLength={254} required />
            </label>
            <label>
              Role
              <select name="role" value={form.role} onChange={handleChange} disabled={Boolean(editingId)}>
                <option value="STUDENT">STUDENT</option>
                <option value="ADMIN">ADMIN</option>
                <option value="MAINTENANCE_ADMIN">MAINTENANCE_ADMIN</option>
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
              minLength={editingId ? undefined : 8}
              maxLength={100}
              required={!editingId}
              placeholder={editingId ? 'Leave blank to keep current password' : 'Enter password'}
            />
          </label>

          <div className="card-actions">
            <button type="submit" className="primary-button" disabled={submitting}>
              {submitting ? 'Saving...' : editingId ? 'Save changes' : 'Create user'}
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
              <th>Account ID</th>
              <th>Name</th>
              <th>Email</th>
              <th>Role</th>
              <th>Account status</th>
              <th>Cancellations</th>
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
                  {userItem.role === 'ADMIN' || userItem.role === 'MAINTENANCE_ADMIN' ? (
                    <span className={`maintenance-account-state ${userItem.active === false ? 'is-inactive' : 'is-active'}`}>
                      {userItem.active === false ? 'Deactivated' : 'Active'}
                    </span>
                  ) : userItem.blocked ? (
                    <span title={userItem.blockedReason || 'Blocked by AI Anomaly Detection'} style={{ background: '#fef2f2', color: '#dc2626', border: '1px solid #fca5a5', padding: '0.2rem 0.55rem', borderRadius: '4px', fontSize: '0.825rem', fontWeight: 600, display: 'inline-block' }}>
                      Booking blocked
                    </span>
                  ) : (
                    <span style={{ background: '#f0fdf4', color: '#16a34a', border: '1px solid #86efac', padding: '0.2rem 0.55rem', borderRadius: '4px', fontSize: '0.825rem', fontWeight: 600, display: 'inline-block' }}>
                      Active
                    </span>
                  )}
                </td>
                <td>{userItem.role === 'STUDENT' ? `${userItem.bookingCancellationCount || 0} / 4` : '—'}</td>
                <td>
                  <div className="inline-actions">
                    {(userItem.role === 'ADMIN' || userItem.role === 'MAINTENANCE_ADMIN') && (
                      <button
                        type="button"
                        className={userItem.active === false ? 'primary-button' : 'secondary'}
                        onClick={() => handleActiveChange(userItem)}
                        disabled={busyId === userItem.studentId}
                      >
                        {busyId === userItem.studentId ? 'Saving...' : userItem.active === false ? 'Activate' : 'Deactivate'}
                      </button>
                    )}
                    {userItem.blocked && (
                      <button type="button" className="primary-button" style={{ backgroundColor: '#16a34a', color: 'white', padding: '0.35rem 0.75rem', fontSize: '0.85rem' }} onClick={() => handleUnblock(userItem.studentId)}>
                        Enable
                      </button>
                    )}
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
