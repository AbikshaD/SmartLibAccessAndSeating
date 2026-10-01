import { useAuth } from '../context/AuthContext';

export default function StudentProfile() {
  const { user } = useAuth();

  if (!user) {
    return <div className="empty-state">Profile not available.</div>;
  }

  return (
    <div className="page-container">
      <header className="page-header">
        <div>
          <p className="eyebrow">Student profile</p>
          <h1>{user.name}</h1>
        </div>
      </header>

      <section className="panel-card profile-card">
        <div className="detail-grid">
          <div>
            <p className="eyebrow">Student ID</p>
            <strong>{user.studentId}</strong>
          </div>
          <div>
            <p className="eyebrow">Email</p>
            <strong>{user.email}</strong>
          </div>
          <div>
            <p className="eyebrow">Role</p>
            <strong>{user.role}</strong>
          </div>
        </div>
      </section>
    </div>
  );
}
