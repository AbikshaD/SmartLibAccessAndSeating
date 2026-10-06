import { apiRequest } from './api';

export const bookingApi = {
  getMyBookings() {
    return apiRequest('/bookings/my');
  },

  createBooking(booking) {
    return apiRequest('/bookings', {
      method: 'POST',
      body: booking,
    });
  },

  cancelBooking(bookingId) {
    return apiRequest(`/bookings/${bookingId}`, {
      method: 'DELETE',
    });
  },
};
