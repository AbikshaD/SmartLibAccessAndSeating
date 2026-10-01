import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { seatApi } from '../api/seatApi';
import LoadingState from '../components/LoadingState';
import ErrorMessage from '../components/ErrorMessage';

export default function StudentDashboard() {
  const { user } = useAuth();
  const [allSeats, setAllSeats] = useState([]);
  const [availableSeats, setAvailableSeats] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;

    const loadDashboard = async () => {
      try {
        const [seats, available] = await Promise.all([seatApi.getAllSeats(), seatApi.getAvailableSeats()]);
        if (!active) return;
        setAllSeats(seats);
        setAvailableSeats(available);
      } catch (err) {
        if (!active) return;
        setError(err.message || 'Unable to load dashboard data.');
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    };

    loadDashboard();

    return () => {
      active = false;
    };
  }, []);

  if (loading) return <LoadingState message="Loading dashboard..." />;

  return (
    <div className="page-container">
      <header className="page-header">
        <div>
          <p className="eyebrow">Student dashboard</p>
          <h1>Welcome, {user?.name || user?.studentId}</h1>
        </div>
      </header>

      <ErrorMessage message={error} />

      <div className="stats-grid">
        <div className="stat-card">
          <span>Total seats</span>
          <strong>{allSeats.length}</strong>
        </div>
        <div className="stat-card accent">
          <span>Available</span>
          <strong>{availableSeats.length}</strong>
        </div>
        <div className="stat-card">
          <span>Occupancy</span>
          <strong>{allSeats.length ? `${Math.round(((allSeats.length - availableSeats.length) / allSeats.length) * 100)}%` : '0%'}</strong>
        </div>
      </div>

      <section className="panel-card">
        <h2>Library quick view</h2>
        <div className="quick-grid">
          <div>
            <p className="eyebrow">Student ID</p>
            <strong>{user?.studentId}</strong>
          </div>
          <div>
            <p className="eyebrow">Email</p>
            <strong>{user?.email}</strong>
          </div>
          <div>
            <p className="eyebrow">Role</p>
            <strong>{user?.role}</strong>
          </div>
        </div>
      </section>
    </div>
  );
}
