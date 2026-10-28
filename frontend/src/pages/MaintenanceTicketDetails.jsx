import { useCallback, useEffect, useState } from 'react';
import { ArrowLeft, Bot, CheckCircle2, Save, Wrench } from 'lucide-react';
import { Link, useParams } from 'react-router-dom';
import { maintenanceApi } from '../api/maintenanceApi';
import ErrorMessage from '../components/ErrorMessage';
import LoadingState from '../components/LoadingState';
import { useAuth } from '../context/AuthContext';

const statuses = [
  { value: 'OPEN', label: 'Open' },
  { value: 'IN_PROGRESS', label: 'In progress' },
  { value: 'RESOLVED', label: 'Resolved' },
];
const teams = [
  { value: 'NETWORK', label: 'Network' },
  { value: 'ELECTRICAL', label: 'Electrical' },
  { value: 'IT_SUPPORT', label: 'IT Support' },
  { value: 'HOUSEKEEPING', label: 'Housekeeping' },
];

const formatDate = (value) => {
  if (!value) return 'Date unavailable';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
};

const AI_CATEGORY_LABEL = {
  NETWORK: 'Network',
  ELECTRICAL: 'Electrical',
  IT_SUPPORT: 'IT Support',
  HOUSEKEEPING: 'Housekeeping',
};

