# TrackTrove Backend: Traceable Micro-Transaction Explorer

This repository contains the backend services for **TrackTrove**, a sophisticated platform designed to simulate and visualize micro-transactions with complete traceability across various services. It acts as a "financial debugger" and "flow lab" for backend developers and operations teams, providing deep insights into transaction lifecycles, DTO evolutions, and failure scenarios.

##  Concept

In complex financial systems, understanding the journey of a micro-transaction (like a tip, bid adjustment, refund, or loyalty credit) can be incredibly challenging. TrackTrove addresses this by offering a sandboxed environment where these mini-transactions (₹1–₹500) can be fired, observed, and debugged. It's built to mimic the intricate flows of real-world platforms like Swiggy or Razorpay, without needing actual bank accounts.

##  Core Features (Backend Contributions)

The backend is the engine powering TrackTrove's core functionalities:

* **Transaction Simulator API:** Initiates micro-transactions with configurable success/failure rates.  
* **State Management:** Manages and persists transaction states (INITIATED, ESCROW, COMPLETED, FAILED, REFUNDED, SETTLED) with full audit trails.  
* **Redis TTL for Escrow:** Simulates time-bound holds on transactions.  
* **Job Scheduler:** Handles T+1 settlement simulations and automated retry logic.  
* **Trace Engine:** Captures and stores snapshots of Data Transfer Objects (DTOs) at various stages of a transaction's lifecycle.  
* **API for Trace Inspection:** Provides endpoints to retrieve detailed DTO evolution for any given transaction.  
* **Failure Injection Logic:** Implements mechanisms to simulate webhook misses, Redis TTL expiries, and other common backend failures.  
* **Admin APIs:** Endpoints for managing dispute flows, forcing retries, and manual state overrides.  
* **Real-time Event Push:** Publishes transaction state changes and trace updates via WebSockets for live frontend visualization.

##  Technology Stack

The backend is built with a robust and modern stack, chosen for its reliability, performance, and suitability for financial-grade applications:

* **Spring Boot:**  
  * **Why it fits:** Provides a powerful framework for building robust RESTful APIs, managing dependencies, and accelerating development. Its extensive ecosystem supports production-ready applications, including transaction management and security.  
* **PostgreSQL:**  
  * **Why it fits:** A highly reliable, open-source relational database known for its data integrity (ACID compliance), advanced features (like JSONB for flexible data storage), and excellent performance, making it ideal for audit trails and ledger modeling.  
* **Redis:**  
  * **Why it fits:** Used for high-speed, ephemeral data storage, transaction queueing, and simulating time-to-live (TTL) expiry for ESCROW holds. Its in-memory nature makes it perfect for real-time data and transient states. Also serves as a buffer for WebSocket events.  
* **WebSocket (Spring's STOMP/raw):**  
  * **Why it fits:** Enables real-time, bidirectional communication between the backend and frontend, crucial for live event replay and instant payout status updates.  
* **Spring Cron / Quartz Scheduler:**  
  * **Why it fits:** For scheduling background jobs, such as simulating T+1 settlement processes and implementing automated retry mechanisms for failed transactions.  

##  High-Level Architecture & Flow

The backend acts as the central orchestrator:

1. **Initiation:** The frontend (React) sends a request to the backend's /transactions/initiate API.  
2. **Processing:** The backend records the transaction, manages its state (e.g., INITIATED \-\> ESCROW), potentially queues it in Redis, and simulates various business logic steps.  
3. **Real-time Updates:** As the transaction progresses, the backend pushes state changes and DTO snapshots via WebSockets to the connected frontend.  
4. **Persistence:** All significant transaction events, state changes, and DTO traces are persistently stored in PostgreSQL.  
5. **Scheduled Tasks:** Cron jobs periodically process transactions for simulated settlement (T+1) and manage retries.  
6. **Admin & Debugging:** Dedicated APIs allow for manual intervention, failure injection, and detailed trace retrieval.

##  Database Schema (Core Entities)

The PostgreSQL database stores the critical information for traceability and auditing:

* **transactions**: Main table holding transaction details, current status, amount, and initial payload.  
* **traces**: Stores snapshots of the transaction's DTO at various points in its lifecycle, enabling the "Trace Viewer."  
* **ledger_entry**: Records simulated T+1 settlement details for completed transactions.  
* **users (Optional)**: For authentication and role management if multi-user access is implemented.

##  Getting Started (Backend)

To set up and run the TrackTrove backend locally:

### **Prerequisites**

* Java 17+  
* Maven 3.9.9  
* PostgreSQL (running locally or accessible via connection string)  
* Redis (running locally or accessible via connection string)

### **Setup**

1. **Clone the repository:**  
   git clone https://github.com/AnkitV15/tracktrove-backend.git  
   cd tracktrove-backend

2. **Configure Database & Redis:**  
   * Create a PostgreSQL database (e.g., tracktrove\_db).  
   * Update src/main/resources/application.yml (or application.properties) with your PostgreSQL and Redis connection details.  
   * Example application.yml snippet:  
     spring:  
       datasource:  
         url: jdbc:postgresql://localhost:5432/tracktrove\_db  
         username: your\_pg\_username  
         password: your\_pg\_password  
       redis:  
         host: localhost  
         port: 6379

3. **Build the project:**  
   mvn clean install

4. **Run the application:**  
   mvn spring-boot:run

   The backend will typically start on http://localhost:8080.

### **API Documentation**

Once running, you can access the Swagger UI for API documentation at:  
http://localhost:8080/swagger-ui.html

##  What This Project Shows About Me

This project is designed to showcase:

* **Systems-Oriented Thinking:** My ability to track retries, states, and ledgers cleanly, and design robust, traceable financial flows.  
* **DevTools Building:** My empathy for developers and operations teams, focusing on readability, traceability, and providing powerful debugging tools.  
* **Architectural Prowess:** My capability to architect complex flows that mimic real-world platforms (like Swiggy, Razorpay, Copart) without requiring actual financial integrations.