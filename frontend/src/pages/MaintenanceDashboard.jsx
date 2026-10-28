import { useCallback, useEffect, useState } from 'react';
import { AlertCircle, Bot, CheckCircle2, Clock3, RefreshCw, Send, Wrench } from 'lucide-react';
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
const priorities = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];

const AI_CATEGORY_LABEL = {
  NETWORK: 'Network',
  ELECTRICAL: 'Electrical',
  IT_SUPPORT: 'IT Support',
  HOUSEKEEPING: 'Housekeeping',
};

export default function MaintenanceDashboard() {
  const { user } = useAuth();
  const [dashboard, setDashboard] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [report, setReport] = useState({ description: '', floor: '', priority: 'MEDIUM' });

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

  const submitReport = async (event) => {
    event.preventDefault();
    setError('');
    setSuccess('');
    setSubmitting(true);
    try {
      const ticket = await maintenanceApi.reportIssue({
        description: report.description.trim(),
        floor: report.floor.trim(),
        priority: report.priority,
      });
      setReport({ description: '', floor: '', priority: 'MEDIUM' });
      setSuccess(`Issue submitted for triage as ticket ${ticket.id}.`);
    } catch (err) {
      setError(err.message || 'Unable to submit this maintenance issue.');
    } finally {
      setSubmitting(false);
    }
  };

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
      {success && <div className="success-box" role="status">{success}</div>}

      <div className="stats-grid maintenance-stats">
        {summary.map(({ label: title, count, icon: Icon, style }) => (
          <div className={`stat-card ${style}`} key={title}>
            <span><Icon size={16} /> {title}</span>
            <strong>{count}</strong>
          </div>
        ))}
      </div>

      <section className="panel-card maintenance-form-card">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Report an issue</p>
            <h2><Send size={20} /> Submit for Maintenance Admin triage</h2>
          </div>
        </div>
        <form className="stack-form" onSubmit={submitReport}>
          <div className="two-column-form">
            <label>
              Library floor / location
              <input
                value={report.floor}
                onChange={(event) => setReport((current) => ({ ...current, floor: event.target.value }))}
                maxLength={100}
                required
              />
            </label>
            <label>
              Priority
              <select
                value={report.priority}
                onChange={(event) => setReport((current) => ({ ...current, priority: event.target.value }))}
              >
                {priorities.map((priority) => <option key={priority} value={priority}>{priority}</option>)}
              </select>
            </label>
          </div>
          <label>
            Issue description
            <textarea
              value={report.description}
              onChange={(event) => setReport((current) => ({ ...current, description: event.target.value }))}
              maxLength={2000}
              rows={4}
              required
            />
            <span className="field-hint">{report.description.length} / 2000</span>
          </label>
          <button className="primary-button" type="submit" disabled={submitting}>
            <Send size={16} />
            {submitting ? 'Submitting...' : 'Submit issue'}
          </button>
        </form>
        <p className="maintenance-api-note">
          New reports are sent to Maintenance Admin for team assignment and appear in your team queue after assignment.
        </p>
      </section>

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
                  <span className={`maintenance-ticket-status status-${ticket.status?.toLowerCase().replaceAll('_', '-')}`}>
                    {label(ticket.status)}
                  </span>
                </div>
                <p className="maintenance-ticket-description">{ticket.description}</p>

                {/* AI classification metadata */}
                {ticket.aiCategory && (
                  <div className="maintenance-ai-inline">
                    <Bot size={13} />
                    <span>AI category: </span>
                    <span className="maintenance-ai-badge">
                      {AI_CATEGORY_LABEL[ticket.aiCategory] || ticket.aiCategory}
                    </span>
                    {ticket.classificationReason && (
                      <span className="maintenance-ai-reason-inline">— {ticket.classificationReason}</span>
                    )}
                  </div>
                )}
                {ticket.assignedStaffId && (
                  <div className="maintenance-ticket-meta">
                    <span>Assigned staff: <strong>{ticket.assignedStaffId}</strong></span>
                  </div>
                )}

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
