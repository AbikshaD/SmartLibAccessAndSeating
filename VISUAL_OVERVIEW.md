# Booking Expiry Feature - Visual Overview

## Before (Old UI)
```
My bookings
┌─────────────────────────────────────────────────┐
│ 🪑 Seat B-42                     ✓ CONFIRMED   │
│ 📍 Ground floor · 📅 Mon, Oct 6 · 🕐 14:00–15:00│
│                                   [Cancel]     │
└─────────────────────────────────────────────────┘
```

## After (New UI with Expiry)

### Active Booking (15 minutes remaining)
```
My bookings (Total: 3)  Active: 2  Completed: 1
┌─────────────────────────────────────────────────────────┐
│ 🪑 Seat B-42                          ✓ Active          │
│ 📍 Ground floor · 📅 Mon, Oct 6 · 🕐 14:00–15:00        │
│ ⏳ 15m left                                              │
│                                          [Cancel]      │
└─────────────────────────────────────────────────────────┘
```

### Booking About to Expire (30 seconds remaining)
```
┌─────────────────────────────────────────────────────────┐
│ 🪑 Seat B-42                          ✓ Active          │
│ 📍 Ground floor · 📅 Mon, Oct 6 · 🕐 14:00–15:00        │
│ ⏳ ending soon  (Red highlighted)                        │
│                                          [Cancel]      │
└─────────────────────────────────────────────────────────┘
```

### Completed Booking (Slot Expired)
```
┌─────────────────────────────────────────────────────────┐
│ ✅ Seat B-42                         ✓ Completed        │
│ 📍 Ground floor · 📅 Mon, Oct 6 · 🕐 14:00–15:00        │
│ Your slot has ended. The seat is now available for     │
│ other students.                                        │
└─────────────────────────────────────────────────────────┘
```
(Green background, no cancel button, CheckCircle2 icon)

### Cancelled Booking
```
┌─────────────────────────────────────────────────────────┐
│ ⚠️ Seat B-42                         ✓ Cancelled       │
│ 📍 Ground floor · 📅 Mon, Oct 6 · 🕐 14:00–15:00        │
└─────────────────────────────────────────────────────────┘
```
(Red background, faded, no cancel button)

## Dashboard Stats

### Before
```
Stats:
  🪑 Total seats: 64
  📍 Available now: 42
  📅 Active bookings: 3
```

### After
```
Stats:
  🪑 Total seats: 64
  📍 Available now: 42
  📅 Active bookings: 2
  ✅ Completed: 1
```

## Status Color Scheme

| Status | Color | Icon | Badge | Background |
|--------|-------|------|-------|-----------|
| Active | Blue | 🪑 | "Active" | Light blue tint |
| Completed | Green | ✅ | "Completed" | Light green (#f0fdf4) |
| Cancelled | Red | ⚠️ | "Cancelled" | Light red (#fef2f2) |

## Time Remaining Display

Shows only for active bookings, updates on page refresh:
- `45m left` - More than 1 minute
- `2h 15m left` - Hours and minutes format
- `ending soon` - Less than 1 minute (displayed in red)
- *(hidden)* - Already expired

## Information Flow

```
┌────────────────────────────────────────────────────────┐
│                     Frontend                           │
│  StudentSeats.jsx                                      │
│  ├─ getBookingStatus() → 'active'/'completed'/'...'   │
│  ├─ getTimeRemaining() → '45m left'/'ending soon'/null│
│  └─ isBookingExpired() → boolean                       │
└──────────┬─────────────────────────────────────────────┘
           │ renders with status/time
           ▼
┌──────────────────────────────────┐
│     Booking List Display         │
│  ├─ Status badge                │
│  ├─ Status icon                 │
│  ├─ Time remaining (if active)  │
│  ├─ Completion message (if done)│
│  └─ Cancel button (if active)   │
└──────────┬──────────────────────┘
           │ Manual refresh / Page reload
           ▼
┌────────────────────────────────────────────────────────┐
│                     Backend                           │
│  BookingService.completeExpiredBookings()            │
│  └─ Scheduler runs every 10 seconds                 │
│     ├─ Finds CONFIRMED bookings with expired endTime │
│     ├─ Marks booking as COMPLETED                   │
│     └─ Changes seat from BOOKED to AVAILABLE        │
└────────────────────────────────────────────────────────┘
```

## Key Features Added

✅ **Status Badge System**
- Color-coded by booking status
- Shows current state at a glance

✅ **Time Remaining Display**
- Only shows for active bookings
- Formats as "Xh Ym left" or "Xm left"
- Changes to red "ending soon" when < 1 min
- Disappears when expired

✅ **Completion Confirmation**
- Clear message when slot ends
- Explains seat is now available
- Distinguishes from manual cancellation

✅ **Smart Cancel Button**
- Only visible for active bookings
- Hidden for completed or cancelled bookings
- Prevents confusion with expired slots

✅ **Dashboard Insights**
- Track active vs completed bookings
- Quick overview of booking activity

## Performance Impact

- **No Polling**: Frontend doesn't poll backend
- **Helper Functions**: O(1) performance, pure functions
- **Build Size**: No significant increase (300.80 kB total)
- **Rendering**: Minimal overhead on list display
- **Memory**: Reuses existing booking data structures

## Accessibility

- ✅ Semantic HTML structure
- ✅ Proper use of heading hierarchy
- ✅ Icons paired with text labels
- ✅ Color not the only indicator (icons + text)
- ✅ High contrast for status badges
- ✅ Disabled states clearly indicated
