import { apiRequest } from './api';

export const capacityApi = {
  setFloorCapacity(floor, maxSeats) {
    return apiRequest('/seats/capacity', {
      method: 'PUT',
      body: { floor, maxSeats },
    });
  },

  getFloorCapacities() {
    return apiRequest('/seats/capacity');
  },
};
