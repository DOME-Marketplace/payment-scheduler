# Release Notes

**Release Notes** of the *Payment Scheduler* software:


### <code>0.0.1</code> :calendar: 13/02/2025
**Feature**
* Add swagger UI for REST APIs.
* Add `StartupListener` listener to log (display) the current version of *Billing Scheduler* at startup.
* Usage of the `BILLING_PREFIX` in the `application.yaml` file.

**BugFixing**
* Add `validation` dependency.
* Set `org.apache.coyote.http11: ERROR` to avoid the `Error parsing HTTP request header`.
