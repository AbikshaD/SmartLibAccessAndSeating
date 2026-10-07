import { useEffect, useState } from 'react';
import { MessageSquareText, Send, Trash2 } from 'lucide-react';
import { feedbackApi } from '../api/feedbackApi';
import ErrorMessage from '../components/ErrorMessage';
import LoadingState from '../components/LoadingState';

const categories = ['SEATING', 'BOOKING', 'BOOKS', 'FACILITIES', 'CLEANLINESS', 'STAFF', 'OTHER'];
const ratingLabels = ['Very poor', 'Poor', 'Average', 'Good', 'Excellent'];
const initialForm = { category: '', rating: 0, comment: '' };

const formatDate = (value) => {
  if (!value) return 'Date unavailable';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
};

const statusClass = (status) => `feedback-status status-${status?.toLowerCase() || 'pending'}`;

export default function StudentFeedback() {
  const [feedback, setFeedback] = useState([]);
  const [form, setForm] = useState(initialForm);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [deletingId, setDeletingId] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const loadFeedback = async () => {
    try {
      const items = await feedbackApi.getMine();
      setFeedback(items || []);
    } catch (err) {
      setError(err.message || 'Unable to load your feedback.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadFeedback();
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
      const created = await feedbackApi.submit({ ...form, comment });
      setFeedback((current) => [created, ...current]);
      setForm(initialForm);
      setSuccess('Your feedback was submitted.');
    } catch (err) {
      setError(err.message || 'Unable to submit feedback.');
    } finally {
      setSubmitting(false);
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
          <p className="page-subtitle">Your feedback helps improve the library.</p>
        </div>
      </header>

      <ErrorMessage message={error} />
      {success && <div className="success-box" role="status">{success}</div>}

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

          <fieldset className="rating-fieldset">
            <legend>Rating</legend>
            <div className="rating-options" role="group" aria-label="Choose a rating from one to five">
              {ratingLabels.map((label, index) => {
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
                    <span>{label}</span>
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
    </div>
  );
}