.. _GradingPolicy:

Grading Policy Server
==========================

This should talk about what the grading policy server is and what it uses
user sends in \*.js, stored in S3 bucket
backend calls start grading via sending raw scores, routed through RabbitMQ
policy server takes raw scores, applies policies, sends back new scores via RabbitMQ
backend takes new scores and applies to each assignment