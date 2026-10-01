import SeatCard from './SeatCard';

export default function SeatGrid({ seats, onEdit, onDelete, isAdmin = false }) {
  if (!seats || seats.length === 0) {
    return <div className="empty-state">No seats match the current filter.</div>;
  }

  return (
    <div className="seat-grid">
      {seats.map((seat) => (
        <SeatCard
          key={seat.id || seat.seatNumber}
          seat={seat}
          onEdit={onEdit}
          onDelete={onDelete}
          isAdmin={isAdmin}
        />
      ))}
    </div>
  );
}
