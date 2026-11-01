# SmartLib – Intelligent Library Access and Operations Platform

A smart library management platform designed to simplify seat reservations, manage library resources, and streamline maintenance operations through a centralised web application.

SmartLib provides students and administrators with an organised digital platform to manage library seating, bookings, books, feedback, announcements, and maintenance activities.

## Live Demo

- **Frontend:** https://smart-lib-access-and-seating-one.vercel.app
- **Backend:** Add your Render deployment URL here.
- **GitHub Repository:** https://github.com/AbikshaD/SmartLibAccessAndSeating

## Key Features

### 1. Seat Booking Management
- View available library seats across multiple floors.
- Book seats through the application.
- Manage and monitor seat reservations.
- Administrator-controlled seat management.

### 2. Role-Based Access Control
The application supports different user roles with dedicated permissions:

- **Student:** Book seats, view library resources, submit feedback, and raise maintenance reports.
- **Admin:** Manage users, seats, books, announcements, and feedback.
- **Maintenance Admin:** Manage maintenance staff, assign maintenance teams, and oversee maintenance requests.
- **Maintenance Staff:** Access assigned maintenance activities and update their status.

### 3. Maintenance Management
- Submit maintenance reports.
- Track maintenance requests and their status.
- Assign maintenance requests to appropriate teams.
- AI-assisted maintenance request classification using Groq API.

### 4. Library Resource Management
- Manage available books.
- Browse library resources.
- Explore book recommendations through an AI-powered chat feature.

### 5. Feedback and Announcements
- Students can submit feedback.
- Administrators can review and respond to feedback.
- Publish and manage library announcements.

### 6. Secure Authentication
- JWT-based authentication.
- Password encryption using BCrypt.
- Role-based endpoint authorisation.
- Stateless authentication using Spring Security.

## Technology Stack

| Layer | Technologies |
|---|---|
| Frontend | React.js, JavaScript, CSS |
| Backend | Java, Spring Boot |
| Security | Spring Security, JWT, BCrypt |
| Database | MongoDB Atlas |
| AI Integration | Groq API |
| API Testing | Postman |
| Version Control | Git, GitHub |
| Frontend Deployment | Vercel |
| Backend Deployment | Render |

## Application Architecture

```text
          React Frontend
             (Vercel)
                 |
                 | REST API Requests
                 |
          Spring Boot Backend
              (Render)
                 |
        ---------------------
        |         |         |
     Security  Services  Controllers
        |         |         |
        ---------------------
                 |
             MongoDB Atlas
                 |
          Application Data

       Groq API Integration
       (AI-powered features)
```

## Project Structure

```text
SmartLibAccessAndSeating/
│
├── backend/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── library_management/
│   │       └── resources/
│   ├── Dockerfile
│   └── pom.xml
│
├── frontend/
│   ├── src/
│   ├── public/
│   ├── package.json
│   └── ...
│
└── README.md
```

## Getting Started

### Prerequisites

- Java 23
- Maven
- Node.js and npm
- MongoDB Atlas account
- Groq API key

### 1. Clone the Repository

```bash
git clone https://github.com/AbikshaD/SmartLibAccessAndSeating.git

cd SmartLibAccessAndSeating
```

### 2. Run the Backend

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

Configure the required environment variables before starting the backend.

### 3. Run the Frontend

Open another terminal:

```bash
cd frontend
npm install
npm run dev
```

The frontend and backend can now run locally.

## Environment Configuration

Configure the following variables in your deployment environment.

**Backend:**
- `MONGODB_URI`
- `MONGODB_DATABASE`
- `SECURITY_JWT_SECRET`
- `SECURITY_JWT_EXPIRATION_MS`
- `GROQ_API_URL`
- `GROQ_API_KEY`
- `app.frontend-origin`

**Frontend:**
- Configure the backend API URL using the environment variable expected by your frontend API configuration.

Never commit API keys, database credentials, JWT secrets, or `.env` files to GitHub.

## Deployment

- Frontend deployed using Vercel.
- Backend deployed using Render with Docker.
- Database hosted on MongoDB Atlas.

## Purpose of the Project

SmartLib aims to improve the library experience by digitising seat reservation, resource management, and maintenance workflows. It combines a role-based management system with AI integrations to make library operations more organised and efficient.

---

**Developed as a full-stack application using React, Java Spring Boot, MongoDB, and AI integration.**