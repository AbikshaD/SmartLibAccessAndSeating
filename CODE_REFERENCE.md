# Booking Expiry Feature - Code Reference

## Helper Functions (StudentSeats.jsx, Lines 26-47)

### getBookingStatus
Determines the current status of a booking for display purposes.
```javascript
const getBookingStatus = (booking) => {
  if (booking.status === 'COMPLETED') return 'completed';
  if (booking.status === 'CANCELLED') return 'cancelled';
  if (booking.status === 'CONFIRMED') return 'active';
  return 'unknown';
};
```

**Returns**: String indicating booking state
- `'completed'` - Booking slot ended, seat released
- `'cancelled'` - Student manually cancelled booking
- `'active'` - Booking is currently valid
- `'unknown'` - Status not recognized (defensive coding)

**Usage**: `const status = getBookingStatus(booking);`

---

### getTimeRemaining
Calculates human-readable time until booking slot expires.
```javascript
const getTimeRemaining = (bookingDate, endTime) => {
  const today = new Date();
  const bookingDateObj = new Date(`${bookingDate}T${endTime}:00`);
  const diffMs = bookingDateObj - today;
  const diffMins = Math.floor(diffMs / 60000);
  
  if (diffMins < 0) return null;  // Already expired
  if (diffMins < 1) return 'ending soon';  // < 1 minute
  if (diffMins < 60) return `${diffMins}m left`;  // Minutes only
  const hours = Math.floor(diffMins / 60);
  const mins = diffMins % 60;
  return `${hours}h ${mins}m left`;  // Hours and minutes
};
```

**Parameters**:
- `bookingDate` - String in YYYY-MM-DD format (e.g., "2025-01-15")
- `endTime` - String in HH:MM format (e.g., "14:30")

**Returns**: 
- Formatted string if still active (e.g., "45m left", "2h 30m left", "ending soon")
- `null` if already expired

---

### isBookingExpired
Boolean check for whether a booking has expired.
```javascript
const isBookingExpired = (bookingDate, endTime) => {
  const today = new Date();
  const bookingDateObj = new Date(`${bookingDate}T${endTime}:00`);
  return bookingDateObj < today;
};
```

**Parameters**: Same as `getTimeRemaining`

**Returns**: Boolean

---

## Booking List Item Rendering (StudentSeats.jsx, Lines 238-300)

Key changes:
- Status icon changes based on booking status (Armchair → CheckCircle2 → AlertCircle)
- Status badge added (Active → Completed → Cancelled)
- Time remaining display added for active bookings
- Completion message added for completed bookings
- Cancel button only shown for CONFIRMED bookings

---

## CSS Classes (App.css)

### Booking Item Variants
```css
/* Completed booking (green tint) */
.booking-item-completed {
  background: #f0fdf4;
  border-color: #bbf7d0;
}

/* Cancelled booking (red tint, faded) */
.booking-item-cancelled {
  background: #fef2f2;
  border-color: #fecdd3;
  opacity: 0.8;
}
```

### Status Badges
```css
.badge-active { background: #dbeafe; color: #1e40af; }
.badge-completed { background: #dcfce7; color: #166534; }
.badge-cancelled { background: #fecdd3; color: #991b1b; }
```

### Time Remaining Display
```css
.booking-expiry-info {
  background: #fef3c7;  /* Yellow when > 1 min */
}

.booking-expiry-info.expiring {
  background: #fee2e2;  /* Red when < 1 min */
}
```

### Completion Message
```css
.booking-completed-info {
  background: #ecfdf5;
  border-left: 3px solid #22c55e;
}
```

---

## Stats Dashboard Update

Added two new stats for students:
- "Active bookings" - Count of CONFIRMED bookings
- "Completed" - Count of COMPLETED bookings

```javascript
const confirmedBookings = bookings.filter((booking) => booking.status === 'CONFIRMED');
const completedBookings = bookings.filter((booking) => booking.status === 'COMPLETED');
```

---

## Integration with Backend

Backend Status → Frontend Display:
- **CONFIRMED** → "Active" badge (blue), time remaining, cancel button
- **COMPLETED** → "Completed" badge (green), completion message
- **CANCELLED** → "Cancelled" badge (red), faded appearance

The backend scheduler marks bookings as COMPLETED every 10 seconds when their end time passes. No API changes required.
