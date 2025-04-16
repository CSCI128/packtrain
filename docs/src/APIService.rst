.. _APIService:

API Service
==========================

At a high level, the API is divided into two main components:

1. **Frontend** (User Interface)
2. **Backend** (REST API + Business Logic)

The frontend manages the user interface and all user interactions. After obtaining a valid access token from the authorization server, Authentik, it makes authenticated requests to the backend using OpenAPI-specified routes.

The backend then receives requests from the frontend, then as an OAuth resource server, verifies the incoming Bearer JWTs against Authentik. Upon successful validation, the backend proceeds with the business logic, such as retrieving or updating grading information, interacting with external services (e.g., Canvas, Gradescope, Runestone, PrairieLearn), and storing policies (in S3).

Table of Contents
-----------------

Below are links to more in-depth documentation on each major component of the API service:

.. toctree::
   :maxdepth: 1

   APIService/Frontend
   APIService/Backend
