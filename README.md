# About SZEBI

**SZEBI** is a comprehensive, scalable analytics and reporting ecosystem built with **Java 25**, **Maven**, and a modern **web frontend**. Operating within a robust containerized infrastructure (Docker, leveraging both PostgreSQL and MongoDB), the system is explicitly engineered to handle the complete lifecycle of data pipelines—from raw data acquisition through advanced analytics, optimization, and forecasting.

### System Architecture & Core Modules
The backend is structured as a multi-module Maven application, ensuring clean separation of concerns and high extensibility. The key modules include:

*   **Acquisition (`modules/acquisition`)**: Responsible for gathering and ingesting data from external sources into the system reliably.
*   **Database API (`modules/database`)**: The central persistence layer that abstracts data storage logic, utilizing **PostgreSQL** for relational structures and **MongoDB** for flexible, document-based storage.
*   **Analysis & Reporting (`modules/analysis-report`)**: Transforms raw metrics into readable, actionable insights and standardized reports.
*   **Optimization (`modules/optimization`)**: Provides powerful business logic and algorithms to optimize processes and resource allocation based on the collected data.
*   **Forecast (`modules/forecast-module`)**: Integrates predictive modeling and trend analysis to estimate future data behaviors.
*   **Alerts (`modules/alerts-module`)**: A real-time monitoring engine that tracks system anomalies and triggers immediate notifications based on customized thresholds.

### Tech Stack
*   **Backend:** Java 25, Apache Maven
*   **Frontend:** Node.js, Vite
*   **Database:** PostgreSQL, MongoDB
*   **Infrastructure:** Docker & Docker Compose