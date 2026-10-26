import { useCallback, useEffect, useState } from 'react';
import { ArrowLeft, CheckCircle2, Save, Wrench } from 'lucide-react';
import { Link, useParams } from 'react-router-dom';
import { maintenanceApi } from '../api/maintenanceApi';
import ErrorMessage from '../components/ErrorMessage';
import LoadingState from '../components/LoadingState';

const statuses = [
  { value: 'OPEN', label: 'Open' },
  { value: 'IN_PROGRESS', label: 'In progress' },
  { value: 'RESOLVED', label: 'Resolved' },
];

const formatDate = (value) => {
  if (!value) return 'Date unavailable';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
};

export default function MaintenanceTicketDetails() {
  const { ticketId } = useParams();
  const [ticket, setTicket] = useState(null);
  const [status, setStatus] = useState('OPEN');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const loadTicket = useCallback(async () => {
    try {
      const data = await maintenanceApi.getTicket(ticketId);
      setTicket(data);
      setStatus(data.status);
      setError('');
    } catch (err) {
      setError(err.message || 'Unable to load this ticket.');
    } finally {
      setLoading(false);
    }
  }, [ticketId]);

  useEffect(() => {
    loadTicket();
  }, [loadTicket]);

  const saveStatus = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError('');
    setSuccess('');
    try {
      const updated = await maintenanceApi.updateTicketStatus(ticket.id, status);
      setTicket(updated);
      setStatus(updated.status);
      setSuccess('Ticket status updated.');
    } catch (err) {
      setError(err.message || 'Unable to update ticket status.');
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <LoadingState message="Loading ticket details..." />;

  return (
    <div className="page-container">
      <header className="page-header">
        <div>
          <p className="eyebrow">Maintenance ticket</p>
          <h1>Ticket details</h1>
          <p className="page-subtitle">Review the issue and update its status.</p>
        </div>
        <Link to="/maintenance" className="secondary maintenance-details-link">
          <ArrowLeft size={16} />
          Team dashboard
        </Link>
      </header>

      <ErrorMessage message={error} />
      {success && <div className="success-box" role="status">{success}</div>}

      {!ticket ? (
        !error && <div className="empty-state">Ticket details are unavailable.</div>
      ) : (
        <section className="panel-card maintenance-ticket-detail">
          <div className="maintenance-ticket-heading">
            <span className={`maintenance-priority priority-${ticket.priority?.toLowerCase()}`}>
              {ticket.priority || 'Unspecified'} priority
            </span>
            <span className={`maintenance-ticket-status status-${ticket.status?.toLowerCase()}`}>
              {ticket.status?.replaceAll('_', ' ')}
            </span>
          </div>
          <h2><Wrench size={20} /> Issue description</h2>
          <p className="maintenance-ticket-description">{ticket.description}</p>

          <dl className="maintenance-ticket-facts">
            <div><dt>Library floor</dt><dd>{ticket.floor}</dd></div>
            <div><dt>Assigned team</dt><dd>{ticket.team?.replaceAll('_', ' ')}</dd></div>
            <div><dt>Submitted</dt><dd>{formatDate(ticket.submittedAt)}</dd></div>
            <div><dt>Ticket ID</dt><dd>{ticket.id}</dd></div>
          </dl>

          <form className="maintenance-status-form" onSubmit={saveStatus}>
            <label>
              Update status
              <select value={status} onChange={(event) => setStatus(event.target.value)}>
                {statuses.map((item) => (
                  <option key={item.value} value={item.value}>{item.label}</option>
                ))}
              </select>
            </label>
            <button className="primary-button" type="submit" disabled={saving || status === ticket.status}>
              {status === 'RESOLVED' ? <CheckCircle2 size={16} /> : <Save size={16} />}
              {saving ? 'Saving...' : 'Save status'}
            </button>
          </form>
          <p className="maintenance-api-note">
            Resolution notes are not supported by the current ticket API, so this page only saves the ticket status.
          </p>
        </section>
      )}
    </div>
  );
}
