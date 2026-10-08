import { useEffect, useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import {
  BookOpen,
  Book,
  Bookmark,
  Layers,
  Search,
  MapPin,
  Calendar,
  Hash,
  CheckCircle2,
  Clock,
  RotateCcw,
  Sparkles,
  ArrowRight,
  Armchair
} from 'lucide-react';
import { bookApi } from '../api/bookApi';
import LoadingState from '../components/LoadingState';
import ErrorMessage from '../components/ErrorMessage';

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

export default function StudentBooks() {
  const [searchParams, setSearchParams] = useSearchParams();
  const selectedGenre = searchParams.get('genre') || '';

  const [genres, setGenres] = useState([]);
  const [allBooks, setAllBooks] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;

    const loadData = async () => {
      try {
        setLoading(true);
        const [genresData, booksData] = await Promise.all([
          bookApi.getGenres(),
          bookApi.getAllBooks()
        ]);

        if (!active) return;
        setGenres(genresData);
        setAllBooks(booksData);
      } catch (err) {
        if (!active) return;
        setError(err.message || 'Failed to load library catalog.');
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

  const handleSelectGenre = (genreName) => {
    if (selectedGenre.toLowerCase() === genreName.toLowerCase()) {
      // Toggle off to view all
      setSearchParams({});
    } else {
      setSearchParams({ genre: genreName });
    }
    setSearchTerm('');
  };

  const handleClearGenre = () => {
    setSearchParams({});
    setSearchTerm('');
  };

  // Filter books according to selected genre and search
  const booksForSelectedGenre = !selectedGenre
    ? allBooks
    : allBooks.filter(
        (b) => b.genre.toLowerCase() === selectedGenre.toLowerCase()
      );

  const displayedBooks = !searchTerm.trim()
    ? booksForSelectedGenre
    : booksForSelectedGenre.filter((b) => {
        const term = searchTerm.toLowerCase();
        return (
          b.title.toLowerCase().includes(term) ||
          b.author.toLowerCase().includes(term) ||
          (b.isbn && b.isbn.toLowerCase().includes(term)) ||
          (b.description && b.description.toLowerCase().includes(term))
        );
      });

  // Total statistics
  const totalBooksCount = allBooks.length;
  const totalGenresCount = genres.length;

  const selectedGenreBookCount = selectedGenre
    ? booksForSelectedGenre.length
    : totalBooksCount;

  if (loading) {
    return <LoadingState message="Loading book collection and genres..." />;
  }

  return (
    <div className="page-container">
      {/* Header */}
      <header className="page-header">
        <div>
          <p className="eyebrow">Library Catalog</p>
          <h1>Books & Genres</h1>
          <p className="page-subtitle">
            Explore diverse book genres, discover available titles, and check shelf locations.
          </p>
        </div>
        <div className="inline-actions">
          <Link to="/student/seats" className="secondary link-button" style={{ marginTop: 0 }}>
            <Armchair size={16} />
            <span>Book a Study Seat</span>
          </Link>
        </div>
      </header>

      <ErrorMessage message={error} />

      {/* Top Stats Overview */}
      <div className="stats-grid">
        <div className="stat-card">
          <span>
            <Layers size={16} /> Total Genres
          </span>
          <strong>{totalGenresCount}</strong>
        </div>

        <div className="stat-card accent">
          <span>
            <BookOpen size={16} /> Total Books
          </span>
          <strong>{totalBooksCount}</strong>
        </div>

        <div className="stat-card">
          <span>
            <Bookmark size={16} />
            {selectedGenre ? `Books in "${selectedGenre}"` : 'Active Selection'}
          </span>
          <strong>
            {selectedGenre ? `${selectedGenreBookCount} Books` : 'All Genres'}
          </strong>
        </div>

        <div className="stat-card">
          <span>
            <Sparkles size={16} /> Total Copies Available
          </span>
          <strong>
            {allBooks.reduce((acc, b) => acc + (b.availableCopies || 0), 0)}
          </strong>
        </div>
      </div>

      {/* Genre Categories Section */}
      <section className="panel-card">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Browse by Category</p>
            <h2>Book Genres</h2>
          </div>
          {selectedGenre && (
            <button
              type="button"
              className="secondary"
              onClick={handleClearGenre}
              style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}
            >
              <RotateCcw size={14} />
              <span>Show All Genres</span>
            </button>
          )}
        </div>

        <p className="genre-instruction">
          Click any genre below to view the total number of books and explore available titles:
        </p>

        <div className="genre-grid">
          {genres.map((item) => {
            const isSelected =
              selectedGenre.toLowerCase() === item.genre.toLowerCase();
            const meta = GENRE_METADATA[item.genre] || {
              color: '#4f46e5',
              bg: '#eef2ff',
              border: '#c7d2fe',
              description: 'Collection of curated titles in this category.'
            };

            return (
              <div
                key={item.genre}
                className={`genre-card ${isSelected ? 'selected' : ''}`}
                onClick={() => handleSelectGenre(item.genre)}
                role="button"
                tabIndex={0}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    handleSelectGenre(item.genre);
                  }
                }}
              >
                <div className="genre-card-header">
                  <div
                    className="genre-icon-pill"
                    style={{ backgroundColor: meta.bg, color: meta.color }}
                  >
                    <Book size={20} />
                  </div>
                  {/* Genre Book Count Badge */}
                  <span
                    className="genre-count-badge"
                    style={{
                      backgroundColor: isSelected ? '#1d4ed8' : meta.bg,
                      color: isSelected ? '#ffffff' : meta.color,
                      borderColor: meta.border
                    }}
                  >
                    {item.bookCount} {item.bookCount === 1 ? 'Book' : 'Books'}
                  </span>
                </div>

                <h3 className="genre-card-title">{item.genre}</h3>
                <p className="genre-card-desc">{meta.description}</p>

                <div className="genre-card-footer">
                  <span className="genre-click-action">
                    {isSelected ? 'Active Genre (Click to reset)' : 'Click to view books'}
                  </span>
                  <ArrowRight
                    size={16}
                    className={`genre-arrow ${isSelected ? 'rotated' : ''}`}
                  />
                </div>
              </div>
            );
          })}
        </div>
      </section>

      {/* Selected Genre Banner & Number of Books Announcement */}
      {selectedGenre && (
        <div className="genre-count-banner" role="region" aria-label="Genre Summary">
          <div className="genre-banner-icon">
            <BookOpen size={28} />
          </div>
          <div className="genre-banner-text">
            <span className="genre-banner-tag">Selected Genre</span>
            <h2>{selectedGenre}</h2>
            <p>
              There are <strong className="count-highlight">{booksForSelectedGenre.length}</strong>{' '}
              {booksForSelectedGenre.length === 1 ? 'book' : 'books'} currently cataloged in this genre,
              with <strong>{booksForSelectedGenre.reduce((sum, b) => sum + (b.availableCopies || 0), 0)}</strong> copies ready on library shelves.
            </p>
          </div>
          <button
            type="button"
            className="secondary genre-banner-clear"
            onClick={handleClearGenre}
          >
            Clear Filter
          </button>
        </div>
      )}

      {/* Books List Section */}
      <section className="panel-card">
        <div className="toolbar-row">
          <div>
            <p className="eyebrow">
              {selectedGenre ? `Genre: ${selectedGenre}` : 'All Library Holdings'}
            </p>
            <h2>
              {selectedGenre
                ? `${selectedGenre} Collection (${booksForSelectedGenre.length} ${booksForSelectedGenre.length === 1 ? 'Book' : 'Books'})`
                : `All Books (${allBooks.length} Books)`}
            </h2>
          </div>

          <div className="search-wrap">
            <Search size={16} className="search-icon" />
            <input
              type="text"
              className="search-input"
              placeholder={
                selectedGenre
                  ? `Search inside ${selectedGenre}...`
                  : 'Search books by title, author, or ISBN...'
              }
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
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
        </div>

        {searchTerm && (
          <p className="search-status-text">
            Found <strong>{displayedBooks.length}</strong> {displayedBooks.length === 1 ? 'book' : 'books'} matching &quot;{searchTerm}&quot;
            {selectedGenre && ` in ${selectedGenre}`}.
          </p>
        )}

        {displayedBooks.length === 0 ? (
          <div className="empty-catalog-state">
            <Book size={44} className="empty-icon" />
            <h3>No books found</h3>
            <p>
              {searchTerm
                ? 'Try adjusting your search keywords or clear the filter.'
                : 'No books available for this genre at the moment.'}
            </p>
            {searchTerm && (
              <button
                type="button"
                className="secondary"
                onClick={() => setSearchTerm('')}
              >
                Clear Search
              </button>
            )}
          </div>
        ) : (
          <div className="books-grid">
            {displayedBooks.map((book) => {
              const hasCopies = (book.availableCopies || 0) > 0;
              const meta = GENRE_METADATA[book.genre] || {
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
                        backgroundColor: meta.bg,
                        color: meta.color,
                        borderColor: meta.border
                      }}
                      onClick={() => handleSelectGenre(book.genre)}
                      title={`Filter by ${book.genre}`}
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
                        <span>
                          <strong>Location:</strong> {book.shelfLocation}
                        </span>
                      </div>
                    )}
                    {book.publicationYear && (
                      <div className="book-detail-item">
                        <Calendar size={14} className="detail-icon" />
                        <span>
                          <strong>Published:</strong> {book.publicationYear}
                        </span>
                      </div>
                    )}
                    {book.isbn && (
                      <div className="book-detail-item">
                        <Hash size={14} className="detail-icon" />
                        <span>
                          <strong>ISBN:</strong> {book.isbn}
                        </span>
                      </div>
                    )}
                  </div>

                  <div className="book-card-actions">
                    <Link to="/student/seats" className="secondary book-action-btn">
                      <Armchair size={14} />
                      <span>Reserve Seat to Read</span>
                    </Link>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
}
