import { useEffect, useState } from 'react';
import { ClipboardPlus, RefreshCw, UserPlus, Users, Wrench } from 'lucide-react';
import { Link } from 'react-router-dom';
import ErrorMessage from '../components/ErrorMessage';
import LoadingState from '../components/LoadingState';
import { maintenanceApi } from '../api/maintenanceApi';
import { maintenanceUserApi } from '../api/maintenanceUserApi';

const teams = [
  { value: 'NETWORK', label: 'Network' },
  { value: 'ELECTRICAL', label: 'Electrical' },
  { value: 'IT_SUPPORT', label: 'IT Support' },
  { value: 'HOUSEKEEPING', label: 'Housekeeping' },
];
const priorities = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];

const initialStaffForm = {
  studentId: '',
  name: '',
  email: '',
  password: '',
  team: 'NETWORK',
};

const initialTicketForm = {
  team: 'NETWORK',
  description: '',
  floor: '',
  priority: 'MEDIUM',
};

export default function AdminMaintenance() {
  const [staff, setStaff] = useState([]);
  const [tickets, setTickets] = useState([]);
  const [staffForm, setStaffForm] = useState(initialStaffForm);
  const [ticketForm, setTicketForm] = useState(initialTicketForm);
  const [loadingTickets, setLoadingTickets] = useState(true);
  const [loading, setLoading] = useState(true);
  const [staffSubmitting, setStaffSubmitting] = useState(false);
  const [ticketSubmitting, setTicketSubmitting] = useState(false);
  const [busyStaffId, setBusyStaffId] = useState('');
  const [busyTicketId, setBusyTicketId] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const loadData = async () => {
    const [users, allTickets] = await Promise.all([
      maintenanceUserApi.getAll(),
      maintenanceApi.getAllManagementTickets(),
    ]);
    setStaff(users || []);
    setTickets(allTickets || []);
  };

  useEffect(() => {
    let active = true;

    Promise.all([
      maintenanceUserApi.getAll(),
      maintenanceApi.getAllManagementTickets(),
    ])
      .then(([users, allTickets]) => {
        if (active) {
          setStaff(users || []);
          setTickets(allTickets || []);
        }
      })
      .catch((err) => {
        if (active) setError(err.message || 'Unable to load maintenance operations.');
      })
      .finally(() => {
        if (active) {
          setLoading(false);
          setLoadingTickets(false);
        }
      });

    return () => {
      active = false;
    };
  }, []);

  const handleCreateStaff = async (event) => {
    event.preventDefault();
    setError('');
    setSuccess('');
    setStaffSubmitting(true);
    try {
      const created = await maintenanceUserApi.create({
        ...staffForm,
        studentId: staffForm.studentId.trim(),
        name: staffForm.name.trim(),
        email: staffForm.email.trim(),
      });
      setStaff((current) => [...current, created].sort((left, right) => left.name.localeCompare(right.name)));
      setStaffForm(initialStaffForm);
      setSuccess(`Maintenance account created for ${created.name}.`);
    } catch (err) {
      setError(err.message || 'Unable to create the maintenance account.');
    } finally {
      setStaffSubmitting(false);
    }
  };

  const handleTeamChange = async (user, team) => {
    setError('');
    setSuccess('');
    setBusyStaffId(user.studentId);
    try {
      const updated = await maintenanceUserApi.updateTeam(user.studentId, team);
      setStaff((current) => current.map((entry) => (
        entry.studentId === user.studentId ? updated : entry
      )));
      setSuccess(`${user.name}'s team updated to ${teamLabel(team)}.`);
    } catch (err) {
      setError(err.message || 'Unable to update the maintenance team.');
    } finally {
      setBusyStaffId('');
    }
  };

  const handleActiveChange = async (user) => {
    setError('');
    setSuccess('');
    setBusyStaffId(user.studentId);
    try {
      const updated = await maintenanceUserApi.setActive(user.studentId, !user.active);
      setStaff((current) => current.map((entry) => (
        entry.studentId === user.studentId ? updated : entry
      )));
      setSuccess(`${user.name}'s account is now ${updated.active ? 'active' : 'deactivated'}.`);
    } catch (err) {
      setError(err.message || 'Unable to update the account status.');
    } finally {
      setBusyStaffId('');
    }
  };

  const handleCreateTicket = async (event) => {
    event.preventDefault();
    setError('');
    setSuccess('');
    setTicketSubmitting(true);
    try {
      const ticket = await maintenanceApi.createTicket({
        ...ticketForm,
        description: ticketForm.description.trim(),
        floor: ticketForm.floor.trim(),
      });
      setTicketForm(initialTicketForm);
      setTickets((current) => [ticket, ...current]);
      setSuccess(`Ticket ${ticket.id} was assigned to ${teamLabel(ticket.team)}.`);
    } catch (err) {
      setError(err.message || 'Unable to create the maintenance ticket.');
    } finally {
      setTicketSubmitting(false);
    }
  };

  const handleRefresh = async () => {
    setError('');
    try {
      await loadData();
    } catch (err) {
      setError(err.message || 'Unable to refresh maintenance operations.');
    }
  };

  const handleAssignTeam = async (ticket, team) => {
    setError('');
    setSuccess('');
    setBusyTicketId(ticket.id);
    try {
      const updated = await maintenanceApi.assignTeam(ticket.id, team);
      setTickets((current) => current.map((entry) => (
        entry.id === updated.id ? updated : entry
      )));
      setSuccess(`Ticket ${ticket.id} assigned to ${teamLabel(team)}.`);
    } catch (err) {
      setError(err.message || 'Unable to assign this ticket.');
    } finally {
      setBusyTicketId('');
    }
  };

  if (loading) return <LoadingState message="Loading maintenance operations..." />;

  return (
    <div className="page-container">
      <header className="page-header">
        <div>
          <p className="eyebrow">Maintenance administration</p>
          <h1>Maintenance operations</h1>
          <p className="page-subtitle">Manage maintenance staff and triage tickets reported across the library.</p>
        </div>
      </header>

      <ErrorMessage message={error} />
      {success && <div className="success-box" role="status">{success}</div>}

      <section className="maintenance-admin-grid">
        <section className="panel-card maintenance-form-card">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Staff access</p>
              <h2><UserPlus size={20} /> Register maintenance staff</h2>
            </div>
          </div>
          <form className="stack-form" onSubmit={handleCreateStaff}>
            <label>
              Staff ID
              <input
                value={staffForm.studentId}
                onChange={(event) => setStaffForm((current) => ({ ...current, studentId: event.target.value }))}
                maxLength={100}
                required
              />
            </label>
            <label>
              Full name
              <input
                value={staffForm.name}
                onChange={(event) => setStaffForm((current) => ({ ...current, name: event.target.value }))}
                maxLength={100}
                required
              />
            </label>
            <label>
              Email
              <input
                type="email"
                value={staffForm.email}
                onChange={(event) => setStaffForm((current) => ({ ...current, email: event.target.value }))}
                maxLength={254}
                required
              />
            </label>
            <label>
              Temporary password
              <input
                type="password"
                value={staffForm.password}
                onChange={(event) => setStaffForm((current) => ({ ...current, password: event.target.value }))}
                minLength={8}
                maxLength={100}
                autoComplete="new-password"
                required
              />
              <span className="field-hint">Use at least 8 characters.</span>
            </label>
            <label>
              Assigned team
              <TeamSelect
                value={staffForm.team}
                onChange={(team) => setStaffForm((current) => ({ ...current, team }))}
              />
            </label>
            <button className="primary-button" type="submit" disabled={staffSubmitting}>
              <UserPlus size={16} />
              {staffSubmitting ? 'Creating account...' : 'Create account'}
            </button>
          </form>
        </section>

        <section className="panel-card maintenance-form-card">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Ticket intake</p>
              <h2><ClipboardPlus size={20} /> Create a ticket</h2>
            </div>
          </div>
          <form className="stack-form" onSubmit={handleCreateTicket}>
            <label>
              Initial team
              <TeamSelect
                value={ticketForm.team}
                onChange={(team) => setTicketForm((current) => ({ ...current, team }))}
              />
            </label>
            <label>
              Library floor / location
              <input
                value={ticketForm.floor}
                onChange={(event) => setTicketForm((current) => ({ ...current, floor: event.target.value }))}
                maxLength={100}
                placeholder="e.g. Ground floor"
                required
              />
            </label>
            <label>
              Priority
              <select
                value={ticketForm.priority}
                onChange={(event) => setTicketForm((current) => ({ ...current, priority: event.target.value }))}
              >
                {priorities.map((priority) => <option key={priority} value={priority}>{priority}</option>)}
              </select>
            </label>
            <label>
              Issue description
              <textarea
                value={ticketForm.description}
                onChange={(event) => setTicketForm((current) => ({ ...current, description: event.target.value }))}
                rows={5}
                maxLength={2000}
                required
              />
              <span className="field-hint">{ticketForm.description.length} / 2000</span>
            </label>
            <button className="primary-button" type="submit" disabled={ticketSubmitting}>
              <Wrench size={16} />
              {ticketSubmitting ? 'Creating ticket...' : 'Create ticket'}
            </button>
          </form>
        </section>
      </section>

      <section className="panel-card">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Account directory</p>
            <h2><Users size={20} /> Maintenance staff</h2>
          </div>
          <div className="page-header-actions">
            <span className="summary-box">{staff.length} staff</span>
            <button type="button" className="secondary refresh-button" onClick={handleRefresh}>
              <RefreshCw size={16} />
              Refresh
            </button>
          </div>
        </div>

        {staff.length === 0 ? (
          <div className="empty-state">No maintenance accounts have been registered yet.</div>
        ) : (
          <div className="maintenance-staff-list">
            {staff.map((user) => (
              <article className="maintenance-staff-row" key={user.studentId}>
                <div className="maintenance-staff-identity">
                  <strong>{user.name}</strong>
                  <span>{user.studentId} · {user.email}</span>
                </div>
                <label className="maintenance-inline-field">
                  Team
                  <TeamSelect
                    value={user.maintenanceTeam || ''}
                    onChange={(team) => handleTeamChange(user, team)}
                    disabled={busyStaffId === user.studentId}
                    aria-label={`Team for ${user.name}`}
                  />
                </label>
                <span className={`maintenance-account-state ${user.active ? 'is-active' : 'is-inactive'}`}>
                  {user.active ? 'Active' : 'Deactivated'}
                </span>
                <button
                  type="button"
                  className={user.active ? 'secondary' : 'primary-button'}
                  onClick={() => handleActiveChange(user)}
                  disabled={busyStaffId === user.studentId}
                >
                  {busyStaffId === user.studentId ? 'Saving...' : user.active ? 'Deactivate' : 'Activate'}
                </button>
              </article>
            ))}
          </div>
        )}
      </section>

      <div className="stats-grid maintenance-stats">
        <div className="stat-card maintenance-open">
          <span>Open tickets</span>
          <strong>{tickets.filter((ticket) => ticket.status === 'OPEN').length}</strong>
        </div>
        <div className="stat-card maintenance-progress">
          <span>In progress</span>
          <strong>{tickets.filter((ticket) => ticket.status === 'IN_PROGRESS').length}</strong>
        </div>
        <div className="stat-card maintenance-resolved">
          <span>Resolved</span>
          <strong>{tickets.filter((ticket) => ticket.status === 'RESOLVED').length}</strong>
        </div>
      </div>

      <section className="panel-card">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Triage and assignment</p>
            <h2><Wrench size={20} /> Maintenance tickets</h2>
          </div>
          <div className="page-header-actions">
            <span className="summary-box">{tickets.length} tickets</span>
            <button type="button" className="secondary refresh-button" onClick={handleRefresh}>
              <RefreshCw size={16} />
              Refresh
            </button>
          </div>
        </div>
        {loadingTickets ? (
          <LoadingState message="Loading tickets..." />
        ) : tickets.length === 0 ? (
          <div className="empty-state">There are no maintenance tickets yet.</div>
        ) : (
          <div className="maintenance-ticket-list">
            {tickets.map((ticket) => (
              <article className="maintenance-ticket-card" key={ticket.id}>
                <div className="maintenance-ticket-heading">
                  <span className={`maintenance-priority priority-${ticket.priority?.toLowerCase()}`}>
                    {ticket.priority || 'Unspecified'} priority
                  </span>
                  <span className={`maintenance-ticket-status status-${ticket.status?.toLowerCase()}`}>
                    {ticket.status?.replaceAll('_', ' ') || 'Unknown'}
                  </span>
                </div>
                <p className="maintenance-ticket-description">{ticket.description}</p>
                <div className="maintenance-ticket-meta">
                  <span>Floor: <strong>{ticket.floor || 'Not specified'}</strong></span>
                  <span>Team: <strong>{teamLabel(ticket.team)}</strong></span>
                  <span>Submitted: <strong>{formatDate(ticket.submittedAt)}</strong></span>
                </div>
                <div className="maintenance-ticket-actions">
                  <label className="maintenance-inline-field">
                    Assign team
                    <TeamSelect
                      value={ticket.team || ''}
                      onChange={(team) => handleAssignTeam(ticket, team)}
                      disabled={busyTicketId === ticket.id}
                      aria-label={`Assign ticket ${ticket.id} to a team`}
                    />
                  </label>
                  <Link className="secondary maintenance-details-link" to={`/maintenance-admin/tickets/${encodeURIComponent(ticket.id)}`}>
                    View details
                  </Link>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

function TeamSelect({ value, onChange, disabled = false, ...props }) {
  return (
    <select
      value={value}
      onChange={(event) => onChange(event.target.value)}
      disabled={disabled}
      required
      {...props}
    >
      {teams.map((team) => (
        <option key={team.value} value={team.value}>{team.label}</option>
      ))}
      {value === '' && <option value="" disabled>Choose a team</option>}
    </select>
  );
}

function teamLabel(value) {
  return teams.find((team) => team.value === value)?.label || value || 'Unassigned';
}

function formatDate(value) {
  if (!value) return 'Date unavailable';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}