import { useEffect, useState } from 'react';
import {
  Megaphone,
  AlertTriangle,
  Clock,
  Wrench,
  Info,
  Calendar,
  Trash2,
  PlusCircle,
  CheckCircle2,
  Bell
} from 'lucide-react';
import { announcementApi } from '../api/announcementApi';
import LoadingState from '../components/LoadingState';
import ErrorMessage from '../components/ErrorMessage';

const TYPE_CONFIG = {
  CLOSURE: {
    label: 'Early Closure',
    color: '#b91c1c',
    bg: '#fee2e2',
    border: '#fca5a5',
    icon: Clock
  },
  URGENT: {
    label: 'Urgent Alert',
    color: '#c2410c',
    bg: '#ffedd5',
    border: '#fdba74',
    icon: AlertTriangle
  },
  MAINTENANCE: {
    label: 'Maintenance',
    color: '#b45309',
    bg: '#fef3c7',
    border: '#fde68a',
    icon: Wrench
  },
  EVENT: {
    label: 'Library Event',
    color: '#4338ca',
    bg: '#e0e7ff',
    border: '#c7d2fe',
    icon: Calendar
  },
  GENERAL: {
    label: 'General Notice',
    color: '#1d4ed8',
    bg: '#eff6ff',
    border: '#bfdbfe',
    icon: Info
  }
};

const PRIORITY_BADGES = {
  HIGH: { label: 'High Priority', color: '#b91c1c', bg: '#fee2e2' },
  MEDIUM: { label: 'Medium Priority', color: '#b45309', bg: '#fef3c7' },
  NORMAL: { label: 'Normal Priority', color: '#15803d', bg: '#dcfce7' }
};

