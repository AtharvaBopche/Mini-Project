# Smart Parking System

A simple college full-stack project for parking providers and customers. Users can manage vehicles, search locations, get a basic slot recommendation, book, check in and check out. Providers manage locations and slots and review bookings and users.

## Stack
React, JavaScript and CSS (Vite); Java 17, Spring Boot, Maven and JDBC; MySQL.

## Run
1. Install Java 17+, Maven, Node.js and MySQL.
2. The project is configured for a dedicated local MySQL instance on port 3307, separate from any MySQL server already installed on the computer. If you use another server, set `DB_URL`, `DB_USER` and `DB_PASSWORD` before starting Spring Boot.
3. In `backend`, run `mvn spring-boot:run`.
4. In `frontend`, run `npm install` and `npm run dev`; open the Vite URL.

The REST API is under `/api`. Basic routes include `/auth/register`, `/auth/login`, `/parking/locations`, `/vehicles`, `/bookings`, `/sessions/*` and `/admin/*`.

This is a learning demo. Login/password handling is deliberately basic and must not be used as public production authentication.

## Deploy the frontend to Vercel

The repository root contains `vercel.json`. Import this repository into Vercel with the repository root (`.`) as the project root. Vercel installs the frontend dependencies, builds the Vite app and serves `frontend/dist`; the rewrite keeps the React app working on direct page visits.

The Spring Boot API and MySQL database must be hosted separately. Vercel's documented function runtimes do not include Java, so this project does not deploy the Spring Boot server as a Vercel Function. Deploy the backend to a Java-capable host and connect it to a reachable MySQL database. Then set these environment variables:

- Vercel: `VITE_API_URL` = the backend base URL, for example `https://your-api.example.com` (do not add `/api`).
- Backend host: `DB_URL`, `DB_USER`, `DB_PASSWORD` = the hosted MySQL connection, and `FRONTEND_URL` = the deployed Vercel site origin, for example `https://your-project.vercel.app`.

The default frontend API address (`http://localhost:8081`) is for local development only. After changing Vercel environment variables, create a new deployment so the Vite build includes the API URL.
