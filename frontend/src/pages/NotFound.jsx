import { Link } from 'react-router-dom';

export default function NotFound() {
  return (
    <div className="auth-shell">
      <div className="auth-card access-card">
        <p className="eyebrow">404</p>
        <h1>Page not found</h1>
        <p>The page you requested does not exist.</p>
        <Link to="/login" className="primary-button link-button">
          Go home
        </Link>
      </div>
    </div>
  );
}
