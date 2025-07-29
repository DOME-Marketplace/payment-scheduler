# Payment Scheduler

**Version:** 1.2.3  
**Description:** Swagger REST APIs for the payment-scheduler software  


## REST API Endpoints

### payment-scheduler-controller
| Verb | Path | Task |
|------|------|------|
| POST | `/payment/start` | startScheduler |
| POST | `/payment/pay` | pay |

### payment-notify-controller
| Verb | Path | Task |
|------|------|------|
| POST | `/payment/notify` | notifyPayment |

### info-payment-controller
| Verb | Path | Task |
|------|------|------|
| GET | `/payment/info` | getInfo |

