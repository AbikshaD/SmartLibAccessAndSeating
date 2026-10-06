# Floor Capacity Management - User Guide

## For Admin Users

### Setting Floor Capacity

1. **Navigate to Admin Section**
   - Click on "Admin Seats" in the sidebar

2. **Access Capacity Management**
   - Look for the "Floor Capacity Management" section at the top
   - You'll see a lightning bolt icon (⚡) next to the heading

3. **Configure Capacity**
   - **Floor**: Select from dropdown (Ground, First, Second, Third)
   - **Maximum Seats**: Enter a number between 1-500
   - Example: Setting Ground Floor to 20 will create seats G-01 through G-20

4. **Submit**
   - Click "Set Floor Capacity" button
   - System will automatically create any missing seats up to your specified capacity

5. **Verify Results**
   - A success message will appear showing:
     - How many new seats were created
     - Total seats now available for that floor
   - "Current Floor Capacities" section displays all configured floors

### What Happens When You Set Capacity?

- **First Time**: System creates all seats up to your specified maximum (e.g., 20 seats)
- **Same Capacity**: No changes (idempotent - won't create duplicates)
- **Increase Capacity**: Creates only the new missing seats (e.g., 20→30 creates seats 21-30)
- **Reduce Capacity**: Returns an error if seats are already booked (can't reduce below existing seats)

### Important Rules

⚠️ **Cannot reduce capacity below existing seats**
- Example: If you set Ground Floor to 20 seats, then want to reduce to 10, the system will reject this if seats 11-20 exist
- You can only reduce capacity when seats are deleted first

### Manual Seat Management

For advanced use cases, you can still manually create individual seats:
- Use the "Create seat" section below capacity management
- Useful for special maintenance seats or custom configurations

### Current Floor Capacities Display

Shows all floors you've configured with:
- **Floor Name**: Which floor (e.g., GROUND)
- **Max Seats**: Maximum capacity you set
- **Created**: Number of seats actually created

## API Responses

### Setting Capacity - Success Response
```json
{
  "floor": "GROUND",
  "maxSeats": 20,
  "createdSeats": ["G-01", "G-02", ..., "G-20"],
  "totalSeats": 20
}
```

### Getting Capacities - Response
```json
[
  {
    "floor": "GROUND",
    "maxSeats": 20,
    "totalSeats": 20
  },
  {
    "floor": "FIRST",
    "maxSeats": 15,
    "totalSeats": 15
  }
]
```

## Common Scenarios

### Scenario 1: Setting up a new library floor
1. Set GROUND to 20 seats
2. System creates: G-01, G-02, ..., G-20
3. Students can now book ground floor seats

### Scenario 2: Expanding a floor
1. Original setting: FIRST floor = 10 seats
2. Later expand to: FIRST floor = 15 seats
3. System creates: F-11, F-12, F-13, F-14, F-15
4. No duplicates created, original F-01 through F-10 remain

### Scenario 3: Custom seat creation
1. Use Floor Capacity to create standard seats
2. Use Manual Seat Creation for special cases (e.g., accessible seating marked MAINTENANCE)
3. Both systems work together

## Troubleshooting

### Error: "Maximum seats must be between 1 and 500"
- Ensure your input is a valid number between 1-500
- Clear the field and try again

### Error: "Unable to set floor capacity" with 409 status
- You tried to reduce capacity below existing seats
- Example: Had 20 seats, tried to reduce to 10
- Solution: Delete unused seats first, or contact support

### No capacities showing
- You haven't set any capacities yet
- Set at least one floor capacity to see the display
- List will appear once the first capacity is created

## Technical Notes

- Floor names are automatically normalized to uppercase (GROUND, FIRST, SECOND, THIRD)
- Seat numbers use prefixes: G-XX, F-XX, S-XX, T-XX
- Changes are immediately reflected in the seat grid below
- All operations are atomic - either fully succeeds or fully fails
