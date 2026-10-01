import { useEffect, useState } from 'react';
import { seatApi } from '../api/seatApi';
import { userApi } from '../api/userApi';
import ErrorMessage from '../components/ErrorMessage';
import LoadingState from '../components/LoadingState';

export default function AdminDashboard() {
  const [users, setUsers] = useState([]);
  const [seats, setSeats] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;

    const loadData = async () => {
      try {
        const [usersResponse, seatsResponse] = await Promise.all([userApi.getAllUsers(), seatApi.getAllSeats()]);
        if (!active) return;
        setUsers(usersResponse || []);
        setSeats(seatsResponse || []);
      } catch (err) {
        if (!active) return;
        setError(err.message || 'Unable to load admin data.');
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    };

    loadData();

    return () => {
      active = false;
    };
  }, []);

  if (loading) return <LoadingState message="Loading admin overview..." />;

  return (
    <div className="page-container">
      <header className="page-header">
        <div>
          <p className="eyebrow">Admin dashboard</p>
          <h1>System Overview</h1>
        </div>
      </header>

      <ErrorMessage message={error} />

      <div className="stats-grid">
        <div className="stat-card">
          <span>Total users</span>
          <strong>{users.length}</strong>
        </div>
        <div className="stat-card accent">
          <span>Total seats</span>
          <strong>{seats.length}</strong>
        </div>
        <div className="stat-card">
          <span>Available seats</span>
          <strong>{seats.filter((seat) => seat.status === 'AVAILABLE').length}</strong>
        </div>
      </div>
    </div>
  );
}
