package io.pleo.antaeus.core.services

import org.quartz.*
import org.quartz.CronScheduleBuilder.cronSchedule
import org.quartz.JobBuilder.newJob
import org.quartz.TriggerBuilder.newTrigger
import org.quartz.impl.StdSchedulerFactory
import java.nio.file.Path
import java.nio.file.Paths
import org.quartz.Job

/**
 * Invoice Job Class extends quartz job
 * Implements the retrieval and processing of invoices
 */
class InvoiceJob : Job {
    @Throws(JobExecutionException::class)
    override fun execute(context: JobExecutionContext) {
        val dataMap = context.jobDetail.jobDataMap
        val billingService:BillingService = dataMap["billingService"] as BillingService

        println("Processing invoices")

        //process invoices by attempting payment
        billingService.processInvoices("process");
    }
}

/**
 * Pending Invoice Notification Class extends quartz job
 * Sends email notifications to all customers with pending invoices to manually pay
 */
class FailedInvoicePaymentReminderJob : Job {
    @Throws(JobExecutionException::class)
    override fun execute(context: JobExecutionContext) {
        val dataMap = context.jobDetail.jobDataMap
        val billingService:BillingService = dataMap["billingService"] as BillingService

        println("Processing failed invoice reminders")

        //process invoices by sending emails
        billingService.processInvoices("email");
    }
}

/**
 * Invoice Job Scheduler creates a job for scheduling invoices every
 * 5th,10,15 hour of the first day of the month
 * To ensure success, trigger three times in the day before failing
 * Created a fourth schedule to send failed payment notifications to customers.
 */
class InvoiceJobScheduler(private val billingService: BillingService) {
    fun scheduleJob() {
        try {
            // Grab the Scheduler instance from the Factory,initialize and get scheduler
            val sf = StdSchedulerFactory()
            val path: Path = Paths.get("","gradle","quartz.properties")
            sf.initialize(path.toAbsolutePath().toString())

            val scheduler:Scheduler =  sf.scheduler

            //billing service to be passed to invoice job
            val jobMap:JobDataMap = JobDataMap();
            jobMap["billingService"] = billingService;

            //define invoice job and triggers
            val processInvoiceJob: JobDetail = newJob(InvoiceJob::class.java)
                    .withIdentity("invoiceJob", "group1")
                    .usingJobData(jobMap) //pass billing service instance to invoice job
                    .build()

            //define failed invoice remind
            val failedInvoiceJob: JobDetail = newJob(FailedInvoicePaymentReminderJob::class.java)
                    .withIdentity("failedInvoiceReminder", "group1")
                    .usingJobData(jobMap) //pass billing service instance to invoice job
                    .build()

            //TO ENSURE ALL PAYMENTS ARE SUCCESSFUL,
            // ATTEMPT PAYING INVOICE AT LEAST THREE TIMES BEFORE FAIL
            // fire on the 1th day of every month at 5:00am,10:00am,15:00 local timezone
            val processInvoiceCron = cronSchedule("0 0 5,10,15 1 * ?")
            val notifyCustomerCron = cronSchedule("0 0 20 1 * ?")

            //for testing set to every minute for payment processing
            // val processInvoiceCron = cronSchedule("0 * * * * ?")
            // val notifyCustomerCron = cronSchedule("0 */5 * * * ?")

            //process invoice trigger
            val processInvoiceTrigger = newTrigger()
                    .withIdentity("processInvoiceTrigger", "group1")
                    .startNow()
                    .withSchedule(processInvoiceCron)
                    .build();

            //failed invoice trigger
            val failedInvoiceTrigger = newTrigger()
                    .withIdentity("failedInvoiceTrigger", "group1")
                    .startNow()
                    .withSchedule(notifyCustomerCron)
                    .build();

            // Schedule the processing of invoice and notification job trigger
            scheduler.scheduleJob(processInvoiceJob, processInvoiceTrigger);
            scheduler.scheduleJob(failedInvoiceJob, failedInvoiceTrigger);
            scheduler.start()
        } catch (se: SchedulerException) {
            se.printStackTrace()
        }
    }
}