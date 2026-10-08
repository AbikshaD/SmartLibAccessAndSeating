import { Armchair, BarChart3, BookOpen, LayoutGrid, LogOut, Megaphone, MessageSquareText, ShieldCheck, UserRound, Users } from 'lucide-react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const studentLinks = [
  { to: '/student', label: 'Dashboard', icon: LayoutGrid },
  { to: '/student/seats', label: 'Seats', icon: Armchair },
  { to: '/student/books', label: 'Books', icon: BookOpen },
  { to: '/student/profile', label: 'Profile', icon: UserRound },
  { to: '/student/feedback', label: 'Feedback', icon: MessageSquareText },
];

const adminLinks = [
  { to: '/admin', label: 'Dashboard', icon: BarChart3 },
  { to: '/admin/announcements', label: 'Announcements', icon: Megaphone },
  { to: '/student/books', label: 'Books', icon: BookOpen },
  { to: '/admin/users', label: 'Users', icon: Users },
  { to: '/admin/seats', label: 'Seats', icon: Armchair },
  { to: '/admin/feedback', label: 'Feedback', icon: MessageSquareText },
];

export default function Sidebar() {
  const { user, logout, role } = useAuth();
  const links = role === 'ADMIN'
    ? [...studentLinks.filter(({ to }) => to !== '/student/feedback'), ...adminLinks]
    : studentLinks;

  return (
    <aside className="sidebar">
      <div className="brand-block">
        <div className="brand-badge">
          <ShieldCheck size={18} />
        </div>
        <div>
          <p className="eyebrow">SmartLib</p>
          <h2>Library Access</h2>
        </div>
      </div>

      <nav className="nav-list" aria-label="Main navigation">
        {links.map(({ to, label, icon: Icon }) => (
          <NavLink key={to} to={to} className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
            <Icon size={18} />
            <span>{label}</span>
          </NavLink>
        ))}
      </nav>

      <div className="sidebar-footer">
        <div>
          <p className="eyebrow">Signed in</p>
          <strong>{user?.name || user?.studentId || 'User'}</strong>
        </div>
        <button type="button" className="logout-button" onClick={logout}>
          <LogOut size={16} />
          Logout
        </button>
      </div>
    </aside>
  );
}
