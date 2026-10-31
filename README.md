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

## Backend and frontend connection

The frontend reads the backend URL from `VITE_API_URL` in `frontend/.env`. The default is `http://localhost:8080`. From the repository root, use separate terminals:

```powershell
cd backend
# Copy backend/.env.example to backend/.env and fill in local secrets.
.\start-local.ps1

cd ../frontend
npm install
npm run dev
```

The backend listens on port `8080` and MongoDB is expected at `mongodb://localhost:27017`. Spring Boot loads `backend/.env` when started from the repository root, or `.env` when started from the `backend/` directory, so Groq and other local settings are available when launching through Maven or an IDE as well as `start-local.ps1`. Restart the backend after changing `.env`; running processes do not reload configuration automatically. `.env` is ignored by Git; do not commit credentials. The JWT secret must remain the same when restarting the backend, otherwise existing browser tokens become invalid.

## Deploying the backend to Render

The root `render.yaml` defines the Spring Boot backend as a free Render Docker web service. Push the repository to GitHub, then create a Blueprint in Render from that repository. When prompted, enter `MONGODB_URI` from your local `backend/.env`; local `.env` files are not uploaded or read by Render. Enter `GROQ_API_KEY` if AI classification and recommendations should be enabled. Render generates `SECURITY_JWT_SECRET` for the deployment.

Create the MongoDB database in MongoDB Atlas and allow connections from Render in Atlas Network Access. Keep credentials in Render environment settings, never in the repository. When deploying the frontend later, set the backend's `FRONTEND_ORIGIN` to the frontend's `https://` Render hostname so API requests and maintenance WebSockets are allowed. Free backend services may sleep when idle; the first request after a pause can take longer.

## Pages

- `/login`
- `/register`
- `/student`
- `/student/seats`
- `/student/recommendations`
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

When a confirmed booking's end time passes, the backend marks it `COMPLETED` and makes its seat available again. Expiry is checked automatically every 10 seconds.

The backend seeds a starter book catalog across computer science, fiction, science and mathematics, history and philosophy, business and management, and literature and classics. Catalog seeding is additive: it adds missing sample books on startup without deleting or changing existing catalog records.

Students can request AI-powered catalog recommendations with `POST /books/recommendations/chat` using their JWT:

```json
{
  "message": "Suggest beginner-level Java books",
  "conversation": []
}
```

The optional `conversation` array contains up to 10 earlier `{ "role": "user" | "assistant", "content": "..." }` messages for follow-up requests. The response includes a `message` and a `books` array containing only records from MongoDB, including their available-copy counts and descriptions. Groq receives the server-side `GROQ_API_KEY`; it is never sent to the frontend. An empty catalog returns no recommendations, and provider or invalid-response errors are surfaced to the client.

## Authentication flow

- User submits `studentId` and `password` to `POST /auth/login`.
- Backend returns a JWT token in the `token` field.
- The frontend stores the JWT in localStorage.
- The frontend reads the JWT subject as the `studentId` and then calls `GET /users/{studentId}` to load the full user record and role.
- Role-based route access is enforced on the frontend as a UI gate, while the backend remains the final authority.

## Maintenance ticket notifications

The backend persists a notification for each active, unblocked maintenance staff member on a ticket's assigned team and pushes it over STOMP for connected staff:

- Connect to `ws://localhost:8080/ws` (or `wss://<backend-host>/ws`) using STOMP. Send `Authorization: Bearer <JWT>` in the STOMP `CONNECT` headers, then subscribe to `/user/queue/notifications`.
- `GET /maintenance/notifications` returns the authenticated staff member's notifications.
- `GET /maintenance/notifications/unread-count` returns `{ "unreadCount": <number> }`.
- `PUT /maintenance/notifications/{notificationId}/read` marks that staff member's notification as read.

These notification REST endpoints and WebSocket subscriptions require the `MAINTENANCE` role. A staff member only receives messages addressed to their own account; notifications are not broadcast across teams.
