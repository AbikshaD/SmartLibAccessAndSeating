import { useEffect, useState, useMemo } from 'react';
import {
  BookOpen,
  Book,
  Layers,
  Search,
  MapPin,
  Calendar,
  Hash,
  CheckCircle2,
  Clock,
  Sparkles,
  X,
  Armchair
} from 'lucide-react';
import { bookApi } from '../api/bookApi';
import { borrowApi } from '../api/borrowApi';
import LoadingState from '../components/LoadingState';
import ErrorMessage from '../components/ErrorMessage';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const GENRE_METADATA = {
  'Computer Science': {
    color: '#2563eb',
    bg: '#eff6ff',
    border: '#bfdbfe',
    description: 'Software development, algorithms, system design, and AI.'
  },
  'Fiction': {
    color: '#7c3aed',
    bg: '#f5f3ff',
    border: '#ddd6fe',
    description: 'Dystopian epics, modern classics, literary drama, and adventures.'
  },
  'Science & Mathematics': {
    color: '#0891b2',
    bg: '#ecfeff',
    border: '#a5f3fc',
    description: 'Physics, cosmology, calculus, biology, and scientific thought.'
  },
  'History & Philosophy': {
    color: '#d97706',
    bg: '#fffbeb',
    border: '#fde68a',
    description: 'Human evolution, ancient thought, civilization, and stoic wisdom.'
  },
  'Business & Management': {
    color: '#059669',
    bg: '#ecfdf5',
    border: '#a7f3d0',
    description: 'Entrepreneurship, economic psychology, strategy, and leadership.'
  },
  'Literature & Classics': {
    color: '#db2777',
    bg: '#fdf2f8',
    border: '#fbcfe8',
    description: 'Timeless literary masterpieces, world classics, and poetry.'
  }
};

const formatDate = (date) => {
  if (!date) return '—';
  const [year, month, day] = date.split('-').map(Number);
  return new Date(year, month - 1, day).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  });
};

