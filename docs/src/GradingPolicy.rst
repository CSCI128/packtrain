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

PolicyScoredDTO (Data Transfer Object)
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Your policy must return an object with these properties set:

    **finalScore** *(number)*  
        The adjusted score after applying your policy.

    **adjustedSubmissionDate** *(Date)*  
        The (possibly modified) submission timestamp.

    **adjustedDaysLate** *(number)*  
        The number of days late after policy adjustments.

    **submissionStatus** *(SubmissionStatus enum)*  
        Updated status, one of::  

            ON_TIME
            LATE
            MISSING

    **extensionStatus** *(AppliedExtensionStatus enum)*  
        How the extension was applied, one of::

            APPLIED
            DENIED
            NONE

An example for format is below:

.. code-block:: js
    :linenos:


    return {
        finalScore: rawScore.rawScore,
        adjustedSubmissionDate: new Date(),
        adjustedDaysLate: 0,
        submissionStatus: "on_time",
        extensionStatus: "no_extension"
    };

