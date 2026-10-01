import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { authApi } from '../api/authApi';
import { getAuthToken, clearAuthToken, setAuthToken } from '../api/api';
import { userApi } from '../api/userApi';

const AuthContext = createContext(null);

const decodeSubject = (token) => {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.sub || payload.username || null;
  } catch {
    return null;
  }
};

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => getAuthToken());
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const refreshUser = async (currentToken = token) => {
    if (!currentToken) {
      setUser(null);
      setLoading(false);
      return null;
    }

    const studentId = decodeSubject(currentToken);

    if (!studentId) {
      clearAuthToken();
      setToken(null);
      setUser(null);
      setLoading(false);
      return null;
    }

    try {
      const currentUser = await userApi.getUserById(studentId);
      setUser(currentUser);
      return currentUser;
    } catch (error) {
      clearAuthToken();
      setToken(null);
      setUser(null);
      return null;
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    refreshUser(token);
  }, [token]);

  const login = async ({ studentId, password }) => {
    const response = await authApi.login({ studentId, password });
    const nextToken = response.token;

    setAuthToken(nextToken);
    setToken(nextToken);
    setLoading(true);

    const currentUser = await userApi.getUserById(studentId);
    setUser(currentUser);
    setLoading(false);
    return currentUser;
  };

  const logout = () => {
    clearAuthToken();
    setToken(null);
    setUser(null);
  };

  const register = async (payload) => {
    const response = await authApi.registerUser(payload);
    return response;
  };

  const value = useMemo(
    () => ({
      token,
      user,
      role: user?.role ? user.role.toUpperCase() : null,
      isAuthenticated: Boolean(token) && Boolean(user),
      loading,
      login,
      logout,
      register,
      refreshUser,
    }),
    [token, user, loading],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }

  return context;
}
