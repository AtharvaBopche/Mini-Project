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
