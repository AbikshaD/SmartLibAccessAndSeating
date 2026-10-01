import { apiRequest } from './api';

export const authApi = {
  login({ studentId, password }) {
    return apiRequest('/auth/login', {
      method: 'POST',
      body: { studentId, password },
      auth: false,
    });
  },

  registerUser(user) {
    return apiRequest('/users', {
      method: 'POST',
      body: user,
      auth: false,
    });
  },
};
