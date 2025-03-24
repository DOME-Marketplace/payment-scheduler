# Release Notes

**Release Notes** of the *Payment Scheduler* software:

### <code>0.0.7</code> :calendar: 19/03/2025
**BugFixing**
* Create `M2MTokenService` class to build the **client_assertion** for retrieving token from VC Verifier.
* Updated PaymentScheduler service to retrieve the `paymentPreAuthorizationId` from the product's characteristics.

**Feature**
* Use of the `aggregate` feature before payment to group (aggregate) same ApplyCustomerBillRates by **EndDateTime**.


### <code>0.0.6</code> :calendar: 07/03/2025
**BugFixing**
* Update of the `PaymentStartNonInteractive` class.
* Read **EGPayment** object from `EG Payment Service`.


### <code>0.0.5</code> :calendar: 04/03/2025
**BugFixing**
* Usage of the `LinkedMultiValueMap()` for **application/x-www-form-urlencoded**.


### <code>0.0.4</code> :calendar: 03/03/2025
**BugFixing**
* Update **body** for the `getVCVerifierToken()` method.


### <code>0.0.3</code> :calendar: 27/02/2025
**Feature**
* Add `VC Verifier` API to get the **token** for API Payment.


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
