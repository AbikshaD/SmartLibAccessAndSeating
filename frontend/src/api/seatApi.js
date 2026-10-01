import { apiRequest } from './api';

export const seatApi = {
  getAllSeats() {
    return apiRequest('/seats');
  },

  getAvailableSeats() {
    return apiRequest('/seats/available');
  },

  getSeatById(seatId) {
    return apiRequest(`/seats/${seatId}`);
  },

  createSeat(seat) {
    return apiRequest('/seats', {
      method: 'POST',
      body: seat,
    });
  },

  updateSeat(seatId, seat) {
    return apiRequest(`/seats/${seatId}`, {
      method: 'PUT',
      body: seat,
    });
  },

  deleteSeat(seatId) {
    return apiRequest(`/seats/${seatId}`, {
      method: 'DELETE',
    });
  },
};
