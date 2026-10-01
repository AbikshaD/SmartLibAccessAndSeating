import { useEffect, useMemo, useState } from 'react';
import { seatApi } from '../api/seatApi';
import ErrorMessage from '../components/ErrorMessage';
import LoadingState from '../components/LoadingState';
import SeatGrid from '../components/SeatGrid';

export default function StudentSeats() {
  const [seats, setSeats] = useState([]);
  const [selectedFloor, setSelectedFloor] = useState('All');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;

    const loadSeats = async () => {
      try {
        const response = await seatApi.getAllSeats();
        if (!active) return;
        setSeats(response || []);
      } catch (err) {
        if (!active) return;
        setError(err.message || 'Unable to load seats.');
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    };

    loadSeats();

    return () => {
      active = false;
    };
  }, []);

  const floorOptions = useMemo(() => {
    const uniqueFloors = [...new Set(seats.map((seat) => seat.floor).filter(Boolean))];
    return ['All', ...uniqueFloors];
  }, [seats]);

  const filteredSeats = useMemo(() => {
    if (selectedFloor === 'All') return seats;
    return seats.filter((seat) => seat.floor === selectedFloor);
  }, [selectedFloor, seats]);

  const availableCount = seats.filter((seat) => seat.status === 'AVAILABLE').length;

  if (loading) return <LoadingState message="Loading seats..." />;

  return (
    <div className="page-container">
      <header className="page-header">
        <div>
          <p className="eyebrow">Seat directory</p>
          <h1>Library Seats</h1>
        </div>
      </header>

      <ErrorMessage message={error} />

      <section className="panel-card">
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
          <div className="summary-box">
            Available: {availableCount} / {seats.length}
          </div>
        </div>
      </section>

      <SeatGrid seats={filteredSeats} />
    </div>
  );
}
