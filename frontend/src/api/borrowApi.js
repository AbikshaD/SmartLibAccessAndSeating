import { apiRequest } from './api';

export const borrowApi = {
  /** Student: request to borrow a book */
  requestBorrow: (bookId) =>
    apiRequest('/borrows', { method: 'POST', body: { bookId } }),

  /** Student: get my borrow history */
  getMyBorrows: () =>
    apiRequest('/borrows/my'),

  /** Admin: get all borrow requests */
  getAllBorrows: () =>
    apiRequest('/borrows'),

  /** Admin: approve a borrow request */
  approveBorrow: (id) =>
    apiRequest(`/borrows/${id}/approve`, { method: 'PUT' }),

  /** Admin: reject a borrow request */
  rejectBorrow: (id) =>
    apiRequest(`/borrows/${id}/reject`, { method: 'PUT' }),

  /** Admin: mark book as returned */
  markReturned: (id) =>
    apiRequest(`/borrows/${id}/return`, { method: 'PUT' }),
};
