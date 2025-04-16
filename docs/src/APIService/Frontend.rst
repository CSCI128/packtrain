.. _Frontend:

Frontend
==========================

The frontend of the grading admin application is responsible for the user interface of the system. It manages user and data interaction with the :ref:`backend's <Backend>` REST API. The frontend is architected into three applications that run concurrently:

- **Admin App:** This contains the bulk of the code, responsible for most of the user and course management.
- **Student App:** This displays course information, assignments, and grades, allowing students to request extensions or use late passes.
- **Instructor App:** This provides tools for instructors to manage their course section through approving extensions, managing migrations, and viewing students in their course.

Workflow and Interaction
------------------------

1. **User Authentication:**  
   The user initiates an OAuth flow on :ref:`Authentik <Authentication>` to obtain a JWT. This token is then used in subsequent API calls to ensure secure communication with the backend.

2. **Data Fetching:**  
   Uses Tanstack React Query and its OpenAPI React Query integration

3. **UI Rendering:**  
   Mantine components render the UI elements

4. **Separation of Concerns:**  
   By splitting the frontend into distinct applications (admin, student, instructor), each group of users interacts with a tailored interface that meets their specific needs.
