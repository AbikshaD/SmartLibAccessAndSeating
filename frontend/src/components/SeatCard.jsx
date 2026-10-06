const formatStatus = (status) => {
  if (!status) return 'Unknown';
  return status.charAt(0).toUpperCase() + status.slice(1).toLowerCase();
};

export default function SeatCard({ seat, onEdit, onDelete, onBook, isAdmin = false }) {
  const isAvailable = seat?.status === 'AVAILABLE';

  return (
    <article className="seat-card">
      <div className="seat-card-header">
        <div>
          <p className="eyebrow">Seat</p>
          <h3>{seat.seatNumber}</h3>
        </div>
        <span className={`badge ${isAvailable ? 'success' : 'warning'}`}>
          {formatStatus(seat.status)}
        </span>
      </div>

      <dl className="seat-meta">
        <div>
          <dt>Floor</dt>
          <dd>{seat.floor}</dd>
        </div>
        <div>
          <dt>Status</dt>
          <dd>{formatStatus(seat.status)}</dd>
        </div>
      </dl>

      {isAdmin && (
        <div className="card-actions">
          <button type="button" className="secondary" onClick={() => onEdit(seat)}>
            Edit
          </button>
          <button type="button" className="danger" onClick={() => onDelete(seat)}>
            Delete
          </button>
        </div>
      )}

      {onBook && (
        <div className="card-actions">
          <button
            type="button"
            className="primary-button seat-book-button"
            onClick={() => onBook(seat)}
            disabled={!isAvailable}
          >
            {isAvailable ? 'Book this seat' : 'Unavailable'}
          </button>
        </div>
      )}
    </article>
  );
}
