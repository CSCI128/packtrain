.. _GradingPolicy:

Grading Policy Server
==========================

The **Grading Policy Server** is the server that handles grading policies on the raw scores of assignments.

This is a typescript application that runs asynchronously from the primary :ref:`API Service <APIService>`.
The steps of the grading policy is as follows:

1. The user can upload a created policy from the web UI (*\*.js*), which will be stored within a S3 bucket.
2. Starting the migration from the Web UI will call the ``startGrading`` method of the API Service, which will send a signal to the grading policy server to subscribe to the message broker, `RabbitMQ <https://www.rabbitmq.com/>`_.
3. Through communication via RabbitMQ, the API Service and the Policy server will convert raw scores using the respective policy from the S3 bucket to the new scores, of which the API Service will receive.

This visualization from the system architecture design can depict this process;

.. image:: images/backendpolicy.png
    :alt: Image of the described grading policy and backend interaction.
    :align: center

Policy Format
----------------

RawScoreDTO
^^^^^^^^^^^

The following fields are aviable from the rawscore:
    **cwid** *(string)*
        The ID of the student who is being graded.

    **assignmentId** *(string)*
        The UUID of the currently graded assignment. Mostly aviable for information purposes. 
        There isn't a way to get the actual assignment in the policy service with this id.
    
    **rawScore** *(number)*
        The student's score before any processing has been applied.

    **canvasMinScore** *(number)*
        The minimum configured score for the assignment.
        Generally, it is zero.

    **canvasMaxScore** *(number)* 
        The maximum score allowed for an assignment.
        This score comes from canvas

    **externalMaxScore** *(number)* 
        The maximum score allowed for an assignment from the external service.
        This score comes from what ever external service has been configured.
        If no external service exists, then the Canvas maxScore will be used.

    **initialDueDate** *(string)*
        The inital due date for the assignment. 
        Can (and should) be converted to a date by passing it into a new date object.

    **submissionDate** *(nullable string)*
        The submission date for the student's submission.
        If the student's submission is missing, then it is null.
        Can (and should) be converted to a date by passing it into a new date object.

    **submissionStatus** *(SubmissionStatus)*
        The student's submission status.
        Must be one of::

            missing
            excused
            late
            extended
            on_time
        
        In general, only ``missing``, ``late``, and ``on_time`` will be set for raw scores.

    **extensionStatus** *(IncomingExtensionStatus)*
        The student's extension status.
        Must be one of::

            ignored
            approved
            rejected
            pending
            no_extension
            applied

        If the student does not have an extension applied, then they will have ``no_extension`` set.
        In general, only ``approved``, ``rejected``, ``pending`` will be set for raw scores.
        
        If the extension is in the ``pending`` state, then no instructor has taken an action on the extension.
        These can be treated as either implict approvals or implict rejections, but your policy should set it.

    **extensionDate** *(nullable string)*
        The students new due date given the extension.
        This string is null if the student has no extension.
        Can (and should) be converted to a date by passing it into a new date object.

    **extensionDays** *(nullable number)*
        The total number of days that the student requested.

    **extensionType** *(nullable string)*
        The type of extension that the student requested.
        Generally only ``Late Pass`` should be explictly checked as the other options are what ever the student said.
    

PolicyScoredDTO 
^^^^^^^^^^^^^^^

Your policy must return an object with these properties set:

    **finalScore** *(number)*  
        The adjusted score after applying your policy.

    **adjustedSubmissionDate** *(Date)*  
        The (possibly modified) submission timestamp.
        This MUST be a date and not a string.

    **adjustedDaysLate** *(number)*  
        The number of days late after policy adjustments.

    **submissionStatus** *(SubmissionStatus)*  
        Updated submission status, one of::  
            
            missing
            excused
            late
            extended
            on_time

    **extensionStatus** *(ExtensionStatus)*  
        How the extension was applied, one of::
            
            ignored
            approved
            rejected
            pending
            no_extension
            applied

    **extensionMessage** *(nullable string)*
        An optional message describing if / how extensions were applied.

    **submissionMessage** *(nullable string)*
        An optional message for the overall submission.
    
    **numberExtensionDaysApplied** *(number)*
        the number of extension days applied

An example for format is below:

.. code-block:: js
    :linenos:

    let submissionStatus = "on_time";

    if (rawScore.submissionDate > rawScore.initialDueDate){
        submissionStatus = "late";
    }

    let submissionComment = "Nice work!"

    if (rawScore.submissionStatus === "missing"){
        submissionComment = "Nothing submitted! Contact your instructor!";
        submissionStatus = "missing";
    }

    let score = rawScore.rawScore;

    if (submissionStatus === "late"){
        submissionComment = "Submitted late :( -50%";
        score *= .50;
    }

    return {
        finalScore: score,
        adjustedSubmissionDate: rawScore.submissionDate,
        adjustedDaysLate: 0,
        submissionStatus: submissionStatus,
        extensionStatus: "no_extension",
        submissionMessage: submissionComment,
    };

