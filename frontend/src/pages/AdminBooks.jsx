import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import BookCard from '../components/BookCard';
import { bookApi } from '../api/bookApi';
import LoadingState from '../components/LoadingState';
import ErrorMessage from '../components/ErrorMessage';

export default function AdminBooks() {
  const [books, setBooks] = useState([]);
  const [borrows, setBorrows] = useState([]); // admin view of all borrow records
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    const load = async () => {
      try {
        const [booksData, adminBorrows] = await Promise.all([
          bookApi.getAllBooks(),
          // admin endpoint returns all borrow records (including student info)
          fetch(`${import.meta.env.VITE_API_URL}/borrows`, {
            headers: {
              Authorization: `Bearer ${localStorage.getItem('smartlib_token')}`,
            },
          }).then((r) => r.json()),
        ]);
        if (active) {
          setBooks(booksData);
          setBorrows(adminBorrows);
        }
      } catch (e) {
        if (active) setError(e.message || 'Failed to load admin books.');
      } finally {
        if (active) setLoading(false);
      }
    };
    load();
    return () => {
      active = false;
    };
  }, []);

  if (loading) return <LoadingState message="Loading books…" />;
  if (error) return <ErrorMessage message={error} />;

  const getBorrower = (bookId) =>
    borrows.find((b) => b.bookId === bookId && b.status === 'APPROVED');

  return (
    <div className="page-container">
      <header className="page-header">
        <h1>Library Books (Admin)</h1>
        <Link to="/admin/dashboard" className="secondary-button">
          ← Back to Dashboard
        </Link>
      </header>

      <section className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {books.map((book) => {
          const borrower = getBorrower(book.id);
          return (
            <BookCard
              key={book.id}
              book={book}
              role="ADMIN"
              borrower={
                borrower
                  ? { studentId: borrower.studentId, studentName: borrower.studentName }
                  : null
              }
            />
          );
        })}
      </section>
    </div>
  );
}
