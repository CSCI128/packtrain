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

Service Breakdown
-----------------

AssignmentService
^^^^^^^^^^^^^^^^^

Handles everything related to assignments within a course, including syncing with Canvas and scheduling background sync tasks.

CourseMemberService
^^^^^^^^^^^^^^^^^^^

Manages enrollment data (students, instructors) within courses and syncing with Canvas

CourseService
^^^^^^^^^^^^^

Synchronization for course metadata, late‑request configurations, and course‑wide policies.

CredentialService
^^^^^^^^^^^^^^^^^

Per‑user or per‑course API credentials (e.g., Canvas/Gradescope API keys).

ExtensionService
^^^^^^^^^^^^^^^^

Handles student requests for extensions and late‑pass applications on assignments.

MigrationService
^^^^^^^^^^^^^^^^

Manages singular migrations, tying assignments to policies, importing raw scores, and sending results to the policy server via RabbitMQ.

RawScoreService
^^^^^^^^^^^^^^^

Imports CSV exports (Gradescope, PrairieLearn, Runestone), normalizes them into ``RawScore`` entities, and tracks import progress via ``MigrationService``.

SectionService
^^^^^^^^^^^^^^

Syncs Canvas course sections into the database.

TaskExecutorService
^^^^^^^^^^^^^^^^^^^

Service that listens for ``NewTaskEvent`` s and runs scheduled tasks asynchronously.

UserService
^^^^^^^^^^^

Manages users, linking CWIDs to OAuth IDs, creating new users from Canvas data, and handling admin/staff roles.

CanvasService
^^^^^^^^^^^^^

Provides authenticated access to Canvas APIs for courses, users, sections, assignments, and mapping enrollments to roles.

GradescopeService
^^^^^^^^^^^^^^^^^

Handles authentication and CSV download for Gradescope assignments.

PolicyServerService
^^^^^^^^^^^^^^^^^^^

Manages interactions with the grading policy server.

RabbitMqService
^^^^^^^^^^^^^^^

Orchestrates RabbitMQ connections and channels for raw grades and scored messages.

S3Service
^^^^^^^^^

Integrates with an S3 storage (MinIO in development) for course buckets and policy documents.
