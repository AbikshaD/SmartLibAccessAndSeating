import { apiRequest } from './api';

export const maintenanceUserApi = {
  getAll() {
    return apiRequest('/users/maintenance');
  },

  create(user) {
    return apiRequest('/users/maintenance', {
      method: 'POST',
      body: user,
    });
  },

  updateTeam(studentId, team) {
    return apiRequest(`/users/${encodeURIComponent(studentId)}/maintenance-team`, {
      method: 'PUT',
      body: { team },
    });
  },

  setActive(studentId, active) {
    return apiRequest(`/users/${encodeURIComponent(studentId)}/active`, {
      method: 'PUT',
      body: { active },
    });
  },
};