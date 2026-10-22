import { useEffect, useState } from 'react';
import { BookOpen, CheckCircle2, XCircle, RotateCcw, Clock, AlertTriangle, RefreshCw } from 'lucide-react';
import { borrowApi } from '../api/borrowApi';
import LoadingState from '../components/LoadingState';
import ErrorMessage from '../components/ErrorMessage';

const STATUS_STYLES = {
  PENDING:  { badge: 'warning',  label: 'Pending'  },
  APPROVED: { badge: 'success',  label: 'Approved' },
  RETURNED: { badge: 'success',  label: 'Returned' },
  REJECTED: { badge: 'danger',   label: 'Rejected' },
  OVERDUE:  { badge: 'danger',   label: 'Overdue'  },
};

const formatDate = (d) => d ? new Date(d).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' }) : '—';

export default function AdminBorrows() {
  const [borrows, setBorrows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [actionId, setActionId] = useState('');

  const loadBorrows = async () => {
    try {
      const data = await borrowApi.getAllBorrows();
      setBorrows(data || []);
    } catch (err) {
      setError(err.message || 'Unable to load borrow requests.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadBorrows(); }, []);

  const doAction = async (id, action, label) => {
    setError(''); setSuccess(''); setActionId(id);
    try {
      await action(id);
      setSuccess(`Borrow request ${label} successfully.`);
      await loadBorrows();
    } catch (err) {
      setError(err.message || `Failed to ${label} borrow.`);
    } finally {
      setActionId('');
    }
  };

  if (loading) return <LoadingState message="Loading borrow requests..." />;

  const pending  = borrows.filter(b => b.status === 'PENDING');
  const active   = borrows.filter(b => b.status === 'APPROVED' || b.status === 'OVERDUE');
  const history  = borrows.filter(b => b.status === 'RETURNED' || b.status === 'REJECTED');

  return (
    <div className="page-container">
      <header className="page-header">
        <div>
          <p className="eyebrow">Admin · Book Borrowing</p>
          <h1>Borrow Requests</h1>
          <p className="page-subtitle">Review student borrow requests, approve, and track returns.</p>
        </div>
        <button type="button" className="secondary" onClick={loadBorrows} style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <RefreshCw size={15} /> Refresh
        </button>
      </header>

      <ErrorMessage message={error} />
      {success && <div className="success-message"><div className="success-icon">✓</div><div>{success}</div></div>}

      {/* Stats */}
      <div className="stats-grid">
        <div className="stat-card">
          <span><BookOpen size={15} /> Total Requests</span>
          <strong>{borrows.length}</strong>
        </div>
        <div className="stat-card accent">
          <span><Clock size={15} /> Pending</span>
          <strong>{pending.length}</strong>
        </div>
        <div className="stat-card">
          <span><CheckCircle2 size={15} /> Active / Approved</span>
          <strong>{active.length}</strong>
        </div>
        <div className="stat-card">
          <span><AlertTriangle size={15} /> Overdue</span>
          <strong>{borrows.filter(b => b.status === 'OVERDUE').length}</strong>
        </div>
      </div>

      {/* Pending Requests */}
      <section className="panel-card">
        <div className="section-heading">
          <div><p className="eyebrow">Action required</p><h2>Pending Requests ({pending.length})</h2></div>
        </div>
        {pending.length === 0 ? (
          <div className="empty-state">No pending borrow requests.</div>
        ) : (
          <div className="borrow-table-wrap">
            <table className="borrow-table">
              <thead>
                <tr>
                  <th>Student ID</th>
                  <th>Name</th>
                  <th>Book</th>
                  <th>Author</th>
                  <th>Genre</th>
                  <th>Requested</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {pending.map(b => (
                  <tr key={b.id}>
                    <td><strong>{b.studentId}</strong></td>
                    <td>{b.studentName}</td>
                    <td>{b.bookTitle}</td>
                    <td>{b.bookAuthor}</td>
                    <td>{b.bookGenre}</td>
                    <td>{formatDate(b.requestedAt)}</td>
                    <td>
                      <div style={{ display: 'flex', gap: 8 }}>
                        <button
                          type="button"
                          className="primary-button"
                          disabled={actionId === b.id}
                          onClick={() => doAction(b.id, borrowApi.approveBorrow, 'approved')}
                          style={{ fontSize: '0.8rem', padding: '4px 10px' }}
                        >
                          <CheckCircle2 size={13} /> Approve
                        </button>
                        <button
                          type="button"
                          className="danger"
                          disabled={actionId === b.id}
                          onClick={() => doAction(b.id, borrowApi.rejectBorrow, 'rejected')}
                          style={{ fontSize: '0.8rem', padding: '4px 10px' }}
                        >
                          <XCircle size={13} /> Reject
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {/* Active / Approved / Overdue */}
      <section className="panel-card">
        <div className="section-heading">
          <div><p className="eyebrow">Currently borrowed</p><h2>Active Borrows ({active.length})</h2></div>
        </div>
        {active.length === 0 ? (
          <div className="empty-state">No active borrows.</div>
        ) : (
          <div className="borrow-table-wrap">
            <table className="borrow-table">
              <thead>
                <tr>
                  <th>Student ID</th>
                  <th>Name</th>
                  <th>Book</th>
                  <th>Due Date</th>
                  <th>Status</th>
                  <th>Fine</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {active.map(b => {
                  const s = STATUS_STYLES[b.status] || STATUS_STYLES.APPROVED;
                  return (
                    <tr key={b.id}>
                      <td><strong>{b.studentId}</strong></td>
                      <td>{b.studentName}</td>
                      <td>{b.bookTitle}</td>
                      <td>{formatDate(b.dueDate)}</td>
                      <td><span className={`badge ${s.badge}`}>{s.label}</span></td>
                      <td>
                        {b.fineAmount > 0
                          ? <span style={{ color: '#dc2626', fontWeight: 700 }}>₹{b.fineAmount.toFixed(2)}</span>
                          : '—'}
                      </td>
                      <td>
                        <button
                          type="button"
                          className="secondary"
                          disabled={actionId === b.id}
                          onClick={() => doAction(b.id, borrowApi.markReturned, 'returned')}
                          style={{ fontSize: '0.8rem', padding: '4px 10px', display: 'flex', alignItems: 'center', gap: 4 }}
                        >
                          <RotateCcw size={13} /> Mark Returned
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {/* History */}
      {history.length > 0 && (
        <section className="panel-card">
          <div className="section-heading">
            <div><p className="eyebrow">Completed</p><h2>History ({history.length})</h2></div>
          </div>
          <div className="borrow-table-wrap">
            <table className="borrow-table">
              <thead>
                <tr>
                  <th>Student ID</th>
                  <th>Name</th>
                  <th>Book</th>
                  <th>Due Date</th>
                  <th>Returned</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {history.map(b => {
                  const s = STATUS_STYLES[b.status] || { badge: 'warning', label: b.status };
                  return (
                    <tr key={b.id}>
                      <td><strong>{b.studentId}</strong></td>
                      <td>{b.studentName}</td>
                      <td>{b.bookTitle}</td>
                      <td>{formatDate(b.dueDate)}</td>
                      <td>{formatDate(b.returnedAt)}</td>
                      <td><span className={`badge ${s.badge}`}>{s.label}</span></td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </section>
      )}
    </div>
  );
}
