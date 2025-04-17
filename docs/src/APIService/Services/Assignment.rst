.. _AssignmentService:

Assignment Service
=====================

This is responsible for managing **Assignment** entities and synchronizing course assignments with Canvas.

Responsibilities
----------------

- **Fetch by ID**  
  ``Optional<Assignment> getAssignmentById(String id)``  
  Lookup an assignment by its UUID string.

- **Create, Update & Toggle**  
    - ``Optional<Assignment> addAssignmentToCourse(String courseId, AssignmentDTO assignmentDTO)``
        - Create a new assignment in the given course.  
    - ``Optional<Assignment> updateAssignment(String courseId, AssignmentDTO assignmentDTO)``  
        - Update fields (name, points, category, dates, enabled) on an existing assignment.  
    - ``Optional<Assignment> enableAssignment(String assignmentId)`` /  ``disableAssignment(String assignmentId)``  
        - Flip the ``enabled`` flag on an assignment.

- **List Unlocked**
    - ``List<Assignment> getAllUnlockedAssignments(String courseId)``
        - Return only those assignments whose ``unlockDate`` is null or in the past.

Canvas Synchronization
----------------------

- **Enqueue a Sync Task**  

.. code-block:: Java
  :linenos:

  Optional<ScheduledTaskDef> syncAssignmentsFromCanvas(
      User actingUser,
      Set<Long> dependencies,
      UUID courseId,
      boolean addNew,
      boolean deleteOld,
      boolean updateExisting
  )