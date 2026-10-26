import { useCallback, useEffect, useState } from 'react';
import { AlertCircle, CheckCircle2, Clock3, RefreshCw, Wrench } from 'lucide-react';
import { Link } from 'react-router-dom';
import { maintenanceApi } from '../api/maintenanceApi';
import { useAuth } from '../context/AuthContext';
import ErrorMessage from '../components/ErrorMessage';
import LoadingState from '../components/LoadingState';

const formatDate = (value) => {
  if (!value) return 'Date unavailable';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
};

const label = (value) => value?.replaceAll('_', ' ') || 'Unknown';

export default function MaintenanceDashboard() {
  const { user } = useAuth();
  const [dashboard, setDashboard] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadDashboard = useCallback(async () => {
    try {
      const data = await maintenanceApi.getDashboard();
      setDashboard(data);
      setError('');
    } catch (err) {
      setError(err.message || 'Unable to load your team tickets.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadDashboard();
  }, [loadDashboard]);

  if (loading) return <LoadingState message="Loading your team dashboard..." />;

  const tickets = dashboard?.tickets || [];
  const summary = [
    { label: 'Open', count: dashboard?.openCount ?? 0, icon: AlertCircle, style: 'maintenance-open' },
    { label: 'In progress', count: dashboard?.inProgressCount ?? 0, icon: Clock3, style: 'maintenance-progress' },
    { label: 'Resolved', count: dashboard?.resolvedCount ?? 0, icon: CheckCircle2, style: 'maintenance-resolved' },
  ];

  return (
    <div className="page-container">
      <header className="page-header">
        <div>
          <p className="eyebrow">Maintenance team</p>
          <h1>{label(dashboard?.team)} dashboard</h1>
          <p className="page-subtitle">Tickets assigned to your team, ordered by latest submission.</p>
        </div>
        <button type="button" className="secondary refresh-button" onClick={loadDashboard}>
          <RefreshCw size={16} />
          Refresh
        </button>
      </header>

      <ErrorMessage message={error} />

      <div className="stats-grid maintenance-stats">
        {summary.map(({ label: title, count, icon: Icon, style }) => (
          <div className={`stat-card ${style}`} key={title}>
            <span><Icon size={16} /> {title}</span>
            <strong>{count}</strong>
          </div>
        ))}
      </div>

      <section className="panel-card">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Assigned work</p>
            <h2><Wrench size={20} /> Team tickets</h2>
          </div>
          <span className="summary-box">{tickets.length} tickets</span>
        </div>

        {!error && tickets.length === 0 ? (
          <div className="empty-state">There are no tickets assigned to your team right now.</div>
        ) : (
          <div className="maintenance-ticket-list">
            {tickets.map((ticket) => (
              <article className="maintenance-ticket-card" key={ticket.id}>
                <div className="maintenance-ticket-heading">
                  <span className={`maintenance-priority priority-${ticket.priority?.toLowerCase()}`}>
                    {ticket.priority || 'Unspecified'} priority
                  </span>
                  <span className={`maintenance-ticket-status status-${ticket.status?.toLowerCase()}`}>
                    {label(ticket.status)}
                  </span>
                </div>
                <p className="maintenance-ticket-description">{ticket.description}</p>
                <div className="maintenance-ticket-meta">
                  <span>Floor: <strong>{ticket.floor}</strong></span>
                  <span>Submitted: <strong>{formatDate(ticket.submittedAt)}</strong></span>
                </div>
                <Link className="secondary maintenance-details-link" to={`/maintenance/tickets/${encodeURIComponent(ticket.id)}`}>
                  View details
                </Link>
              </article>
            ))}
          </div>
        )}
      </section>
      <p className="maintenance-session-note">
        Signed in as {user?.name || user?.studentId}. Your team and ticket visibility come from your account.
      </p>
    </div>
  );
}