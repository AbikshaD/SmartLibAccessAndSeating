import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Armchair,
  BookOpen,
  ArrowRight,
  BookMarked,
  Megaphone,
  Clock,
  AlertTriangle,
  Info,
  Wrench,
  ChevronDown,
  ChevronUp,
  X
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { seatApi } from '../api/seatApi';
import { bookApi } from '../api/bookApi';
import { announcementApi } from '../api/announcementApi';
import LoadingState from '../components/LoadingState';
import ErrorMessage from '../components/ErrorMessage';

const ANNOUNCEMENT_ICONS = {
  CLOSURE: Clock,
  URGENT: AlertTriangle,
  MAINTENANCE: Wrench,
  GENERAL: Info,
  EVENT: Megaphone
};

export default function StudentDashboard() {
  const { user } = useAuth();
  const [allSeats, setAllSeats] = useState([]);
  const [availableSeats, setAvailableSeats] = useState([]);
  const [genres, setGenres] = useState([]);
  const [totalBooks, setTotalBooks] = useState(0);
  const [announcements, setAnnouncements] = useState([]);
  const [dismissedAnnouncements, setDismissedAnnouncements] = useState([]);
  const [showAllAnnouncements, setShowAllAnnouncements] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;

    const loadDashboard = async () => {
      try {
        const [seats, available, genresData, booksData, announcementsData] = await Promise.all([
          seatApi.getAllSeats(),
          seatApi.getAvailableSeats(),
          bookApi.getGenres(),
          bookApi.getAllBooks(),
          announcementApi.getActiveAnnouncements()
        ]);
        if (!active) return;
        setAllSeats(seats);
        setAvailableSeats(available);
        setGenres(genresData || []);
        setTotalBooks((booksData && booksData.length) || 0);
        setAnnouncements(announcementsData || []);
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

  const handleDismiss = (id) => {
    setDismissedAnnouncements((prev) => [...prev, id]);
  };

  const visibleAnnouncements = announcements.filter(
    (a) => !dismissedAnnouncements.includes(a.id)
  );

  if (loading) return <LoadingState message="Loading dashboard..." />;

  const primaryAnnouncement = visibleAnnouncements[0];
  const otherAnnouncements = visibleAnnouncements.slice(1);

  return (
    <div className="page-container">
      <header className="page-header">
        <div>
          <p className="eyebrow">Student dashboard</p>
          <h1>Welcome, {user?.name || user?.studentId}</h1>
          <p className="page-subtitle">
            Manage your study seats, discover library book genres, and check official notices.
          </p>
        </div>
        <div className="inline-actions">
          <Link to="/student/books" className="primary-button link-button" style={{ marginTop: 0 }}>
            <BookOpen size={16} />
            <span>Open Books Catalog</span>
          </Link>
        </div>
      </header>

      <ErrorMessage message={error} />

      {/* Official Library Announcements Banner for Students */}
      {visibleAnnouncements.length > 0 && primaryAnnouncement && (
        <section
          className={`student-announcement-banner ${
            primaryAnnouncement.priority === 'HIGH' || primaryAnnouncement.type === 'CLOSURE'
              ? 'alert-urgent'
              : 'alert-info'
          }`}
          role="region"
          aria-label="Library Announcements"
        >
          <div className="announcement-banner-inner">
            <div className="announcement-banner-icon-wrap">
              {(() => {
                const Icon = ANNOUNCEMENT_ICONS[primaryAnnouncement.type] || Megaphone;
                return <Icon size={24} />;
              })()}
            </div>

            <div className="announcement-banner-body">
              <div className="announcement-banner-heading-row">
                <span className="announcement-tag">
                  {primaryAnnouncement.type === 'CLOSURE' ? 'Library Closure Notice' : 'Official Announcement'}
                </span>
                <span className="announcement-date-tag">
                  {primaryAnnouncement.createdAt
                    ? new Date(primaryAnnouncement.createdAt).toLocaleDateString(undefined, {
                        month: 'short',
                        day: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit'
                      })
                    : 'Recent'}
                </span>
              </div>

              <h2 className="announcement-headline">{primaryAnnouncement.title}</h2>
              <p className="announcement-text">{primaryAnnouncement.content}</p>

              {otherAnnouncements.length > 0 && (
                <button
                  type="button"
                  className="announcement-toggle-btn"
                  onClick={() => setShowAllAnnouncements((prev) => !prev)}
                >
                  <span>
                    {showAllAnnouncements
                      ? 'Hide additional notices'
                      : `View ${otherAnnouncements.length} more ${otherAnnouncements.length === 1 ? 'notice' : 'notices'}`}
                  </span>
                  {showAllAnnouncements ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
                </button>
              )}

              {/* Collapsible Additional Announcements */}
              {showAllAnnouncements && otherAnnouncements.length > 0 && (
                <div className="additional-announcements-list">
                  {otherAnnouncements.map((extra) => {
                    const ExtraIcon = ANNOUNCEMENT_ICONS[extra.type] || Info;
                    return (
                      <div key={extra.id} className="extra-announcement-item">
                        <div className="extra-ann-icon">
                          <ExtraIcon size={16} />
                        </div>
                        <div className="extra-ann-details">
                          <div className="extra-ann-header">
                            <strong>{extra.title}</strong>
                            <span className="extra-ann-date">
                              {extra.createdAt ? new Date(extra.createdAt).toLocaleDateString() : ''}
                            </span>
                          </div>
                          <p>{extra.content}</p>
                        </div>
                        <button
                          type="button"
                          className="dismiss-extra-btn"
                          onClick={() => handleDismiss(extra.id)}
                          title="Dismiss"
                        >
                          <X size={14} />
                        </button>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            <button
              type="button"
              className="dismiss-banner-btn"
              onClick={() => handleDismiss(primaryAnnouncement.id)}
              title="Dismiss announcement"
              aria-label="Dismiss announcement"
            >
              <X size={18} />
            </button>
          </div>
        </section>
      )}

      {/* Primary Stats Grid */}
      <div className="stats-grid">
        <div className="stat-card">
          <span>
            <Armchair size={15} /> Total seats
          </span>
          <strong>{allSeats.length}</strong>
        </div>
        <div className="stat-card accent">
          <span>
            <Armchair size={15} /> Available seats
          </span>
          <strong>{availableSeats.length}</strong>
        </div>
        <div className="stat-card">
          <span>Occupancy</span>
          <strong>{allSeats.length ? `${Math.round(((allSeats.length - availableSeats.length) / allSeats.length) * 100)}%` : '0%'}</strong>
        </div>
        <Link to="/student/books" className="stat-card stat-card-link" title="Click to view Books">
          <span>
            <BookOpen size={15} /> Books Catalog
          </span>
          <strong>{totalBooks} Books</strong>
          <small className="stat-card-hint">Across {genres.length} genres →</small>
        </Link>
      </div>

      {/* Featured Books Feature Panel */}
      <section className="panel-card dashboard-books-panel">
        <div className="dashboard-books-header">
          <div className="dashboard-books-badge">
            <BookOpen size={24} />
          </div>
          <div>
            <p className="eyebrow">Library Resources</p>
            <h2>Books & Genre Catalog</h2>
            <p className="dashboard-books-desc">
              Browse our diverse collection of physical library books organized by genre. Click any genre to see the exact number of books and check real-time shelf availability.
            </p>
          </div>
        </div>

        <div className="dashboard-genres-preview">
          <span className="eyebrow" style={{ display: 'block', marginBottom: '8px' }}>
            Popular Genres:
          </span>
          <div className="filter-group">
            {genres.map((g) => (
              <Link
                key={g.genre}
                to={`/student/books?genre=${encodeURIComponent(g.genre)}`}
                className="filter-chip dashboard-genre-pill"
              >
                <span>{g.genre}</span>
                <span className="genre-pill-count">{g.bookCount}</span>
              </Link>
            ))}
          </div>
        </div>

        <div className="dashboard-books-footer">
          <Link to="/student/books" className="primary-button link-button" style={{ marginTop: 0 }}>
            <BookMarked size={16} />
            <span>Explore All Books & Genres</span>
            <ArrowRight size={16} />
          </Link>
          <Link to="/student/seats" className="secondary link-button" style={{ marginTop: 0 }}>
            <Armchair size={16} />
            <span>Book a Seat to Study</span>
          </Link>
        </div>
      </section>

      {/* Student Profile Quick View */}
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
