import { useEffect, useMemo, useState } from 'react';
import { Check, Send, Trash2 } from 'lucide-react';
import { feedbackApi } from '../api/feedbackApi';
import ErrorMessage from '../components/ErrorMessage';
import LoadingState from '../components/LoadingState';

const filters = ['ALL', 'PENDING', 'REVIEWED', 'RESOLVED'];

const formatDate = (value) => {
  if (!value) return 'Date unavailable';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
};

const statusClass = (status) => `feedback-status status-${status?.toLowerCase() || 'pending'}`;

export default function AdminFeedback() {
  const [feedback, setFeedback] = useState([]);
  const [responses, setResponses] = useState({});
  const [filter, setFilter] = useState('ALL');
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const loadFeedback = async () => {
    try {
      const items = await feedbackApi.getAll();
      setFeedback(items || []);
      setResponses(Object.fromEntries((items || []).map((item) => [item.id, item.adminResponse || ''])));
    } catch (err) {
      setError(err.message || 'Unable to load feedback.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadFeedback();
  }, []);

  const visibleFeedback = useMemo(
    () => filter === 'ALL' ? feedback : feedback.filter((item) => item.status === filter),
    [feedback, filter],
  );

  const saveResponse = async (item) => {
    const response = (responses[item.id] || '').trim();
    if (!response) {
      setError('Enter a response before saving.');
      return;
    }

    setBusyId(item.id);
    setError('');
    setSuccess('');
    try {
      const updated = await feedbackApi.respond(item.id, response);
      setFeedback((current) => current.map((entry) => entry.id === item.id ? updated : entry));
      setResponses((current) => ({ ...current, [item.id]: updated.adminResponse || response }));
      setSuccess('Response saved. Feedback marked as reviewed.');
    } catch (err) {
      setError(err.message || 'Unable to save response.');
    } finally {
      setBusyId('');
    }
  };

  const resolveFeedback = async (item) => {
    setBusyId(item.id);
    setError('');
    setSuccess('');
    try {
      const updated = await feedbackApi.resolve(item.id);
      setFeedback((current) => current.map((entry) => entry.id === item.id ? updated : entry));
      setSuccess('Feedback marked as resolved.');
    } catch (err) {
      setError(err.message || 'Unable to resolve feedback.');
    } finally {
      setBusyId('');
    }
  };

  const deleteFeedback = async (item) => {
    if (!window.confirm(`Delete feedback from ${item.studentId}?`)) return;

    setBusyId(item.id);
    setError('');
    setSuccess('');
    try {
      await feedbackApi.delete(item.id);
      setFeedback((current) => current.filter((entry) => entry.id !== item.id));
      setSuccess('Feedback deleted.');
    } catch (err) {
      setError(err.message || 'Unable to delete feedback.');
    } finally {
      setBusyId('');
    }
  };

  if (loading) return <LoadingState message="Loading feedback..." />;

  return (
    <div className="page-container">
      <header className="page-header">
        <div>
          <p className="eyebrow">Admin review</p>
          <h1>Student feedback</h1>
          <p className="page-subtitle">Review submissions, respond, and track resolution.</p>
        </div>
      </header>

      <ErrorMessage message={error} />
      {success && <div className="success-box" role="status">{success}</div>}

      <div className="feedback-admin-toolbar">
        <div className="filter-group" aria-label="Filter feedback by status">
          {filters.map((status) => (
            <button
              type="button"
              key={status}
              className={`filter-chip ${filter === status ? 'selected' : ''}`}
              onClick={() => setFilter(status)}
            >
              {status === 'ALL' ? 'All' : status}
            </button>
          ))}
        </div>
        <span className="summary-box">{visibleFeedback.length} shown / {feedback.length}</span>
      </div>

      {visibleFeedback.length === 0 ? (
        <div className="empty-state">No feedback matches this status.</div>
      ) : (
        <div className="feedback-list">
          {visibleFeedback.map((item) => (
            <article className="feedback-item admin-feedback-item" key={item.id}>
              <div className="feedback-item-top">
                <div className="feedback-item-title">
                  <span className="feedback-category">{item.category}</span>
                  <span className={statusClass(item.status)}>{item.status}</span>
                </div>
                <span className="feedback-date">{formatDate(item.createdAt)}</span>
              </div>

              <div className="feedback-admin-meta">
                <span><strong>Student</strong> {item.studentId}</span>
                <span><strong>Rating</strong> {item.rating} / 5</span>
              </div>
              <p className="feedback-comment">{item.comment}</p>

              <label>
                Admin response
                <textarea
                  value={responses[item.id] ?? item.adminResponse ?? ''}
                  onChange={(event) => setResponses((current) => ({ ...current, [item.id]: event.target.value }))}
                  rows={3}
                  maxLength={2000}
                  placeholder="Write a response to the student"
                />
                <span className="field-hint">{(responses[item.id] || '').length} / 2000</span>
              </label>

              <div className="feedback-item-actions admin-feedback-actions">
                <button
                  type="button"
                  className="primary-button"
                  onClick={() => saveResponse(item)}
                  disabled={busyId === item.id}
                >
                  <Send size={15} aria-hidden="true" />
                  Save response
                </button>
                {item.status !== 'RESOLVED' && (
                  <button
                    type="button"
                    className="secondary"
                    onClick={() => resolveFeedback(item)}
                    disabled={busyId === item.id}
                  >
                    <Check size={16} aria-hidden="true" />
                    Mark resolved
                  </button>
                )}
                <button
                  type="button"
                  className="danger"
                  onClick={() => deleteFeedback(item)}
                  disabled={busyId === item.id}
                >
                  <Trash2 size={15} aria-hidden="true" />
                  Delete
                </button>
              </div>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}