import { Link } from 'react-router-dom';

export default function AccessDenied() {
  return (
    <div className="auth-shell">
      <div className="auth-card access-card">
        <p className="eyebrow">Access denied</p>
        <h1>You do not have permission</h1>
        <p>This area is restricted to the appropriate role.</p>
        <Link to="/login" className="primary-button link-button">
          Return to login
        </Link>
      </div>
    </div>
  );
}