export default function StudentBooks() {
  const { role } = useAuth();
  const [genres, setGenres] = useState([]);
  const [allBooks, setAllBooks] = useState([]);
  const [myBorrows, setMyBorrows] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;

    const loadData = async () => {
      try {
        setLoading(true);
        const [genresData, booksData, borrowData] = await Promise.all([
          bookApi.getGenres(),
          bookApi.getAllBooks(),
          role === 'STUDENT' ? borrowApi.getMyBorrows() : Promise.resolve([])
        ]);
        if (!active) return;
        setGenres(genresData);
        setAllBooks(booksData);
        setMyBorrows(borrowData || []);
      } catch (err) {
        if (!active) return;
        setError(err.message || 'Failed to load library catalog.');
      } finally {
        if (active) setLoading(false);
      }
    };

    loadData();
    return () => { active = false; };
  }, [role]);

  const requestBorrow = async (bookId) => {
    try {
      const borrow = await borrowApi.requestBorrow(bookId);
      setMyBorrows((currentBorrows) => [borrow, ...currentBorrows]);
      alert('Borrow request sent to admin.');
    } catch (err) {
      alert(err.message || 'Failed to request borrow.');
    }
  };

  // When a search term is entered, filter genres and books that match the genre name
  const searchedGenre = searchTerm.trim().toLowerCase();

  const matchedGenres = useMemo(() => {
    if (!searchedGenre) return genres;
    return genres.filter((g) => g.genre.toLowerCase().includes(searchedGenre));
  }, [searchedGenre, genres]);

  // For each matched genre, get its books
  const booksByGenre = useMemo(() => {
    return matchedGenres.map((g) => ({
      genre: g,
      books: allBooks.filter((b) => b.genre.toLowerCase() === g.genre.toLowerCase())
    }));
  }, [matchedGenres, allBooks]);

  const totalCopies = allBooks.reduce((acc, b) => acc + (b.availableCopies || 0), 0);

  if (loading) return <LoadingState message="Loading book collection and genres..." />;

  return (
    <div className="page-container">
      {/* Header */}
      <header className="page-header">
        <div>
          <p className="eyebrow">Library Catalog</p>
          <h1>Books &amp; Genres</h1>
          <p className="page-subtitle">
            Search a genre to explore available titles and check shelf locations.
          </p>
        </div>
      </header>

      <ErrorMessage message={error} />

      {/* Stats */}
      <div className="stats-grid">
        <div className="stat-card">
          <span><Layers size={16} /> Total Genres</span>
          <strong>{genres.length}</strong>
        </div>
        <div className="stat-card accent">
          <span><BookOpen size={16} /> Total Books</span>
          <strong>{allBooks.length}</strong>
        </div>
        <div className="stat-card">
          <span><Sparkles size={16} /> Copies Available</span>
          <strong>{totalCopies}</strong>
        </div>
      </div>

      {/* Search Bar */}
      <section className="panel-card">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Search</p>
            <h2>Find a Genre</h2>
          </div>
          {searchTerm && (
            <button
              type="button"
              className="secondary"
              onClick={() => setSearchTerm('')}
              style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}
            >
              <X size={14} />
              <span>Clear</span>
            </button>
          )}
        </div>

        <div className="search-wrap" style={{ marginTop: '0.75rem' }}>
          <Search size={16} className="search-icon" />
          <input
            type="text"
            className="search-input"
            placeholder="Type a genre name, e.g. Fiction, Science, History..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            autoFocus
          />
          {searchTerm && (
            <button
              type="button"
              className="clear-search-btn"
              onClick={() => setSearchTerm('')}
            >
              ✕
            </button>
          )}
        </div>

        {searchTerm && (
          <p className="search-status-text" style={{ marginTop: '0.5rem' }}>
            {matchedGenres.length === 0
              ? `No genres found matching "${searchTerm}".`
              : `Showing ${matchedGenres.length} genre${matchedGenres.length > 1 ? 's' : ''} matching "${searchTerm}".`}
          </p>
        )}
      </section>

      {/* Genres + Books — one section per genre */}
      {booksByGenre.length === 0 && searchTerm ? (
        <div className="panel-card empty-catalog-state">
          <Book size={44} className="empty-icon" />
          <h3>No genres found</h3>
          <p>Try a different keyword or clear the search to see all genres.</p>
          <button type="button" className="secondary" onClick={() => setSearchTerm('')}>
            Show All Genres
          </button>
        </div>
      ) : (
        booksByGenre.map(({ genre: genreItem, books }) => {
          const meta = GENRE_METADATA[genreItem.genre] || {
            color: '#4f46e5',
            bg: '#eef2ff',
            border: '#c7d2fe',
            description: 'Collection of curated titles in this category.'
          };

          return (
            <section key={genreItem.genre} className="panel-card" style={{ marginBottom: '1.5rem' }}>
              {/* Genre Header */}
              <div className="section-heading" style={{ marginBottom: '1rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                  <div
                    className="genre-icon-pill"
                    style={{ backgroundColor: meta.bg, color: meta.color }}
                  >
                    <Book size={20} />
                  </div>
                  <div>
                    <p className="eyebrow" style={{ color: meta.color }}>{meta.description}</p>
                    <h2 style={{ margin: 0 }}>{genreItem.genre}</h2>
                  </div>
                </div>
                <span
                  className="genre-count-badge"
                  style={{
                    backgroundColor: meta.bg,
                    color: meta.color,
                    borderColor: meta.border
                  }}
                >
                  {genreItem.bookCount} {genreItem.bookCount === 1 ? 'Book' : 'Books'}
                </span>
              </div>

              {/* Books Grid for this genre */}
              {books.length === 0 ? (
                <div className="empty-state">No books available in this genre at the moment.</div>
              ) : (
                <div className="books-grid">
                  {books.map((book) => {
                    const hasCopies = (book.availableCopies || 0) > 0;
                    const currentBorrow = role === 'STUDENT'
                      ? myBorrows.find((borrow) => borrow.bookId === book.id && ['APPROVED', 'OVERDUE'].includes(borrow.status))
                        || myBorrows.find((borrow) => borrow.bookId === book.id && borrow.status === 'PENDING')
                      : null;
                    const isBorrowed = currentBorrow?.status === 'APPROVED' || currentBorrow?.status === 'OVERDUE';
                    const bookMeta = GENRE_METADATA[book.genre] || {
                      color: '#2563eb',
                      bg: '#eff6ff',
                      border: '#bfdbfe'
                    };

                    return (
                      <div key={book.id || book.title} className="book-card">
                        <div className="book-card-header">
                          <span
                            className="book-genre-pill"
                            style={{
                              backgroundColor: bookMeta.bg,
                              color: bookMeta.color,
                              borderColor: bookMeta.border
                            }}
                          >
                            {book.genre}
                          </span>
                          <span className={`badge ${hasCopies ? 'success' : 'warning'}`}>
                            {hasCopies ? (
                              <>
                                <CheckCircle2 size={12} style={{ marginRight: '4px' }} />
                                {book.availableCopies} of {book.totalCopies} Available
                              </>
                            ) : (
                              <>
                                <Clock size={12} style={{ marginRight: '4px' }} />
                                Checked Out
                              </>
                            )}
                          </span>
                        </div>

                        <h3 className="book-title">{book.title}</h3>
                        <p className="book-author">by {book.author}</p>

                        {book.description && (
                          <p className="book-description">{book.description}</p>
                        )}

                        <div className="book-details-list">
                          {book.shelfLocation && (
                            <div className="book-detail-item">
                              <MapPin size={14} className="detail-icon" />
                              <span><strong>Location:</strong> {book.shelfLocation}</span>
                            </div>
                          )}
                          {book.publicationYear && (
                            <div className="book-detail-item">
                              <Calendar size={14} className="detail-icon" />
                              <span><strong>Published:</strong> {book.publicationYear}</span>
                            </div>
                          )}
                          {book.isbn && (
                            <div className="book-detail-item">
                              <Hash size={14} className="detail-icon" />
                              <span><strong>ISBN:</strong> {book.isbn}</span>
                            </div>
                          )}
                        </div>

                        {role === 'STUDENT' && (
                          <div className="book-card-actions">
                            {currentBorrow ? (
                              <div className="book-detail-item" role="status">
                                {isBorrowed ? (
                                  <span><strong>Borrowed</strong> · Due {formatDate(currentBorrow.dueDate)}</span>
                                ) : (
                                  <span><strong>Borrow request pending</strong></span>
                                )}
                              </div>
                            ) : (
                              <button
                                className="primary book-action-btn"
                                onClick={() => requestBorrow(book.id)}
                                disabled={!hasCopies}
                                style={{ marginRight: '8px' }}
                              >
                                <BookOpen size={14} /> Borrow
                              </button>
                            )}
                            <Link to="/student/seats" className="secondary book-action-btn">
                              <Armchair size={14} />
                              <span>Reserve Seat to Read</span>
                            </Link>
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              )}
            </section>
          );
        })
      )}
    </div>
  );
}