export default function AdminAnnouncements() {
  const [announcements, setAnnouncements] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  // Form State
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [type, setType] = useState('CLOSURE');
  const [priority, setPriority] = useState('HIGH');

  const loadAnnouncements = async () => {
    try {
      const data = await announcementApi.getAllAnnouncements();
      setAnnouncements(data || []);
    } catch (err) {
      setError(err.message || 'Failed to load announcements.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAnnouncements();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!title.trim() || !content.trim()) {
      setError('Please provide both title and announcement details.');
      return;
    }

    setSubmitting(true);
    setError('');
    setSuccessMessage('');

    try {
      await announcementApi.createAnnouncement({
        title: title.trim(),
        content: content.trim(),
        type,
        priority
      });

      setSuccessMessage('Announcement posted successfully! All students can now view this notice.');
      setTitle('');
      setContent('');
      setType('CLOSURE');
      setPriority('HIGH');
      await loadAnnouncements();
    } catch (err) {
      setError(err.message || 'Failed to post announcement.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id, annTitle) => {
    if (!window.confirm(`Are you sure you want to delete announcement "${annTitle}"?`)) {
      return;
    }

    try {
      await announcementApi.deleteAnnouncement(id);
      setSuccessMessage(`Announcement "${annTitle}" deleted.`);
      setAnnouncements((prev) => prev.filter((a) => a.id !== id));
    } catch (err) {
      setError(err.message || 'Failed to delete announcement.');
    }
  };

  if (loading) return <LoadingState message="Loading announcements..." />;

  const closuresCount = announcements.filter((a) => a.type === 'CLOSURE' || a.type === 'URGENT').length;
  const maintenanceCount = announcements.filter((a) => a.type === 'MAINTENANCE').length;

  return (
    <div className="page-container">
      <header className="page-header">
        <div>
          <p className="eyebrow">Admin Controls</p>
          <h1>Announcements</h1>
          <p className="page-subtitle">
            Broadcast emergency updates, early closing hours, maintenance warnings, and notices to all students.
          </p>
        </div>
      </header>

      <ErrorMessage message={error} />

      {successMessage && (
        <div className="feedback-status success" role="alert" style={{ marginBottom: 0 }}>
          <CheckCircle2 size={16} />
          <span>{successMessage}</span>
        </div>
      )}

      {/* Metrics Row */}
      <div className="stats-grid">
        <div className="stat-card">
          <span>
            <Megaphone size={16} /> Total Announcements
          </span>
          <strong>{announcements.length}</strong>
        </div>

        <div className="stat-card accent">
          <span>
            <Clock size={16} /> Closures / Urgent Notices
          </span>
          <strong>{closuresCount}</strong>
        </div>

        <div className="stat-card">
          <span>
            <Wrench size={16} /> Maintenance Alerts
          </span>
          <strong>{maintenanceCount}</strong>
        </div>

        <div className="stat-card">
          <span>
            <Bell size={16} /> General Notices
          </span>
          <strong>{announcements.length - closuresCount - maintenanceCount}</strong>
        </div>
      </div>

      {/* Create Announcement Form Panel */}
      <section className="panel-card announcement-create-card">
        <div className="section-heading">
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <div className="create-ann-icon">
              <PlusCircle size={20} />
            </div>
            <div>
              <p className="eyebrow">Publish Notice</p>
              <h2>Post a New Announcement</h2>
            </div>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="stack-form">
          <label>
            Announcement Title *
            <input
              type="text"
              placeholder="e.g. Library will close at 6:00 PM today due to maintenance"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              required
              maxLength={200}
            />
          </label>

          <div className="two-column-form">
            <label>
              Notice Type / Category *
              <select value={type} onChange={(e) => setType(e.target.value)}>
                <option value="CLOSURE">Early Library Closure (Clock/Alert)</option>
                <option value="URGENT">Urgent Sudden Notice</option>
                <option value="MAINTENANCE">Facility / IT Maintenance</option>
                <option value="GENERAL">General Notice</option>
                <option value="EVENT">Library Event / Workshops</option>
              </select>
            </label>

            <label>
              Priority Level *
              <select value={priority} onChange={(e) => setPriority(e.target.value)}>
                <option value="HIGH">High (Prominent Alert Banner)</option>
                <option value="MEDIUM">Medium (Attention Required)</option>
                <option value="NORMAL">Normal (Standard Bulletin)</option>
              </select>
            </label>
          </div>

          <label>
            Detailed Message *
            <textarea
              rows={4}
              placeholder="Describe the announcement details, effective times, affected floors or services, and when normal operations resume..."
              value={content}
              onChange={(e) => setContent(e.target.value)}
              required
              maxLength={3000}
              className="announcement-textarea"
            />
          </label>

          <div className="card-actions" style={{ marginTop: '8px' }}>
            <button
              type="submit"
              className="primary-button"
              disabled={submitting}
              style={{ display: 'inline-flex', alignItems: 'center', gap: '8px' }}
            >
              <Megaphone size={16} />
              <span>{submitting ? 'Publishing...' : 'Publish Announcement to Students'}</span>
            </button>
          </div>
        </form>
      </section>

      {/* Posted Announcements List */}
      <section className="panel-card">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Public Notices</p>
            <h2>Posted Announcements ({announcements.length})</h2>
          </div>
        </div>

        {announcements.length === 0 ? (
          <div className="empty-catalog-state">
            <Megaphone size={40} className="empty-icon" />
            <h3>No announcements posted yet</h3>
            <p>Use the form above to post early closures or sudden library notices.</p>
          </div>
        ) : (
          <div className="announcement-list">
            {announcements.map((ann) => {
              const typeCfg = TYPE_CONFIG[ann.type] || TYPE_CONFIG.GENERAL;
              const priorityCfg = PRIORITY_BADGES[ann.priority] || PRIORITY_BADGES.NORMAL;
              const TypeIcon = typeCfg.icon;

              const formattedDate = ann.createdAt
                ? new Date(ann.createdAt).toLocaleDateString(undefined, {
                    month: 'short',
                    day: 'numeric',
                    year: 'numeric',
                    hour: '2-digit',
                    minute: '2-digit'
                  })
                : 'Just now';

              return (
                <div key={ann.id} className="announcement-card-admin">
                  <div className="announcement-admin-header">
                    <div className="announcement-badges">
                      <span
                        className="announcement-type-pill"
                        style={{
                          backgroundColor: typeCfg.bg,
                          color: typeCfg.color,
                          borderColor: typeCfg.border
                        }}
                      >
                        <TypeIcon size={13} style={{ marginRight: '5px' }} />
                        {typeCfg.label}
                      </span>

                      <span
                        className="announcement-priority-pill"
                        style={{
                          backgroundColor: priorityCfg.bg,
                          color: priorityCfg.color
                        }}
                      >
                        {priorityCfg.label}
                      </span>
                    </div>

                    <button
                      type="button"
                      className="danger delete-ann-btn"
                      onClick={() => handleDelete(ann.id, ann.title)}
                      title="Delete announcement"
                    >
                      <Trash2 size={15} />
                      <span>Delete</span>
                    </button>
                  </div>

                  <h3 className="announcement-card-title">{ann.title}</h3>

                  <div className="announcement-meta-row">
                    <span className="announcement-meta-item">
                      <Calendar size={13} />
                      Posted: {formattedDate}
                    </span>
                    {ann.postedBy && (
                      <span className="announcement-meta-item">
                        By: {ann.postedBy}
                      </span>
                    )}
                  </div>

                  <p className="announcement-card-body">{ann.content}</p>
                </div>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
}
