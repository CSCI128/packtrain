.. _Authentication:

Authentication
==========================
`Authentik <https://goauthentik.io/>`_ is an identity broker and authentication server that manages integrations for all identity providers (Canvas, etc).

In this setup:
    - Authentik is the primary authority for user authentication. It issues JWTs that the :ref:`frontend <Frontend>` can use to call the :ref:`backend <Backend>`.

    - Authentik manages scopes (i.e., what each user/role can do) and enforces these scopes when handing out tokens.
        - Authentik maps external roles (e.g., “Teacher” or “Student” in Canvas) to the internal roles (Instructor, Student)

    - Backend receives the JWT from the frontend and uses Authentik to validate the token and verify the user’s permissions before giving access to data or performing actions.

.. image:: images/authflow.png
    :alt: This is an image showing a system diagram of the auth flow in Packtrain

The above figure displays the previously mentioned authorization flow.

Admin Authentication Level
--------------------------

- **Manages Policies:**  
  Can create, modify, and enforce course-wide/assignment-based policies.
- **Instructor-level Access to All Sections:**  
  Can view and manage every section of the course.

Instructor Authentication Level
-------------------------------

- **Course Section Management:**  
  Can manage their specific course sections, including assignments, grades, and rosters.
- **Handles Extension Requests:**  
  Responsible for approving or denying student requests for assignment extensions.
- **Manages Late Passes:**  
  Oversees the issuance and consumption of late passes.
- **Master Migration:**  
  Responsible for managing migrations.

Student Authentication Level
----------------------------

- **Extension Requests:**  
  Can submit requests for assignment extensions.
- **Late Passes:**  
  Can view and apply available late passes.

Server‑side Enforcement of Roles
--------------------------------

All API endpoints delegate authorization to our **SecurityManager** (in  
``edu.mines.gradingadmin.managers.SecurityManager``).  Its job is to:

1. **Extract** the incoming user’s JWT from the servlet request (via ``setPrincipalFromRequest(HttpServletRequest)``).

2. **Lookup** the ``User`` entity (in ``readUserFromRequest()``), by:
    - Looking up by ``oauthId`` (the JWT’s sub claim).
    - Fall back to linking an existing user by CWID.
    - Creating a new user if no match is found.

3. **Simple checks** that controllers or service methods call:
    - ``getIsAdmin()`` → reads the ``is_admin``` claim from the JWT and throws ``AccessDeniedException`` if false. Use it to gate any admin operations.  
    - ``hasCourseMembership(CourseRole, UUID)`` → looks up the user’s role for a given course via ``CourseMemberService.getRoleForUserAndCourse(...)`` and compares it to the requested ``CourseRole`` enum (Instructor, Student, etc).
    - ``getCredential(CredentialType, UUID)`` → fetches the user’s per‑user or per‑course credential (e.g. Canvas API tokens) and fails if none exist.

4. **Throw** ``AccessDeniedException`` immediately when any check fails.

An example of using this to check a user's privileges is within the Backend's filters,

.. code-block:: Java
  :linenos:
  
  ...
  if (!securityManager.hasCourseMembership(CourseRole.INSTRUCTOR, UUID.fromString(courseId)) && 
      !securityManager.hasCourseMembership(CourseRole.OWNER, UUID.fromString(courseId))){
    log.warn("Blocked attempt by '{}' to access course '{}' as an instructor!",
            securityManager.getUser().getEmail(),
            courseId);
    throw new AccessDeniedException("Not enrolled in course as an instructor!");
  }
  ...


