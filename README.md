# SmartLib Access and Seating Management System

This repository contains the existing Spring Boot backend and a React + Vite frontend for the library access and seating management system.

## Project structure

SmartLibAccessAndSeating/
├── backend/
│   └── existing Spring Boot backend
├── frontend/
│   ├── package.json
│   ├── index.html
│   ├── src/
│   └── ...
└── README.md

## Frontend setup

1. Install dependencies:
   npm install
2. Start the development server:
   npm run dev
3. Create a `.env` file in `frontend/` with:
   VITE_API_URL=http://localhost:8080

## Backend URL

The frontend reads the backend URL from `VITE_API_URL` in the frontend `.env` file. The default is `http://localhost:8080`.

## Pages

- `/login`
- `/register`
- `/student`
- `/student/seats`
- `/student/profile`
- `/admin`
- `/admin/users`
- `/admin/seats`
- `/access-denied`

## Seat capacity API

Admins can set the maximum seat count for a supported floor with `PUT /seats/capacity`:

```json
{
  "floor": "GROUND",
  "maxSeats": 50
}
```

The API persists that floor's capacity and creates any missing seats up to the requested total. Repeating the same capacity does not create duplicates. Capacity values must be between 1 and 500; reducing a floor below its current seat count returns `409 Conflict`. Admins can list saved capacities with `GET /seats/capacity`.

## Authentication flow

- User submits `studentId` and `password` to `POST /auth/login`.
- Backend returns a JWT token in the `token` field.
- The frontend stores the JWT in localStorage.
- The frontend reads the JWT subject as the `studentId` and then calls `GET /users/{studentId}` to load the full user record and role.
- Role-based route access is enforced on the frontend as a UI gate, while the backend remains the final authority.
