import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import './App.css';
import Layout from './components/Layout';
import { AuthProvider, useAuth } from './context/AuthContext';
import AccessDenied from './pages/AccessDenied';
import AdminAnnouncements from './pages/AdminAnnouncements';
import AdminDashboard from './pages/AdminDashboard';
import AdminFeedback from './pages/AdminFeedback';
import AdminBorrows from './pages/AdminBorrows';
import AdminSeats from './pages/AdminSeats';
import AdminUsers from './pages/AdminUsers';
import AdminMaintenance from './pages/AdminMaintenance';
import Login from './pages/Login';
import MaintenanceDashboard from './pages/MaintenanceDashboard';
import MaintenanceTicketDetails from './pages/MaintenanceTicketDetails';
import NotFound from './pages/NotFound';
import Register from './pages/Register';
import StudentBooks from './pages/StudentBooks';
import StudentDashboard from './pages/StudentDashboard';
import StudentFeedback from './pages/StudentFeedback';
import StudentProfile from './pages/StudentProfile';
import StudentSeats from './pages/StudentSeats';
import ProtectedRoute from './routes/ProtectedRoute';

function RedirectToDefault() {
  const { isAuthenticated, role } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  const destination = role === 'ADMIN'
    ? '/admin'
    : role === 'MAINTENANCE_ADMIN' ? '/maintenance-admin'
      : role === 'MAINTENANCE' ? '/maintenance' : '/student';
  return <Navigate to={destination} replace />;
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<RedirectToDefault />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/access-denied" element={<AccessDenied />} />

      <Route element={<ProtectedRoute allowedRoles={['STUDENT', 'ADMIN']} />}>
        <Route path="/student" element={<Layout><StudentDashboard /></Layout>} />
        <Route path="/student/seats" element={<Layout><StudentSeats /></Layout>} />
        <Route path="/student/books" element={<Layout><StudentBooks /></Layout>} />
        <Route path="/student/profile" element={<Layout><StudentProfile /></Layout>} />
        <Route path="/student/feedback" element={<Layout><StudentFeedback /></Layout>} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={['ADMIN']} />}>
        <Route path="/admin" element={<Layout><AdminDashboard /></Layout>} />
        <Route path="/admin/books" element={<Layout><StudentBooks /></Layout>} />
        <Route path="/admin/borrows" element={<Layout><AdminBorrows /></Layout>} />
        <Route path="/admin/announcements" element={<Layout><AdminAnnouncements /></Layout>} />
        <Route path="/admin/users" element={<Layout><AdminUsers /></Layout>} />
        <Route path="/admin/seats" element={<Layout><AdminSeats /></Layout>} />
        <Route path="/admin/feedback" element={<Layout><AdminFeedback /></Layout>} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={['MAINTENANCE_ADMIN']} />}>
        <Route path="/maintenance-admin" element={<Layout><AdminMaintenance /></Layout>} />
        <Route path="/maintenance-admin/tickets/:ticketId" element={<Layout><MaintenanceTicketDetails /></Layout>} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={['MAINTENANCE']} />}>
        <Route path="/maintenance" element={<Layout><MaintenanceDashboard /></Layout>} />
        <Route path="/maintenance/tickets/:ticketId" element={<Layout><MaintenanceTicketDetails /></Layout>} />
      </Route>

      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
