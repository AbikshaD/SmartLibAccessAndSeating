import { apiRequest } from './api';

export const userApi = {
  getAllUsers() {
    return apiRequest('/users');
  },

  getUserById(studentId) {
    return apiRequest(`/users/${studentId}`);
  },

  createUser(user) {
    return apiRequest('/users', {
      method: 'POST',
      body: user,
    });
  },

  createAdmin(user) {
    return apiRequest('/users/admin', {
      method: 'POST',
      body: user,
    });
  },

  updateUser(studentId, user) {
    return apiRequest(`/users/${studentId}`, {
      method: 'PUT',
      body: user,
    });
  },

  deleteUser(studentId) {
    return apiRequest(`/users/${studentId}`, {
      method: 'DELETE',
    });
  },
};
