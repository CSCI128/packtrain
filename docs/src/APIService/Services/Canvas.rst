.. _CanvasService:

Canvas Service
==============

The **CanvasService** wraps calls to the Canvas LMS API, handles token acquisition via the ``SecurityManager``.  It is composed of two parts:

  1. ``CanvasService`` manages configuration and factory setup.  
  2. ``CanvasServiceWithAuth`` contains all Canvas operations, under an authenticated context.

Configuration & Initialization
------------------------------

- Reads ``ExternalServiceConfig.CanvasConfig`` to find the Canvas endpoint and check whether if service is enabled.
- If disabled, any call to ``withRequestIdentity()`` or ``asUser(...)`` will throw ``ExternalServiceDisabledException``.
- On startup, initializes a ``CanvasApiFactory`` pointed at the Canvas endpoint.

Authentication Context
----------------------

- ``withRequestIdentity()``
  Returns a ``CanvasServiceWithAuth`` bound to a ``SecurityManager``, throwing if no request is active.
- ``asUser(IdentityProvider provider)``  
  Returns a ``CanvasServiceWithAuth`` using the given identity provider.

CanvasServiceWithAuth API
-------------------------
Wraps Canvas API calls using an Auth token obtained from  
``identityProvider.getCredential(CredentialType.CANVAS, /* dummy UUID */)``

Methods:
    - **getAllAvailableCourses()** → ``List<Course>``
        Lists all courses where the user has a “TEACHER” enrollment type.  
    - **getCourse(String id)** → ``Optional<Course>`` 
        Fetch a single course by Canvas course ID.  
    - **getCourseMembers(long id)** → ``Map<String, User>``  
        Returns a map of SIS IDs to Canvas ``User`` objects for all active teachers & students in the course.  
    - **getCourseSections(long id)** → ``List<Section>``  
        Lists all sections for a given course.  
    - **getAssignmentGroups(long id)** → ``Map<Long, String>`` 
        Maps assignment‑group IDs to names for the course.  
    - **getCourseAssignments(long id)** → ``List<Assignment>``  
        Lists all assignments in the course (including group assignments with ``groupCategoryId``).  

Role Mapping
------------

- ``Optional<CourseRole> mapEnrollmentToRole(Enrollment enrollment)``  
  Uses ``config.getTeacherEnrollment()``, ``getStudentEnrollment()``, and ``getTaEnrollment()`` strings to map Canvas’s enrollment types into our ``CourseRole`` enum.

Error Handling & Logging
------------------------

- All network calls catch ``IOException``, log an error, and return empty collections or ``Optional.empty()``.
- Informational logs record the start and end counts of items retrieved for transparency.
