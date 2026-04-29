# 🏫 TinySpring Garderie

A comprehensive and modern **Daycare Management System** (Système de gestion de garderie) designed to streamline operations for administrators, parents, and animators. The application provides dedicated portals, advanced scheduling, interactive data visualization, and AI-driven insights.

---

## ✨ Key Features

- **🎓 Class & Student Management**: Full CRUD capabilities for classes and students, featuring an intuitive **Drag-and-Drop Kanban interface** for seamless student transfers between classes.
- **🛡️ Anti-Collision Scheduling Engine**: Advanced validation to prevent time-overlap conflicts for animators and rooms, ensuring a smooth and error-free schedule.
- **📊 Real-Time Analytics Dashboard**: A business intelligence dashboard for administrators, providing analytical insights, visual charts, and data aggregation for daycare operations.
- **🚦 Capacity Enforcement**: Visual occupancy gauges for real-time monitoring of classroom capacity, preventing overbooking.
- **🤖 AI Integration**: A dedicated Python ML service providing intelligent insights and recommendations based on activity and room datasets.
- **🔒 Role-Based Access Control (RBAC)**: Secure access with distinct roles for Admins, Parents, and Animators, ensuring users only see what they need to.

---

## 🛠️ Technology Stack

The project is structured into 4 distinct modules:

1. **Backend (API)**
   - **Java 17** & **Spring Boot**
   - Spring WebMVC, Spring Data JPA, Spring Security
   - MySQL (Runtime) / H2 Database (Testing)
   - Maven & Lombok

2. **Front-Office (Parents & Animators)**
   - **Angular**
   - Node.js & npm

3. **Back-Office (Administrators)**
   - **Angular**
   - Node.js & npm

4. **Python ML (AI Service)**
   - **Python 3**
   - **FastAPI**
   - Scikit-learn / Pandas (for ML models)

---

## 🚀 Getting Started

### Prerequisites
Make sure you have the following installed on your system:
- **Java 17+**
- **Node.js** (v18+ recommended) and **npm**
- **Python 3.8+**
- **MySQL Server** (running locally)

### Installation & Execution

We provide a convenient bash script to install dependencies and launch all four services concurrently.

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   cd validation_blanche
   ```

2. **Database Setup:**
   Ensure your MySQL server is running. You may need to create the initial database schema depending on your `application.properties` configuration in the Spring Boot backend. Alternatively, you can use the provided `populate.sql` script to seed the database:
   ```bash
   mysql -u root -p < populate.sql
   ```

3. **Make the start script executable:**
   ```bash
   chmod +x start.sh
   ```

4. **Run the application:**
   ```bash
   ./start.sh
   ```
   *Note: The script will automatically install missing `node_modules` and set up the Python virtual environment if required.*

### 🌐 Services Overview

Once the script is running, the services will be available at the following URLs:

| Service | URL | Role / Purpose |
| :--- | :--- | :--- |
| **Backend API** | `http://localhost:8081` | Core Spring Boot REST API |
| **Front-Office** | `http://localhost:4200` | Interface for Parents & Animators |
| **Back-Office** | `http://localhost:4201` | Dashboard for Administrators |
| **Python ML** | `http://localhost:8000` | AI/ML Recommendation Service |

### 🔑 Test Accounts

You can use the following credentials to test the application across the different portals:

- **Administrator** (Back-office)
  - Email: `admin@garderie.com`
  - Password: `admin123`

- **Parent** (Front-office)
  - Email: `parent@garderie.com`
  - Password: `parent123`

- **Animatrice** (Front-office)
  - Email: `animatrice@garderie.com`
  - Password: `anim123`

---

## 🛑 Stopping the Application

To gracefully stop all services (Backend, Frontends, and Python ML), simply press `Ctrl+C` in the terminal where `start.sh` is running. The script will automatically terminate all child processes.