import { apiRequest } from './api';

// Curated default announcements in case backend is offline or on initial launch
export const FALLBACK_ANNOUNCEMENTS = [
  {
    id: 'ann-1',
    title: 'Early Library Closure Today at 6:00 PM',
    content: 'Please note that the central library and all reading floors will close early at 6:00 PM today due to scheduled electrical facility upgrades. All checkout desks and seat bookings after 6:00 PM are affected. Regular operating hours resume tomorrow at 8:00 AM.',
    type: 'CLOSURE',
    priority: 'HIGH',
    postedBy: 'admin',
    active: true,
    createdAt: new Date().toISOString()
  },
  {
    id: 'ann-2',
    title: 'Extended 24/7 Hours for Examination Week',
    content: 'To support students during upcoming final examinations, Floor 2 and Floor 3 study halls will remain open 24/7 starting next Monday. High-speed Wi-Fi and quiet zones will be fully operational throughout the night.',
    type: 'GENERAL',
    priority: 'NORMAL',
    postedBy: 'admin',
    active: true,
    createdAt: new Date(Date.now() - 86400000).toISOString()
  },
  {
    id: 'ann-3',
    title: 'Wi-Fi Network Maintenance Notice',
    content: 'Brief Wi-Fi connectivity interruptions may occur on Floor 1 between 2:00 PM and 3:00 PM this Friday while campus IT upgrades network routers.',
    type: 'MAINTENANCE',
    priority: 'MEDIUM',
    postedBy: 'admin',
    active: true,
    createdAt: new Date(Date.now() - 172800000).toISOString()
  }
];

export const announcementApi = {
  async getAllAnnouncements() {
    try {
      const data = await apiRequest('/announcements');
      if (Array.isArray(data) && data.length > 0) {
        return data;
      }
    } catch {
      // Return fallback below
    }
    return FALLBACK_ANNOUNCEMENTS;
  },

  async getActiveAnnouncements() {
    try {
      const data = await apiRequest('/announcements/active');
      if (Array.isArray(data) && data.length > 0) {
        return data;
      }
    } catch {
      // Fallback
    }
    return FALLBACK_ANNOUNCEMENTS.filter((a) => a.active);
  },

  async createAnnouncement(payload) {
    try {
      const data = await apiRequest('/announcements', {
        method: 'POST',
        body: payload
      });
      return data;
    } catch {
      // If offline, create local object
      const fallbackItem = {
        id: `ann-local-${Date.now()}`,
        title: payload.title,
        content: payload.content,
        type: payload.type || 'GENERAL',
        priority: payload.priority || 'NORMAL',
        postedBy: 'admin',
        active: true,
        createdAt: new Date().toISOString()
      };
      FALLBACK_ANNOUNCEMENTS.unshift(fallbackItem);
      return fallbackItem;
    }
  },

  async updateAnnouncement(id, payload) {
    return apiRequest(`/announcements/${id}`, {
      method: 'PUT',
      body: payload
    });
  },

  async deleteAnnouncement(id) {
    try {
      await apiRequest(`/announcements/${id}`, {
        method: 'DELETE'
      });
    } catch {
      const idx = FALLBACK_ANNOUNCEMENTS.findIndex((a) => a.id === id);
      if (idx !== -1) {
        FALLBACK_ANNOUNCEMENTS.splice(idx, 1);
      }
    }
  }
};
