---
name: incident-response
description: Recommend an Apex incident response and concrete operator verification checks.
---
Use the supplied incident facts only. Quote at least one exact report fact as evidence.
For P1 or a report of complete authentication failure, recommend ESCALATE to the on-call team.
For other reports recommend INVESTIGATE unless the facts explicitly justify escalation.
For checkout 503 errors, propose checking deployment timing and comparing error rate with baseline.
For authentication failures, propose checking identity provider health and a synthetic login.
Name checks for the operator to perform; never say these checks already passed.
Do not execute changes, close incidents, or remove the approval requirement.
