import { useEffect, useState } from 'react';
import { Bot, MessageSquareText, Send, Trash2, Wrench } from 'lucide-react';
import { Link } from 'react-router-dom';
import { feedbackApi } from '../api/feedbackApi';
import { maintenanceApi } from '../api/maintenanceApi';
import ErrorMessage from '../components/ErrorMessage';
import LoadingState from '../components/LoadingState';

const categories = ['SEATING', 'BOOKING', 'BOOKS', 'FACILITIES', 'CLEANLINESS', 'STAFF', 'OTHER'];
const maintenanceCategories = new Set(['FACILITIES', 'CLEANLINESS']);
const ratingLabels = ['Very poor', 'Poor', 'Average', 'Good', 'Excellent'];
const initialForm = { category: '', rating: 0, comment: '', floor: '' };
const initialReportForm = { description: '', floor: '', priority: 'MEDIUM' };
const reportPriorities = ['LOW', 'MEDIUM', 'HIGH'];

const AI_CATEGORY_LABEL = {
  NETWORK: 'Network',
  ELECTRICAL: 'Electrical',
  IT_SUPPORT: 'IT Support',
  HOUSEKEEPING: 'Housekeeping',
};

const formatDate = (value) => {
  if (!value) return 'Date unavailable';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
};

const statusClass = (status) => `feedback-status status-${status?.toLowerCase() || 'pending'}`;

