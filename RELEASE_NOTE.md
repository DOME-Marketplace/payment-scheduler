# Release Notes

**Release Notes** of the *Payment Scheduler* software:

### <code>1.2.0</code> :calendar: 26/05/2025
**Improvements**
* Set of `[2.1.0, 2.2.0)` version of `Brokerage Utils`.
* Use **filter** features for retrieving the **AppliedCustomerBillingRate** with `isBilled = false` and `type = recurring` query string params (**Brokerage Utils** version `2.1.0`).


### <code>0.1.1</code> :calendar: 20/05/2025
**Improvements**
* Add **TMForumService** class to manage AppliedCustomerBillRate object.
* Verify **token** validity by using `ext` (expired) **claim** of JWT.
* Remove the `customerId` attribute from Payment payload to send to Payment Gateway.
* Add new `paymentMethodType` attribute in **EGPaymentResponse - Payout** class.
* Usage of *cron job task format* `0 30 */3 * * ?` (sec, min, hour, month, day of week). 

**Feature**
* Payment management of Payment Gateway response based on statuses `PROCESSED`, `FAILED`, `PENDING` in the **scheduling process**. 
* Payment management of Payment Gateway based on statuses `SUCCEEDED`, `FAILED` in the **notification process**. 


### <code>0.1.0</code> :calendar: 06/05/2025
**Improvements**
* Usage of `2.0.0` version of `Brokerage Utils`.
* Add new **env vars**: `VC_VERIFIER_TOKEN_URL`, `SCHEDULING_CRON_JOB_TASK`, and `VC_VERIFIER_EXTERNAL_DOMAIN`.

**BugFixing**
* Change EG Payload adding `External` prefix.
* Get `customerOrganizationId` and `productProviderExternalId` from **relatedParty** of the product. 


### <code>0.0.9</code> :calendar: 27/03/2025
**BugFixing**
* Set **kubernetes secret** (sealed-secret) properly **encoded** for `LEAR_CREDENTIAL_BASE64` and `PRIVATE_KEY_BASE64`.
* Use **lear-credential-machine** provided by Dome ticket in base64 format, and encode it. Use this encoded in the plain-secret.yaml (`LEAR_CREDENTIAL_BASE64`) to create sealed-secret with `kubeseal`. 
* Use **private-key** provided by crypto generator in hex format. Convert it from hex to Base64, and encode it. Use it in the plain-secret.yaml (`PRIVATE_KEY_BASE64`) to create sealed-secret with `kubeseal`. 


### <code>0.0.8</code> :calendar: 23/03/2025
**Feature**
* Create **kubernetes secret** (sealed-secret) and set `env var` to be used a run-time.


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
