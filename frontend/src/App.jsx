import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import './App.css';
import Layout from './components/Layout';
import { AuthProvider, useAuth } from './context/AuthContext';
import AccessDenied from './pages/AccessDenied';
import AdminDashboard from './pages/AdminDashboard';
import AdminSeats from './pages/AdminSeats';
import AdminUsers from './pages/AdminUsers';
import Login from './pages/Login';
import NotFound from './pages/NotFound';
import Register from './pages/Register';
import StudentDashboard from './pages/StudentDashboard';
import StudentProfile from './pages/StudentProfile';
import StudentSeats from './pages/StudentSeats';
import ProtectedRoute from './routes/ProtectedRoute';

function RedirectToDefault() {
  const { isAuthenticated, role } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return <Navigate to={role === 'ADMIN' ? '/admin' : '/student'} replace />;
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
        <Route path="/student/profile" element={<Layout><StudentProfile /></Layout>} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={['ADMIN']} />}>
        <Route path="/admin" element={<Layout><AdminDashboard /></Layout>} />
        <Route path="/admin/users" element={<Layout><AdminUsers /></Layout>} />
        <Route path="/admin/seats" element={<Layout><AdminSeats /></Layout>} />
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
