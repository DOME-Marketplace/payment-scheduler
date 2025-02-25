# Release Notes

**Release Notes** of the *Payment Scheduler* software:


### <code>0.0.2</code> :calendar: 25/02/2025
**Feature**
* Add EG API payment `/payment-start-non-interactive` (only for testing).


### <code>0.0.1</code> :calendar: 21/02/2025
**Feature**
* Add swagger UI for REST APIs.
* Add `StartupListener` listener to log (display) the current version of *Payment Scheduler* at startup.
* Usage of the `BILLING_PREFIX` in the `application.yaml` file.
* Add a POST API `/payment/start` (no payload need) to start scheduler **manually**.
* Usage of custom `ControllerExceptionHandler`.

**BugFixing**
* Add `validation` dependency.
* Set `org.apache.coyote.http11: ERROR` to avoid the `Error parsing HTTP request header`.
