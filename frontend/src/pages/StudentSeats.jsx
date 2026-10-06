import { useCallback, useEffect, useMemo, useState } from 'react';
import { Armchair, CalendarDays, CalendarPlus2, Clock3, MapPin, RefreshCw, X } from 'lucide-react';
import { bookingApi } from '../api/bookingApi';
import { seatApi } from '../api/seatApi';
import { useAuth } from '../context/AuthContext';
import ErrorMessage from '../components/ErrorMessage';
import LoadingState from '../components/LoadingState';
import SeatGrid from '../components/SeatGrid';

const getToday = () => {
  const today = new Date();
  const offset = today.getTimezoneOffset();
  return new Date(today.getTime() - offset * 60_000).toISOString().slice(0, 10);
};

const formatDate = (date) => {
  if (!date) return 'Date unavailable';
  return new Date(`${date}T00:00:00`).toLocaleDateString(undefined, {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
};

const formatTime = (time) => (time ? time.slice(0, 5) : '--:--');

export default function StudentSeats() {
  const { role } = useAuth();
  const isStudent = role === 'STUDENT';
  const [seats, setSeats] = useState([]);
  const [bookings, setBookings] = useState([]);
  const [selectedSeat, setSelectedSeat] = useState(null);
  const [form, setForm] = useState({ bookingDate: getToday(), startTime: '09:00', endTime: '10:00' });
  const [selectedFloor, setSelectedFloor] = useState('All');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [cancellingId, setCancellingId] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const loadData = useCallback(async () => {
    const requests = [seatApi.getAllSeats()];
    if (isStudent) requests.push(bookingApi.getMyBookings());

    const [seatData, bookingData = []] = await Promise.all(requests);
    setSeats(seatData || []);
    setBookings(bookingData || []);
  }, [isStudent]);

  useEffect(() => {
    let active = true;

    const load = async () => {
      try {
        await loadData();
      } catch (err) {
        if (active) setError(err.message || 'Unable to load seat and booking information.');
      } finally {
        if (active) setLoading(false);
      }
    };

    load();
    return () => {
      active = false;
    };
  }, [loadData]);

  const floorOptions = useMemo(() => {
    const floors = [...new Set(seats.map((seat) => seat.floor).filter(Boolean))].sort();
    return ['All', ...floors];
  }, [seats]);

  const filteredSeats = useMemo(
    () => (selectedFloor === 'All' ? seats : seats.filter((seat) => seat.floor === selectedFloor)),
    [selectedFloor, seats],
  );

  const availableCount = seats.filter((seat) => seat.status === 'AVAILABLE').length;
  const confirmedBookings = bookings.filter((booking) => booking.status === 'CONFIRMED');

  const refresh = async () => {
    setError('');
    try {
      await loadData();
    } catch (err) {
      setError(err.message || 'Unable to refresh seat and booking information.');
    }
  };

  const handleBookSeat = (seat) => {
    setSelectedSeat(seat);
    setSuccess('');
    setError('');
  };

  const handleBookingSubmit = async (event) => {
    event.preventDefault();
    setError('');
    setSuccess('');

    if (!selectedSeat) return;
    if (form.bookingDate < getToday()) {
      setError('Choose today or a future date.');
      return;
    }
    if (form.startTime >= form.endTime) {
      setError('The end time must be later than the start time.');
      return;
    }

    setSubmitting(true);
    try {
      await bookingApi.createBooking({
        seatId: selectedSeat.seatNumber,
        bookingDate: form.bookingDate,
        startTime: form.startTime,
        endTime: form.endTime,
      });
      setSelectedSeat(null);
      setSuccess(`Seat ${selectedSeat.seatNumber} is booked for ${formatDate(form.bookingDate)}.`);
      try {
        await loadData();
      } catch (err) {
        setError(`Your booking was created, but the latest seat list could not be loaded. ${err.message || ''}`.trim());
      }
    } catch (err) {
      setError(err.message || 'Unable to create this booking.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleCancelBooking = async (booking) => {
    setError('');
    setSuccess('');
    setCancellingId(booking.id);
    try {
      await bookingApi.cancelBooking(booking.id);
      setSuccess(`Booking for seat ${booking.seatNumber} has been cancelled.`);
      try {
        await loadData();
      } catch (err) {
        setError(`Your booking was cancelled, but the latest seat list could not be loaded. ${err.message || ''}`.trim());
      }
    } catch (err) {
      setError(err.message || 'Unable to cancel this booking.');
    } finally {
      setCancellingId('');
    }
  };

  if (loading) return <LoadingState message="Loading seats and bookings..." />;

  return (
    <div className="page-container">
      <header className="page-header">
        <div>
          <p className="eyebrow">{isStudent ? 'Find your study spot' : 'Seat directory'}</p>
          <h1>{isStudent ? 'Seats & bookings' : 'Library Seats'}</h1>
          <p className="page-subtitle">
            {isStudent
              ? 'Choose an available seat and reserve the time that works for you.'
              : 'View seat availability by floor.'}
          </p>
        </div>
        <button type="button" className="secondary refresh-button" onClick={refresh}>
          <RefreshCw size={16} />
          Refresh
        </button>
      </header>

      <ErrorMessage message={error} />
      {success && <div className="success-box" role="status">{success}</div>}

      <section className="stats-grid booking-stats">
        <div className="stat-card">
          <span><Armchair size={15} /> Total seats</span>
          <strong>{seats.length}</strong>
        </div>
        <div className="stat-card accent">
          <span><MapPin size={15} /> Available now</span>
          <strong>{availableCount}</strong>
        </div>
        {isStudent && (
          <div className="stat-card">
            <span><CalendarDays size={15} /> Active bookings</span>
            <strong>{confirmedBookings.length}</strong>
          </div>
        )}
      </section>

      {isStudent && (
        <section className="panel-card booking-history">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Your reservations</p>
              <h2>My bookings</h2>
            </div>
            <span className="summary-box">{bookings.length} total</span>
          </div>

          {bookings.length === 0 ? (
            <div className="empty-state">You don’t have any bookings yet. Choose an available seat below to get started.</div>
          ) : (
            <div className="booking-list">
              {bookings.map((booking) => {
                const isConfirmed = booking.status === 'CONFIRMED';
                return (
                  <article className="booking-item" key={booking.id}>
                    <div className="booking-seat-icon"><Armchair size={20} /></div>
                    <div className="booking-details">
                      <div className="booking-title-row">
                        <h3>Seat {booking.seatNumber || booking.seatId}</h3>
                        <span className={`badge ${isConfirmed ? 'success' : 'muted'}`}>
                          {booking.status === 'CANCELLED' ? 'Cancelled' : booking.status}
                        </span>
                      </div>
                      <div className="booking-meta">
                        <span><MapPin size={14} /> {booking.floor} floor</span>
                        <span><CalendarDays size={14} /> {formatDate(booking.bookingDate)}</span>
                        <span><Clock3 size={14} /> {formatTime(booking.startTime)}–{formatTime(booking.endTime)}</span>
                      </div>
                    </div>
                    {isConfirmed && (
                      <button
                        type="button"
                        className="danger cancel-button"
                        onClick={() => handleCancelBooking(booking)}
                        disabled={cancellingId === booking.id}
                      >
                        <X size={15} />
                        {cancellingId === booking.id ? 'Cancelling…' : 'Cancel'}
                      </button>
                    )}
                  </article>
                );
              })}
            </div>
          )}
        </section>
      )}

      {isStudent && selectedSeat && (
        <section className="panel-card booking-form-panel" aria-labelledby="booking-form-title">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Reserve your place</p>
              <h2 id="booking-form-title">Book seat {selectedSeat.seatNumber}</h2>
              <p className="form-context">{selectedSeat.floor} floor · Select your date and study hours.</p>
            </div>
            <button
              type="button"
              className="icon-button"
              aria-label="Close booking form"
              onClick={() => setSelectedSeat(null)}
            >
              <X size={18} />
            </button>
          </div>

          <form className="stack-form" onSubmit={handleBookingSubmit}>
            <div className="two-column-form">
              <label>
                Booking date
                <input
                  type="date"
                  value={form.bookingDate}
                  min={getToday()}
                  onChange={(event) => setForm((current) => ({ ...current, bookingDate: event.target.value }))}
                  required
                />
              </label>
              <label>
                Start time
                <input
                  type="time"
                  value={form.startTime}
                  onChange={(event) => setForm((current) => ({ ...current, startTime: event.target.value }))}
                  required
                />
              </label>
              <label>
                End time
                <input
                  type="time"
                  value={form.endTime}
                  min={form.startTime}
                  onChange={(event) => setForm((current) => ({ ...current, endTime: event.target.value }))}
                  required
                />
              </label>
            </div>
            <div className="card-actions">
              <button type="submit" className="primary-button" disabled={submitting}>
                <CalendarPlus2 size={17} />
                {submitting ? 'Booking…' : 'Confirm booking'}
              </button>
              <button type="button" className="secondary" onClick={() => setSelectedSeat(null)}>
                Keep browsing
              </button>
            </div>
          </form>
        </section>
      )}

      <section className="panel-card seat-browser">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Browse by floor</p>
            <h2>Choose a seat</h2>
          </div>
          <div className="seat-legend" aria-label="Seat status legend">
            <span><i className="legend-dot available" /> Available</span>
            <span><i className="legend-dot unavailable" /> Unavailable</span>
          </div>
        </div>
        <div className="toolbar-row">
          <div className="filter-group" aria-label="Floor filter">
            {floorOptions.map((floor) => (
              <button
                key={floor}
                type="button"
                className={`filter-chip ${selectedFloor === floor ? 'selected' : ''}`}
                onClick={() => setSelectedFloor(floor)}
              >
                {floor}
              </button>
            ))}
          </div>
          <div className="summary-box">{availableCount} of {seats.length} seats available</div>
        </div>
        <SeatGrid
          seats={filteredSeats}
          onBook={isStudent ? handleBookSeat : undefined}
        />
      </section>
    </div>
  );
}
