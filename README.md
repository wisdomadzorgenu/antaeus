## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

### Implementation Overview
I chose Quartz Scheduler to handle the processing of invoices as it is an open-source job scheduling library that can be integrated within virtually any Java/Kotlin application.

Quartz can be used to create simple or complex schedules for executing tens, hundreds, or even tens-of-thousands of jobs. Quartz offers job scheduling through patterns like cronjobs available in Unix systems. This is a great way of leveraging existing cronjob documentations as a quick guide.


### Major Changes
 - Added Quartz dependency in Antaeus core module.
 - Created a quartz.properties configuration file under Gradle directory.
 - Created Invoice Job Scheduler with quartz and added cron triggers to execute the payment of invoices and send notifications for failed payments.
 - Added Invoice and Customer Services to BillingService.
 - Imported Invoice Job Scheduler in Antaeus App and executed scheduled job.
 - Added new methods for retrieving and updating invoices in batches in InvoiceService and CustomerService
 - Implemented the logic for issuing payments and updating invoices.
 - Added new methods for notifying customers of successful payment or failed ones.
 - Implemented the logic for notifying customers of failed invoice payments.
 

### Invoice Scheduling
 - I configured the scheduler to execute every 5th, 10th, and 15th hour of the first day of every month.
 - I provided a 5-hour gap between schedules to ensure invoice processing is complete before the next cycle. In real production environments, multiple servers handle processing to speed up the process.
 - Three periodic schedules ensure that all unpaid invoices are attempted three times after which they fail.
 - A fourth schedule is executed on the 20th hour to alert all customers with pending invoices to manually pay through the provided portal.

`Note: Ideally, in a real production environment, schedules should happen after failing for a few days before notifying customers of the need to manually pay.`
 
### Algorithm For Processing Invoices
 -  Count Unpaid Invoices
 -  Retrieve Invoices in batches 50/iteration asynchronously
 -  Process Payments for retrieved invoices asynchronously
 -  Retry processing payment if failed or an exception occurred
 -  Update invoices if payment is successful
 -  Send email notification to customers of successful payment
 -  Retrieve next bath of invoices
 
### Algorithm For Notifying Customers With Failed Invoices Payments
 -  Retrieve Invoices in Batches 50/iteration asynchronously
 -  Send email notification to customers of payment requirement
 -  Retrieve next bath

`Note: Processing invoices in batches is ideal because the server is not overwhelmed with too many database results. In a typical real production environment, retrieving all invoices for thousands/millions of users into memory is not advised. Rather, retrieving in batches consumes less memory and guarantees successful execution.`

### Batch Retrieval
Retrieving unpaid invoices using SQL offset may fail if payment processing for an invoice fails. Usually, when payments are processed, invoice statuses are updated in the database. This leads to a difference in the actual database count and the unpaid invoice count variable before the loop. Using SQL offsets will make the database re-process existing failed invoices or fail to process subsequent invoices down the invoice table when the while loop terminates.

The goal of batch retrieval is to ensure that only pending invoices are processed before the next execution cycle without needing to retrieve all invoice results.

To ensure every unpaid invoice is processed:
- A variable stores the last processed invoice
- Query commands retrieve the next batch of outstanding invoices after the last invoiceId

`Implementing this way ensures that even after updating invoice status, previously failed invoice payments are not retrieved. Only results after the last processed are retrieved.`

### Mock Functions
 - sendCustomerPaymentNotification
 - sendCustomerFailedPaymentNotification
 
 
The email notification handlers are mock functions which I assume are implemented and will be plugged in. However, to ensure functionality works I return true.

 
### Scaling
In the real world, at least two microservices should exist to prevent any single point of failure. One service handles RESTful services and the second for monthly invoice processing.

Managing jobs is handled in RAM as configured in quartz.properties for this small use case. In a real server environment where lots of schedules may exist, storing information about jobs in a database will be ideal as system administrators can easily monitor the health and status of the jobs as well as manually retry for missed jobs.

### Length of Project
I did not know Kotlin, to begin with, so it took me a day to quickly learn the basics of Kotlin and to be up and running as well know my way around the codebase of this challenge. It took me 8hrs to complete the task and update the challenge’s readme.

### Running Tests
```sh
    cd projectDir
    ./gradlew test
```

## Developing

Requirements:
- \>= Java 11 environment

Open the project using your favorite text editor. If you are using IntelliJ, you can open the `build.gradle.kts` file and it is gonna setup the project in the IDE for you.

### Building

```
./gradlew build
```

### Running

There are 2 options for running Anteus. You either need libsqlite3 or docker. Docker is easier but requires some docker knowledge. We do recommend docker though.

*Running Natively*

Native java with sqlite (requires libsqlite3):

If you use homebrew on MacOS `brew install sqlite`.

```
./gradlew run
```

*Running through docker*

Install docker for your platform

```
docker build -t antaeus
docker run antaeus
```

### App Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── buildSrc
|  | gradle build scripts and project wide dependency declarations
|  └ src/main/kotlin/utils.kt 
|      Dependencies
|
├── pleo-antaeus-app
|       main() & initialization
|
├── pleo-antaeus-core
|       This is probably where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|       Module interfacing with the database. Contains the database 
|       models, mappings and access layer.
|
├── pleo-antaeus-models
|       Definition of the Internal and API models used throughout the
|       application.
|
└── pleo-antaeus-rest
        Entry point for HTTP REST API. This is where the routes are defined.
```

### Main Libraries and dependencies
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library
* [Sqlite3](https://sqlite.org/index.html) - Database storage engine
* [Quartz](http://www.quartz-scheduler.org) - Quartz Scheduler
