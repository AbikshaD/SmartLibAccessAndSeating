import { apiRequest } from './api';

export const maintenanceApi = {
  createTicket(ticket) {
    return apiRequest('/maintenance/tickets', {
      method: 'POST',
      body: ticket,
    });
  },

  getDashboard() {
    return apiRequest('/maintenance/dashboard');
  },

  getTicket(ticketId) {
    return apiRequest(`/maintenance/tickets/${encodeURIComponent(ticketId)}`);
  },

  updateTicketStatus(ticketId, status) {
    return apiRequest(`/maintenance/tickets/${encodeURIComponent(ticketId)}/status`, {
      method: 'PUT',
      body: { status },
    });
  },
};