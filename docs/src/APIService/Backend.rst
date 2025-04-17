.. _Backend:

Backend
==========================

The backend is where all the logic of the Packtrain lives. It is implemented using **Java Spring**.

Key Responsibilities and Integrations
-------------------------------------

- **Framework:**  
  Developed using Java Spring, the backend handles routing, business rules, and external service management.

- **Message Queuing (RabbitMQ):**  
  Utilizes `RabbitMQ <https://www.rabbitmq.com/>`_ to send raw scores to the policy server, enabling asynchronous communication from the backend and the policy server.

- **Database (PostgreSQL):**  
  Interfaces with a PostgreSQL database for storage of all application data, including assignment records, grading information, and configuration settings.

- **Frontend Communication via OpenAPI:**  
  Exposes REST endpoints through OpenAPI routes, allowing the frontend to interact with the backend for data retrieval and updates.

- **Authentication and Authorization:**  
  Acts as an OAuth Resource Server by validating Bearer JWTs. It relies on :ref:`Authentik <Authentication>` as the authentication server, offloading credential management and ensuring that only authorized requests are processed.

- **External Services Integration:**  
  - **CSV Uploads:** Supports the uploading of CSV files for systems like Runestone and PrairieLearn.  
  - **API Scraping:** Integrates with external platforms such as Canvas and Gradescope by scraping their APIs.

.. toctree::
  :maxdepth: 1
  :caption: Services:
  
  Services/Assignment
  Services/CourseMember
  Services/Course
  Services/Credential
  Services/Extension
  Services/Migration
  Services/RawScore
  Services/Section
  Services/TaskExecutor
  Services/User
  Services/Canvas
  Services/Gradescope
  Services/PolicyServer
  Services/RabbitMQ
  Services/S3