export default function MaintenanceTicketDetails() {
  const { ticketId } = useParams();
  const { role } = useAuth();
  const isMaintenanceAdmin = role === 'MAINTENANCE_ADMIN';
  const isMaintenance = role === 'MAINTENANCE';
  const isStudent = role === 'STUDENT';

  const [ticket, setTicket] = useState(null);
  const [status, setStatus] = useState('OPEN');
  const [team, setTeam] = useState('');
  const [resolutionNotes, setResolutionNotes] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [classifying, setClassifying] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const loadTicket = useCallback(async () => {
    try {
      const data = isMaintenanceAdmin
        ? await maintenanceApi.getManagementTicket(ticketId)
        : await maintenanceApi.getTicket(ticketId);
      setTicket(data);
      setStatus(data.status);
      setTeam(data.team || '');
      setResolutionNotes(data.resolutionNotes || '');
      setError('');
    } catch (err) {
      setError(err.message || 'Unable to load this ticket.');
    } finally {
      setLoading(false);
    }
  }, [isMaintenanceAdmin, ticketId]);

  useEffect(() => {
    loadTicket();
  }, [loadTicket]);

  const saveStatus = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError('');
    setSuccess('');
    try {
      const updated = await maintenanceApi.updateTicketStatus(ticket.id, status, resolutionNotes.trim());
      setTicket(updated);
      setStatus(updated.status);
      setResolutionNotes(updated.resolutionNotes || '');
      setSuccess('Ticket status updated.');
    } catch (err) {
      setError(err.message || 'Unable to update ticket status.');
    } finally {
      setSaving(false);
    }
  };

  const saveTeam = async () => {
    if (!team || team === ticket.team) return;
    setSaving(true);
    setError('');
    setSuccess('');
    try {
      const updated = await maintenanceApi.assignTeam(ticket.id, team);
      setTicket(updated);
      setTeam(updated.team || '');
      setSuccess('Ticket team assignment updated.');
    } catch (err) {
      setError(err.message || 'Unable to update the assigned team.');
    } finally {
      setSaving(false);
    }
  };

  const handleClassify = async () => {
    setClassifying(true);
    setError('');
    setSuccess('');
    try {
      const updated = await maintenanceApi.classifyTicket(ticket.id);
      setTicket(updated);
      setStatus(updated.status);
      setTeam(updated.team || '');
      setSuccess('Groq AI classification completed successfully.');
    } catch (err) {
      setError(err.message || 'AI classification failed. Please try again.');
    } finally {
      setClassifying(false);
    }
  };

  if (loading) return <LoadingState message="Loading ticket details..." />;

  const backLink = isStudent ? '/student/feedback'
    : isMaintenanceAdmin ? '/maintenance-admin'
    : '/maintenance';
  const backLabel = isStudent ? 'My feedback & reports'
    : isMaintenanceAdmin ? 'Maintenance operations'
    : 'Team dashboard';

  const canEdit = isMaintenanceAdmin || isMaintenance;
  const isPendingClassification = ticket?.status === 'PENDING_CLASSIFICATION';

  return (
    <div className="page-container">
      <header className="page-header">
        <div>
          <p className="eyebrow">Maintenance ticket</p>
          <h1>Ticket details</h1>
          <p className="page-subtitle">
            {isStudent ? 'Track the status of your reported issue.' : 'Review the issue and update its status.'}
          </p>
        </div>
        <Link to={backLink} className="secondary maintenance-details-link">
          <ArrowLeft size={16} />
          {backLabel}
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
            <span className={`maintenance-ticket-status status-${ticket.status?.toLowerCase().replaceAll('_', '-')}`}>
              {ticket.status?.replaceAll('_', ' ')}
            </span>
          </div>

          <h2><Wrench size={20} /> Issue description</h2>
          <p className="maintenance-ticket-description">{ticket.description}</p>

          {/* AI Classification block */}
          {(ticket.aiCategory || isPendingClassification) && (
            <div className={`maintenance-ai-box ${isPendingClassification ? 'maintenance-pending' : ''}`}>
              <div className="maintenance-ai-box-title">
                <Bot size={16} />
                {isPendingClassification ? 'Pending AI Classification' : 'AI Classification'}
              </div>
              {ticket.aiCategory ? (
                <dl className="maintenance-ai-facts">
                  <div>
                    <dt>Category</dt>
                    <dd>
                      <span className="maintenance-ai-badge">
                        {AI_CATEGORY_LABEL[ticket.aiCategory] || ticket.aiCategory}
                      </span>
                    </dd>
                  </div>
                  {ticket.priority && (
                    <div>
                      <dt>AI Priority</dt>
                      <dd>
                        <span className={`maintenance-priority priority-${ticket.priority?.toLowerCase()}`}>
                          {ticket.priority}
                        </span>
                      </dd>
                    </div>
                  )}
                  {ticket.classificationReason && (
                    <div className="maintenance-ai-reason">
                      <dt>Reason</dt>
                      <dd>{ticket.classificationReason}</dd>
                    </div>
                  )}
                </dl>
              ) : (
                <p className="maintenance-ai-pending-msg">
                  This ticket could not be automatically classified. A Maintenance Admin can retry AI classification below.
                </p>
              )}
              {isMaintenanceAdmin && (
                <button
                  type="button"
                  className="ai-classify-btn"
                  onClick={handleClassify}
                  disabled={classifying || saving}
                >
                  <Bot size={14} />
                  {classifying ? 'Classifying...' : isPendingClassification ? 'Classify with Groq AI' : 'Re-classify with Groq AI'}
                </button>
              )}
            </div>
          )}

          {/* Ticket facts */}
          <dl className="maintenance-ticket-facts">
            <div><dt>Library floor</dt><dd>{ticket.floor || 'Not specified'}</dd></div>
            <div><dt>Assigned team</dt><dd>{ticket.team?.replaceAll('_', ' ') || 'Unassigned'}</dd></div>
            {ticket.assignedStaffId && (
              <div><dt>Assigned staff</dt><dd>{ticket.assignedStaffId}</dd></div>
            )}
            {ticket.feedbackId && (
              <div><dt>Linked feedback</dt><dd>{ticket.feedbackId}</dd></div>
            )}
            <div><dt>Submitted</dt><dd>{formatDate(ticket.submittedAt)}</dd></div>
            <div><dt>Ticket ID</dt><dd>{ticket.id}</dd></div>
          </dl>

          {/* Admin: team re-assignment */}
          {isMaintenanceAdmin && (
            <div className="maintenance-status-form">
              <label>
                Assigned team
                <select value={team} onChange={(event) => setTeam(event.target.value)}>
                  <option value="">Unassigned</option>
                  {teams.map((item) => (
                    <option key={item.value} value={item.value}>{item.label}</option>
                  ))}
                </select>
              </label>
              <button className="secondary" type="button" onClick={saveTeam} disabled={saving || !team || team === ticket.team}>
                <Save size={16} />
                {saving ? 'Saving...' : 'Save team'}
              </button>
            </div>
          )}

          {/* Staff or Admin: status update */}
          {canEdit && (
            <form className="maintenance-status-form" onSubmit={saveStatus}>
              <label>
                Update status
                <select value={status} onChange={(event) => setStatus(event.target.value)}>
                  {statuses.map((item) => (
                    <option key={item.value} value={item.value}>{item.label}</option>
                  ))}
                </select>
              </label>
              <label>
                Resolution notes
                <textarea
                  value={resolutionNotes}
                  onChange={(event) => setResolutionNotes(event.target.value)}
                  maxLength={2000}
                  rows={3}
                  placeholder="Add notes about the work completed or next steps."
                />
              </label>
              <button
                className="primary-button"
                type="submit"
                disabled={saving || (status === ticket.status && resolutionNotes === (ticket.resolutionNotes || ''))}
              >
                {status === 'RESOLVED' ? <CheckCircle2 size={16} /> : <Save size={16} />}
                {saving ? 'Saving...' : 'Save status'}
              </button>
            </form>
          )}

          {/* Student: resolution notes read-only */}
          {isStudent && ticket.resolutionNotes && (
            <div className="maintenance-ai-box">
              <div className="maintenance-ai-box-title">Resolution notes</div>
              <p style={{ margin: 0 }}>{ticket.resolutionNotes}</p>
            </div>
          )}
        </section>
      )}
    </div>
  );
}
