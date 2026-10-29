import { useCallback, useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import {
  AlertCircle,
  Bell,
  Bot,
  Check,
  CheckCheck,
  CheckCircle2,
  Clock3,
  RefreshCw,
  Send,
  Wrench,
} from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { getApiBaseUrl, getAuthToken } from '../api/api';
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
  const navigate = useNavigate();
  const [dashboard, setDashboard] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [report, setReport] = useState({ description: '', floor: '', priority: 'MEDIUM' });
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [notificationError, setNotificationError] = useState('');
  const [liveNotification, setLiveNotification] = useState(null);
  const receivedNotifications = useRef(new Map());

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

  useEffect(() => {
    let active = true;
    Promise.all([
      maintenanceApi.getNotifications(),
      maintenanceApi.getUnreadNotificationCount(),
    ])
      .then(([items, count]) => {
        if (!active) return;
        const persistedIds = new Set(items.map((item) => item.id));
        const newlyReceived = [...receivedNotifications.current.values()]
          .filter((item) => !persistedIds.has(item.id));
        setNotifications((current) => {
          const byId = new Map(items.map((item) => [item.id, item]));
          current.forEach((item) => {
            if (!byId.has(item.id)) byId.set(item.id, item);
          });
          return [...byId.values()].sort((first, second) => (
            new Date(second.createdAt) - new Date(first.createdAt)
          ));
        });
        items.forEach((item) => receivedNotifications.current.set(item.id, item));
        setUnreadCount(count.unreadCount + newlyReceived.filter((item) => !item.read).length);
        setNotificationError('');
      })
      .catch((err) => {
        if (active) setNotificationError(err.message || 'Unable to load notifications.');
      });

    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    const token = getAuthToken();
    if (!token) return undefined;

    const apiUrl = new URL(getApiBaseUrl());
    apiUrl.protocol = apiUrl.protocol === 'https:' ? 'wss:' : 'ws:';
    apiUrl.pathname = '/ws';
    apiUrl.search = '';
    apiUrl.hash = '';

    const client = new Client({
      brokerURL: apiUrl.toString(),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe('/user/queue/notifications', (message) => {
          try {
            const notification = JSON.parse(message.body);
            if (!receivedNotifications.current.has(notification.id)) {
              receivedNotifications.current.set(notification.id, notification);
              setNotifications((current) => (
                current.some((item) => item.id === notification.id)
                  ? current
                  : [notification, ...current]
              ));
              setUnreadCount((current) => current + 1);
            }
            setLiveNotification(notification);
            setNotificationError('');
            loadDashboard();
          } catch {
            setNotificationError('A live notification could not be read. Please refresh your notifications.');
          }
        });
      },
      onStompError: () => {
        setNotificationError('Live notifications are unavailable. Reconnecting…');
      },
      onWebSocketError: () => {
        setNotificationError('Live notifications are unavailable. Reconnecting…');
      },
    });

    client.activate();
    return () => {
      client.deactivate();
    };
  }, [loadDashboard]);

  useEffect(() => {
    if (!liveNotification) return undefined;
    const timeoutId = window.setTimeout(() => setLiveNotification(null), 6000);
    return () => window.clearTimeout(timeoutId);
  }, [liveNotification]);

  const markNotificationRead = async (notification) => {
    if (notification.read) return true;
    try {
      const updated = await maintenanceApi.markNotificationRead(notification.id);
      receivedNotifications.current.set(updated.id, updated);
      setNotifications((current) => current.map((item) => (
        item.id === updated.id ? updated : item
      )));
      setUnreadCount((current) => Math.max(0, current - 1));
      setNotificationError('');
      return true;
    } catch (err) {
      setNotificationError(err.message || 'Unable to mark this notification as read.');
      return false;
    }
  };

  const openNotification = async (notification) => {
    if (!await markNotificationRead(notification)) return;
    setNotificationsOpen(false);
    navigate(`/maintenance/tickets/${encodeURIComponent(notification.ticketId)}`);
  };

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
        <div className="maintenance-dashboard-actions">
          <div className="maintenance-notification-center">
            <button
              type="button"
              className="secondary maintenance-notification-trigger"
              aria-label={`Notifications${unreadCount ? `, ${unreadCount} unread` : ''}`}
              aria-expanded={notificationsOpen}
              aria-controls="maintenance-notification-panel"
              onClick={() => setNotificationsOpen((open) => !open)}
            >
              <Bell size={18} />
              <span>Notifications</span>
              {unreadCount > 0 && (
                <span className="maintenance-notification-count" aria-label={`${unreadCount} unread`}>
                  {unreadCount > 99 ? '99+' : unreadCount}
                </span>
              )}
            </button>
            {notificationsOpen && (
              <section
                className="maintenance-notification-panel"
                id="maintenance-notification-panel"
                aria-label="Maintenance notifications"
              >
                <div className="maintenance-notification-panel-heading">
                  <div>
                    <strong>Notifications</strong>
                    <span>{unreadCount} unread</span>
                  </div>
                  <button
                    type="button"
                    className="maintenance-notification-close"
                    aria-label="Close notifications"
                    onClick={() => setNotificationsOpen(false)}
                  >
                    <Check size={17} />
                  </button>
                </div>
                {notificationError && (
                  <p className="maintenance-notification-error" role="status">{notificationError}</p>
                )}
                {notifications.length === 0 ? (
                  <p className="maintenance-notification-empty">You’re all caught up.</p>
                ) : (
                  <ul className="maintenance-notification-list">
                    {notifications.map((notification) => (
                      <li key={notification.id}>
                        <button
                          type="button"
                          className={`maintenance-notification-item${notification.read ? '' : ' is-unread'}`}
                          onClick={() => openNotification(notification)}
                        >
                          <span className="maintenance-notification-item-icon">
                            {notification.read ? <Check size={15} /> : <Wrench size={15} />}
                          </span>
                          <span className="maintenance-notification-item-content">
                            <strong>New {label(notification.team)} ticket</strong>
                            <span>{notification.description}</span>
                            <time dateTime={notification.createdAt}>{formatDate(notification.createdAt)}</time>
                          </span>
                          {!notification.read && <span className="maintenance-notification-unread-dot" />}
                        </button>
                      </li>
                    ))}
                  </ul>
                )}
                <div className="maintenance-notification-panel-footer">
                  <CheckCheck size={15} />
                  Notifications are only visible to your assigned team.
                </div>
              </section>
            )}
          </div>
          <button type="button" className="secondary refresh-button" onClick={loadDashboard}>
            <RefreshCw size={16} />
            Refresh
          </button>
        </div>
      </header>

      <ErrorMessage message={error} />
      {success && <div className="success-box" role="status">{success}</div>}
      {liveNotification && (
        <div className="maintenance-live-toast" role="status">
          <span className="maintenance-live-toast-icon"><Bell size={17} /></span>
          <span>
            <strong>New {label(liveNotification.team)} ticket</strong>
            {liveNotification.description}
          </span>
          <button
            type="button"
            aria-label="Dismiss notification"
            onClick={() => setLiveNotification(null)}
          >
            <Check size={17} />
          </button>
        </div>
      )}

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
