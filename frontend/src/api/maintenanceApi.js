import { apiRequest } from './api';

export const maintenanceApi = {
  createTicket(ticket) {
    return apiRequest('/maintenance/tickets', {
      method: 'POST',
      body: ticket,
    });
  },

  reportIssue(issue) {
    return apiRequest('/maintenance/reports', {
      method: 'POST',
      body: issue,
    });
  },

  getAllManagementTickets() {
    return apiRequest('/maintenance/management/tickets');
  },

  getManagementTicket(ticketId) {
    return apiRequest(`/maintenance/management/tickets/${encodeURIComponent(ticketId)}`);
  },

  assignTeam(ticketId, team) {
    return apiRequest(`/maintenance/tickets/${encodeURIComponent(ticketId)}/team`, {
      method: 'PUT',
      body: { team },
    });
  },

  getDashboard() {
    return apiRequest('/maintenance/dashboard');
  },

  getTicket(ticketId) {
    return apiRequest(`/maintenance/tickets/${encodeURIComponent(ticketId)}`);
  },

  updateTicketStatus(ticketId, status, resolutionNotes) {
    return apiRequest(`/maintenance/tickets/${encodeURIComponent(ticketId)}/status`, {
      method: 'PUT',
      body: { status, resolutionNotes },
    });
  },
};