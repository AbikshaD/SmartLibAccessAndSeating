import { apiRequest } from './api';

export const feedbackApi = {
  submit(feedback) {
    return apiRequest('/feedback', {
      method: 'POST',
      body: feedback,
    });
  },

  getMine() {
    return apiRequest('/feedback/my');
  },

  getAll() {
    return apiRequest('/feedback');
  },

  getById(feedbackId) {
    return apiRequest(`/feedback/${feedbackId}`);
  },

  delete(feedbackId) {
    return apiRequest(`/feedback/${feedbackId}`, {
      method: 'DELETE',
    });
  },

  respond(feedbackId, response) {
    return apiRequest(`/feedback/${feedbackId}/respond`, {
      method: 'PUT',
      body: { response },
    });
  },

  resolve(feedbackId) {
    return apiRequest(`/feedback/${feedbackId}/resolve`, {
      method: 'PUT',
    });
  },
};