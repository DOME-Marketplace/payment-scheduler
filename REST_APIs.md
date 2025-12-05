# Payment Scheduler

**Version:** 2.0.0  
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

### Payment Scheduler Controller
| Verb | Path | Task |
|------|------|------|
| GET | `/payment/info` | getInfo |
| GET | `/payment/health` | getHealth |

