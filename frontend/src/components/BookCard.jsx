import { Link } from 'react-router-dom';
import { BookOpen, Armchair } from 'lucide-react';
import { borrowApi } from '../api/borrowApi';

/**
 * BookCard component – renders a book card.
 * Props:
 *   - book: the book object (id, title, author, genre, ...)
 *   - role: "STUDENT" | "ADMIN"
 *   - borrower?: { studentId: string; studentName: string } // only for ADMIN when the book is borrowed
 */
export default function BookCard({ book, role, borrower }) {
  // Student borrow handler (admin never sees this block)
  const handleBorrow = async () => {
    try {
      await borrowApi.requestBorrow(book.id);
      alert('Borrow request sent to admin.');
    } catch (e) {
      alert(e.message || 'Failed to request borrow.');
    }
  };

  return (
    <div className="book-card border rounded p-4 shadow-sm">
      <h3 className="text-lg font-medium">{book.title}</h3>
      <p className="text-sm text-gray-600">{book.author}</p>
      <p className="text-sm text-gray-500 italic">{book.genre}</p>

      {/* Admin view – show borrower info if present */}
      {role === 'ADMIN' && borrower && (
        <p className="mt-2 text-sm">
          <strong>Borrowed by:</strong> {borrower.studentName} (ID: {borrower.studentId})
        </p>
      )}

      {/* Action buttons – only for students */}
      {role === 'STUDENT' && (
        <div className="mt-3 flex gap-2">
          <button className="primary book-action-btn" onClick={handleBorrow}>
            <BookOpen size={14} /> Borrow
          </button>
          <Link to="/student/seats" className="secondary book-action-btn">
            <Armchair size={14} />
            <span>Reserve Seat to Read</span>
          </Link>
        </div>
      )}
    </div>
  );
}
