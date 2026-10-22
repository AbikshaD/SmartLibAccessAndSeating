import { useEffect, useState } from 'react';
import { BookOpen, Clock, AlertTriangle, CheckCircle2, XCircle, Bell } from 'lucide-react';
import { borrowApi } from '../api/borrowApi';
import LoadingState from '../components/LoadingState';
import ErrorMessage from '../components/ErrorMessage';

const STATUS_STYLES = {
  PENDING:  { badge: 'warning', label: 'Pending — awaiting admin approval', icon: Clock },
  APPROVED: { badge: 'success', label: 'Approved — book ready for pickup', icon: CheckCircle2 },
  RETURNED: { badge: 'success', label: 'Returned', icon: CheckCircle2 },
  REJECTED: { badge: 'danger',  label: 'Rejected by admin', icon: XCircle },
  OVERDUE:  { badge: 'danger',  label: 'Overdue — please return immediately', icon: AlertTriangle },
};

const formatDate = (d) =>
  d ? new Date(d).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' }) : '—';

export default function StudentBorrows() {
  const [borrows, setBorrows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    borrowApi.getMyBorrows()
      .then(data => { if (active) setBorrows(data || []); })
      .catch(err => { if (active) setError(err.message || 'Failed to load borrow history.'); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);

  if (loading) return <LoadingState message="Loading your borrow history..." />;

  const active  = borrows.filter(b => b.status === 'APPROVED' || b.status === 'OVERDUE');
  const pending = borrows.filter(b => b.status === 'PENDING');
  const history = borrows.filter(b => b.status === 'RETURNED' || b.status === 'REJECTED');

  return (
    <div className="page-container">
      <header className="page-header">
        <div>
          <p className="eyebrow">My Library</p>
          <h1>My Borrowed Books</h1>
          <p className="page-subtitle">Track your borrow requests, due dates, and any outstanding fines.</p>
        </div>
      </header>

      <ErrorMessage message={error} />

      {/* Due-soon / overdue notifications */}
      {borrows.filter(b => b.dueSoonNotified && b.status === 'APPROVED').map(b => (
        <div key={b.id} className="student-announcement-banner alert-urgent" style={{ marginBottom: '1rem' }}>
          <div className="announcement-banner-inner">
            <div className="announcement-banner-icon-wrap"><Bell size={22} /></div>
            <div className="announcement-banner-body">
              <h2 className="announcement-headline" style={{ fontSize: '1rem' }}>
                Due Soon: <em>{b.bookTitle}</em>
              </h2>
              <p className="announcement-text">
                Your borrowed book is due on <strong>{formatDate(b.dueDate)}</strong>. Please return it to the library to avoid a fine.
              </p>
            </div>
          </div>
        </div>
      ))}

      {borrows.filter(b => b.status === 'OVERDUE').map(b => (
        <div key={b.id} className="student-announcement-banner alert-urgent" style={{ marginBottom: '1rem' }}>
          <div className="announcement-banner-inner">
            <div className="announcement-banner-icon-wrap"><AlertTriangle size={22} /></div>
            <div className="announcement-banner-body">
              <h2 className="announcement-headline" style={{ fontSize: '1rem' }}>
                Overdue: <em>{b.bookTitle}</em> — Fine: ₹{b.fineAmount?.toFixed(2)}
              </h2>
              <p className="announcement-text">
                This book was due on <strong>{formatDate(b.dueDate)}</strong>. A fine of <strong>₹10 per day</strong> is accumulating. Please return it immediately.
              </p>
            </div>
          </div>
        </div>
      ))}

      {/* Stats */}
      <div className="stats-grid">
        <div className="stat-card accent">
          <span><BookOpen size={15} /> Active Borrows</span>
          <strong>{active.length}</strong>
        </div>
        <div className="stat-card">
          <span><Clock size={15} /> Pending Requests</span>
          <strong>{pending.length}</strong>
        </div>
        <div className="stat-card">
          <span><AlertTriangle size={15} /> Outstanding Fine</span>
          <strong>₹{borrows.reduce((sum, b) => sum + (b.fineAmount || 0), 0).toFixed(2)}</strong>
        </div>
      </div>

      {/* Active borrows */}
      {active.length > 0 && (
        <section className="panel-card">
          <div className="section-heading">
            <div><p className="eyebrow">Currently borrowed</p><h2>Active Books ({active.length})</h2></div>
          </div>
          <div className="booking-list">
            {active.map(b => {
              const s = STATUS_STYLES[b.status] || STATUS_STYLES.APPROVED;
              const Icon = s.icon;
              return (
                <article key={b.id} className={`booking-item booking-item-${b.status === 'OVERDUE' ? 'expired' : 'active'}`}>
                  <div className="booking-seat-icon"><BookOpen size={20} /></div>
                  <div className="booking-details">
                    <div className="booking-title-row">
                      <h3>{b.bookTitle}</h3>
                      <span className={`badge badge-${b.status === 'OVERDUE' ? 'expired' : 'active'}`}>{s.label}</span>
                    </div>
                    <div className="booking-meta">
                      <span>by {b.bookAuthor}</span>
                      <span>Genre: {b.bookGenre}</span>
                      <span>Due: {formatDate(b.dueDate)}</span>
                    </div>
                    {b.fineAmount > 0 && (
                      <div className="booking-expiry-info booking-expiry-expired">
                        <AlertTriangle size={13} />
                        <span>Outstanding fine: ₹{b.fineAmount.toFixed(2)} (₹10/day)</span>
                      </div>
                    )}
                  </div>
                </article>
              );
            })}
          </div>
        </section>
      )}

      {/* Pending */}
      {pending.length > 0 && (
        <section className="panel-card">
          <div className="section-heading">
            <div><p className="eyebrow">Awaiting approval</p><h2>Pending Requests ({pending.length})</h2></div>
          </div>
          <div className="booking-list">
            {pending.map(b => (
              <article key={b.id} className="booking-item booking-item-upcoming">
                <div className="booking-seat-icon"><Clock size={20} /></div>
                <div className="booking-details">
                  <div className="booking-title-row">
                    <h3>{b.bookTitle}</h3>
                    <span className="badge badge-upcoming">Pending</span>
                  </div>
                  <div className="booking-meta">
                    <span>by {b.bookAuthor}</span>
                    <span>Genre: {b.bookGenre}</span>
                    <span>Requested: {formatDate(b.requestedAt)}</span>
                  </div>
                </div>
              </article>
            ))}
          </div>
        </section>
      )}

      {/* History */}
      {history.length > 0 && (
        <section className="panel-card">
          <div className="section-heading">
            <div><p className="eyebrow">Past requests</p><h2>History ({history.length})</h2></div>
          </div>
          <div className="booking-list">
            {history.map(b => {
              const s = STATUS_STYLES[b.status] || { badge: 'warning', label: b.status, icon: BookOpen };
              const Icon = s.icon;
              return (
                <article key={b.id} className={`booking-item booking-item-${b.status === 'RETURNED' ? 'expired' : 'cancelled'}`}>
                  <div className="booking-seat-icon"><Icon size={20} /></div>
                  <div className="booking-details">
                    <div className="booking-title-row">
                      <h3>{b.bookTitle}</h3>
                      <span className={`badge badge-${b.status === 'RETURNED' ? 'expired' : 'cancelled'}`}>{s.label}</span>
                    </div>
                    <div className="booking-meta">
                      <span>by {b.bookAuthor}</span>
                      {b.dueDate && <span>Due was: {formatDate(b.dueDate)}</span>}
                      {b.returnedAt && <span>Returned: {formatDate(b.returnedAt)}</span>}
                    </div>
                  </div>
                </article>
              );
            })}
          </div>
        </section>
      )}

      {borrows.length === 0 && (
        <div className="panel-card empty-state">
          <BookOpen size={44} className="empty-icon" />
          <h3>No borrow requests yet</h3>
          <p>Go to the Books section and click "Request to Borrow" on any book.</p>
        </div>
      )}
    </div>
  );
}
