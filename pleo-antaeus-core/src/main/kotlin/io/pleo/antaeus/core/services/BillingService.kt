package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import org.quartz.*
import org.quartz.CronScheduleBuilder.cronSchedule
import org.quartz.JobBuilder.newJob
import org.quartz.TriggerBuilder.newTrigger
import org.quartz.impl.StdSchedulerFactory
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.*
import java.nio.file.Paths


class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService
) {
// TODO - Add code e.g. here
}

/**
 * Invoice Job Class extends quartz job
 * which has the implementation of retrieving all invoices and updating payments
 */
class InvoiceJob : Job {
    @Throws(JobExecutionException::class)
    override fun execute(context: JobExecutionContext) {
        val dataMap = context.jobDetail.jobDataMap
        val billingService = dataMap["billingService"]

        //ALGORITHM FOR RUNNING INVOICE PAYMENT
        val sdf = SimpleDateFormat("dd/M/yyyy hh:mm:ss")
        val currentDate = sdf.format(Date())
        println("Current DATE is  $currentDate")
    }
}

/**
 * Invoice Job Scheduler creates a job for scheduling invoices every
 * 6th,12,18 hour of the first day of the month
 * To ensure success, trigger three times in the day before failing
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
            val invJob: JobDetail = newJob(InvoiceJob::class.java)
                    .withIdentity("invoiceJob", "group1")
                    .usingJobData(jobMap) //pass billing service instance to invoice job
                    .build()

            //TO ENSURE ALL PAYMENTS ARE SUCCESSFUL,
            // ATTEMPT PAYING INVOICE AT LEAST THREE TIMES BEFORE FAIL
            // fire on the 1th day of every month at 6:00am,12:00pm,18:00 local timezone
            // val firstCron = cronSchedule("0 0 6,12,18 1 * ?")

            //for testing set to every minute
            val firstCron = cronSchedule("0 * * * * ?")

            val invoiceTrigger = newTrigger()
                    .withIdentity("invoiceTrigger", "group1")
                    .startNow()
                    .withSchedule(firstCron)
                    .build();

            // Schedule the job with the trigger
            scheduler.scheduleJob(invJob, invoiceTrigger);
            scheduler.start()
        } catch (se: SchedulerException) {
            se.printStackTrace()
        }
    }
}