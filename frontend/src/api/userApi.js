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

  createMaintenanceAdmin(user) {
    return apiRequest('/users/maintenance-admin', {
      method: 'POST',
      body: user,
    });
  },

  setActive(studentId, active) {
    return apiRequest(`/users/${encodeURIComponent(studentId)}/active`, {
      method: 'PUT',
      body: { active },
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

  unblockUser(studentId) {
    return apiRequest(`/users/${studentId}/unblock`, {
      method: 'PUT',
    });
  },
};
