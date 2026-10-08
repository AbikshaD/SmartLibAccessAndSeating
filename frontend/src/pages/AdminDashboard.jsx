import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Megaphone, Users, Armchair, PlusCircle, ArrowRight } from 'lucide-react';
import { seatApi } from '../api/seatApi';
import { userApi } from '../api/userApi';
import { announcementApi } from '../api/announcementApi';
import ErrorMessage from '../components/ErrorMessage';
import LoadingState from '../components/LoadingState';

export default function AdminDashboard() {
  const [users, setUsers] = useState([]);
  const [seats, setSeats] = useState([]);
  const [announcements, setAnnouncements] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;

    const loadData = async () => {
      try {
        const [usersResponse, seatsResponse, annResponse] = await Promise.all([
          userApi.getAllUsers(),
          seatApi.getAllSeats(),
          announcementApi.getAllAnnouncements()
        ]);
        if (!active) return;
        setUsers(usersResponse || []);
        setSeats(seatsResponse || []);
        setAnnouncements(annResponse || []);
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

  const closuresCount = announcements.filter((a) => a.type === 'CLOSURE' || a.type === 'URGENT').length;

  return (
    <div className="page-container">
      <header className="page-header">
        <div>
          <p className="eyebrow">Admin dashboard</p>
          <h1>System Overview</h1>
        </div>
        <div className="inline-actions">
          <Link to="/admin/announcements" className="primary-button link-button" style={{ marginTop: 0 }}>
            <Megaphone size={16} />
            <span>Post Announcement</span>
          </Link>
        </div>
      </header>

      <ErrorMessage message={error} />

      <div className="stats-grid">
        <div className="stat-card">
          <span>
            <Users size={15} /> Total users
          </span>
          <strong>{users.length}</strong>
        </div>
        <div className="stat-card accent">
          <span>
            <Armchair size={15} /> Total seats
          </span>
          <strong>{seats.length}</strong>
        </div>
        <div className="stat-card">
          <span>Available seats</span>
          <strong>{seats.filter((seat) => seat.status === 'AVAILABLE').length}</strong>
        </div>
        <Link to="/admin/announcements" className="stat-card stat-card-link" title="Manage Announcements">
          <span>
            <Megaphone size={15} /> Announcements
          </span>
          <strong>{announcements.length} Posted</strong>
          {closuresCount > 0 && (
            <small className="stat-card-hint" style={{ color: '#b91c1c' }}>
              {closuresCount} active closure/urgent alert(s) →
            </small>
          )}
        </Link>
      </div>

      {/* Quick Announcement Control Panel */}
      <section className="panel-card dashboard-books-panel">
        <div className="dashboard-books-header">
          <div className="dashboard-books-badge" style={{ background: '#fee2e2', color: '#b91c1c' }}>
            <Megaphone size={24} />
          </div>
          <div>
            <p className="eyebrow">Student Broadcast</p>
            <h2>Library Announcements System</h2>
            <p className="dashboard-books-desc">
              Broadcast sudden library closures, modified operating hours, maintenance alerts, or urgent notices directly to all student dashboards. Only admins can create and manage announcements.
            </p>
          </div>
        </div>

        <div className="dashboard-books-footer">
          <Link to="/admin/announcements" className="primary-button link-button" style={{ marginTop: 0 }}>
            <PlusCircle size={16} />
            <span>Post New Announcement</span>
            <ArrowRight size={16} />
          </Link>
        </div>
      </section>
    </div>
  );
}