export default function StudentFeedback() {
  const [activeTab, setActiveTab] = useState('feedback'); // 'feedback' | 'report'
  const [feedback, setFeedback] = useState([]);
  const [myReports, setMyReports] = useState([]);
  const [form, setForm] = useState(initialForm);
  const [reportForm, setReportForm] = useState(initialReportForm);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [reportSubmitting, setReportSubmitting] = useState(false);
  const [deletingId, setDeletingId] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const isMaintenanceCategory = maintenanceCategories.has(form.category);

  const loadData = async () => {
    try {
      const [items, reports] = await Promise.all([
        feedbackApi.getMine(),
        maintenanceApi.getMyReports(),
      ]);
      setFeedback(items || []);
      setMyReports(reports || []);
    } catch (err) {
      setError(err.message || 'Unable to load your feedback.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    setSuccess('');

    const comment = form.comment.trim();
    if (!form.category || !form.rating || !comment) {
      setError('Choose a category and rating, then enter a comment.');
      return;
    }

    setSubmitting(true);

    try {
      const body = { ...form, comment };
      if (isMaintenanceCategory && form.floor.trim()) {
        body.floor = form.floor.trim();
      }
      const created = await feedbackApi.submit(body);
      setFeedback((current) => [created, ...current]);

      // If a linked maintenance ticket was created, refresh reports
      if (created.maintenanceTicketId) {
        try {
          const reports = await maintenanceApi.getMyReports();
          setMyReports(reports || []);
        } catch {
          // non-critical
        }
        setSuccess('Your feedback was submitted. A maintenance ticket has been automatically created and dispatched.');
      } else {
        setSuccess('Your feedback was submitted.');
      }
      setForm(initialForm);
    } catch (err) {
      setError(err.message || 'Unable to submit feedback.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleReportSubmit = async (event) => {
    event.preventDefault();
    setError('');
    setSuccess('');
    const description = reportForm.description.trim();
    const floor = reportForm.floor.trim();
    if (!description || !floor) {
      setError('Please enter both a description and a floor/location.');
      return;
    }

    setReportSubmitting(true);
    try {
      const ticket = await maintenanceApi.reportIssue({
        description,
        floor,
        priority: reportForm.priority,
      });
      setMyReports((current) => [ticket, ...current]);
      setReportForm(initialReportForm);
      const cat = AI_CATEGORY_LABEL[ticket.aiCategory] || ticket.aiCategory;
      setSuccess(
        cat
          ? `Maintenance ticket created and classified as ${cat}. Dispatched to the ${ticket.team?.replaceAll('_', ' ') || 'relevant'} team.`
          : 'Maintenance ticket submitted. It is pending classification by the Maintenance Admin.'
      );
    } catch (err) {
      setError(err.message || 'Unable to submit the maintenance report.');
    } finally {
      setReportSubmitting(false);
    }
  };

  const deleteFeedback = async (item) => {
    if (!window.confirm('Delete this feedback entry?')) return;

    setDeletingId(item.id);
    setError('');
    try {
      await feedbackApi.delete(item.id);
      setFeedback((current) => current.filter((entry) => entry.id !== item.id));
    } catch (err) {
      setError(err.message || 'Unable to delete feedback.');
    } finally {
      setDeletingId('');
    }
  };

  if (loading) return <LoadingState message="Loading your feedback..." />;

  return (
    <div className="page-container">
      <header className="page-header">
        <div>
          <p className="eyebrow">Student feedback</p>
          <h1>Share your experience</h1>
          <p className="page-subtitle">Submit feedback or report a maintenance issue directly.</p>
        </div>
      </header>

      <ErrorMessage message={error} />
      {success && <div className="success-box" role="status">{success}</div>}

      {/* Tab switcher */}
      <div className="filter-group feedback-tabs" role="tablist" aria-label="Submission type">
        <button
          type="button"
          role="tab"
          aria-selected={activeTab === 'feedback'}
          className={`filter-chip ${activeTab === 'feedback' ? 'selected' : ''}`}
          onClick={() => { setActiveTab('feedback'); setError(''); setSuccess(''); }}
        >
          <MessageSquareText size={14} />
          General feedback
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={activeTab === 'report'}
          className={`filter-chip ${activeTab === 'report' ? 'selected' : ''}`}
          onClick={() => { setActiveTab('report'); setError(''); setSuccess(''); }}
        >
          <Wrench size={14} />
          Report maintenance issue
        </button>
      </div>

      {activeTab === 'feedback' && (
        <section className="panel-card feedback-form-panel">
          <div className="feedback-section-heading">
            <MessageSquareText size={20} aria-hidden="true" />
            <div>
              <p className="eyebrow">New entry</p>
              <h2>Tell us what went well or what needs attention</h2>
            </div>
          </div>

          <form className="stack-form" onSubmit={handleSubmit}>
            <label>
              Category
              <select
                value={form.category}
                onChange={(event) => setForm((current) => ({ ...current, category: event.target.value }))}
              >
                <option value="" disabled>Select a category</option>
                {categories.map((category) => <option key={category} value={category}>{category}</option>)}
              </select>
            </label>

            {/* AI hint for maintenance categories */}
            {isMaintenanceCategory && (
              <div className="maintenance-ai-box">
                <div className="maintenance-ai-box-title">
                  <Bot size={14} />
                  Groq AI will auto-classify this
                </div>
                <p style={{ margin: 0, fontSize: '0.875rem' }}>
                  Feedback submitted under <strong>{form.category}</strong> automatically creates a maintenance ticket.
                  Groq AI will classify and dispatch it to the correct maintenance team.
                </p>
              </div>
            )}

            {/* Optional floor for maintenance categories */}
            {isMaintenanceCategory && (
              <label>
                Library floor / location <span className="field-hint">(optional, helps route your ticket)</span>
                <input
                  value={form.floor}
                  onChange={(event) => setForm((current) => ({ ...current, floor: event.target.value }))}
                  maxLength={100}
                  placeholder="e.g. 2nd Floor, West Wing"
                />
              </label>
            )}

            <fieldset className="rating-fieldset">
              <legend>Rating</legend>
              <div className="rating-options" role="group" aria-label="Choose a rating from one to five">
                {ratingLabels.map((ratingLabel, index) => {
                  const value = index + 1;
                  return (
                    <button
                      type="button"
                      className={`rating-option ${form.rating === value ? 'selected' : ''}`}
                      key={value}
                      aria-pressed={form.rating === value}
                      onClick={() => setForm((current) => ({ ...current, rating: value }))}
                    >
                      <strong>{value}</strong>
                      <span>{ratingLabel}</span>
                    </button>
                  );
                })}
              </div>
            </fieldset>

            <label>
              Comment
              <textarea
                value={form.comment}
                onChange={(event) => setForm((current) => ({ ...current, comment: event.target.value }))}
                rows={4}
                maxLength={2000}
                placeholder="Share the details of your experience"
              />
              <span className="field-hint">{form.comment.length} / 2000</span>
            </label>

            <div>
              <button type="submit" className="primary-button" disabled={submitting}>
                <Send size={16} aria-hidden="true" />
                {submitting ? 'Submitting...' : 'Submit feedback'}
              </button>
            </div>
          </form>
        </section>
      )}

      {activeTab === 'report' && (
        <section className="panel-card feedback-form-panel">
          <div className="feedback-section-heading">
            <Wrench size={20} aria-hidden="true" />
            <div>
              <p className="eyebrow">Maintenance issue</p>
              <h2>Report a maintenance problem</h2>
            </div>
          </div>

          <div className="maintenance-ai-box" style={{ marginBottom: '1rem' }}>
            <div className="maintenance-ai-box-title">
              <Bot size={14} />
              Powered by Groq AI
            </div>
            <p style={{ margin: 0, fontSize: '0.875rem' }}>
              Your issue description is automatically classified by Groq AI and routed to the correct maintenance team — Network, Electrical, IT Support, or Housekeeping.
            </p>
          </div>

          <form className="stack-form" onSubmit={handleReportSubmit}>
            <label>
              Library floor / location
              <input
                value={reportForm.floor}
                onChange={(event) => setReportForm((current) => ({ ...current, floor: event.target.value }))}
                maxLength={100}
                placeholder="e.g. 3rd Floor, East Wing"
                required
              />
            </label>
            <label>
              Priority
              <select
                value={reportForm.priority}
                onChange={(event) => setReportForm((current) => ({ ...current, priority: event.target.value }))}
              >
                {reportPriorities.map((p) => <option key={p} value={p}>{p}</option>)}
              </select>
            </label>
            <label>
              Issue description
              <textarea
                value={reportForm.description}
                onChange={(event) => setReportForm((current) => ({ ...current, description: event.target.value }))}
                rows={5}
                maxLength={2000}
                placeholder="Describe the issue clearly. e.g. 'The Wi-Fi on the 3rd floor is down and students cannot access course materials.'"
                required
              />
              <span className="field-hint">{reportForm.description.length} / 2000</span>
            </label>
            <div>
              <button type="submit" className="primary-button" disabled={reportSubmitting}>
                <Send size={16} aria-hidden="true" />
                {reportSubmitting ? 'Submitting...' : 'Submit maintenance report'}
              </button>
            </div>
          </form>
        </section>
      )}

      {/* Feedback history */}
      <section className="feedback-history">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Your submissions</p>
            <h2>Feedback history</h2>
          </div>
          <span className="summary-box">{feedback.length} {feedback.length === 1 ? 'entry' : 'entries'}</span>
        </div>

        {feedback.length === 0 ? (
          <div className="empty-state">Your submitted feedback will appear here.</div>
        ) : (
          <div className="feedback-list">
            {feedback.map((item) => (
              <article className="feedback-item" key={item.id}>
                <div className="feedback-item-top">
                  <div className="feedback-item-title">
                    <span className="feedback-category">{item.category}</span>
                    <span className={statusClass(item.status)}>{item.status}</span>
                  </div>
                  <span className="feedback-date">{formatDate(item.createdAt)}</span>
                </div>
                <div className="feedback-rating" aria-label={`Rating: ${item.rating} out of 5`}>
                  {item.rating} / 5 <span>{ratingLabels[item.rating - 1]}</span>
                </div>
                <p className="feedback-comment">{item.comment}</p>

                {/* Linked maintenance ticket */}
                {item.maintenanceTicketId && (
                  <div className="maintenance-ai-inline feedback-linked-ticket">
                    <Bot size={13} />
                    <span>Maintenance ticket created</span>
                    {item.aiCategory && (
                      <span className="maintenance-ai-badge">
                        {AI_CATEGORY_LABEL[item.aiCategory] || item.aiCategory}
                      </span>
                    )}
                    <Link
                      to={`/maintenance/tickets/${encodeURIComponent(item.maintenanceTicketId)}`}
                      className="maintenance-details-link"
                    >
                      View ticket →
                    </Link>
                  </div>
                )}

                {item.adminResponse && (
                  <div className="admin-response">
                    <span className="eyebrow">Library response</span>
                    <p>{item.adminResponse}</p>
                  </div>
                )}
                <div className="feedback-item-actions">
                  <button
                    type="button"
                    className="danger feedback-delete-button"
                    onClick={() => deleteFeedback(item)}
                    disabled={deletingId === item.id}
                  >
                    <Trash2 size={15} aria-hidden="true" />
                    {deletingId === item.id ? 'Deleting...' : 'Delete'}
                  </button>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>

      {/* My maintenance reports */}
      {myReports.length > 0 && (
        <section className="feedback-history">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Maintenance reports</p>
              <h2><Wrench size={18} /> My maintenance reports</h2>
            </div>
            <span className="summary-box">{myReports.length} {myReports.length === 1 ? 'report' : 'reports'}</span>
          </div>
          <div className="maintenance-ticket-list">
            {myReports.map((ticket) => (
              <article className="maintenance-ticket-card" key={ticket.id}>
                <div className="maintenance-ticket-heading">
                  <span className={`maintenance-priority priority-${ticket.priority?.toLowerCase()}`}>
                    {ticket.priority || 'Unspecified'} priority
                  </span>
                  <span className={`maintenance-ticket-status status-${ticket.status?.toLowerCase().replaceAll('_', '-')}`}>
                    {ticket.status?.replaceAll('_', ' ')}
                  </span>
                </div>
                <p className="maintenance-ticket-description">{ticket.description}</p>

                {ticket.aiCategory && (
                  <div className="maintenance-ai-inline">
                    <Bot size={13} />
                    <span>AI classified as: </span>
                    <span className="maintenance-ai-badge">
                      {AI_CATEGORY_LABEL[ticket.aiCategory] || ticket.aiCategory}
                    </span>
                    {ticket.team && (
                      <span> · Dispatched to <strong>{ticket.team.replaceAll('_', ' ')}</strong></span>
                    )}
                  </div>
                )}
                {ticket.classificationReason && (
                  <p className="maintenance-ai-reason-text">{ticket.classificationReason}</p>
                )}

                <div className="maintenance-ticket-meta">
                  <span>Floor: <strong>{ticket.floor || 'Not specified'}</strong></span>
                  <span>Submitted: <strong>{formatDate(ticket.submittedAt)}</strong></span>
                </div>
                <Link
                  className="secondary maintenance-details-link"
                  to={`/maintenance/tickets/${encodeURIComponent(ticket.id)}`}
                >
                  View ticket details
                </Link>
              </article>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
