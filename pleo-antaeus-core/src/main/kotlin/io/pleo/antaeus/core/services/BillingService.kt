package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import org.quartz.*
import org.quartz.CronScheduleBuilder.cronSchedule
import org.quartz.JobBuilder.newJob
import org.quartz.TriggerBuilder.newTrigger
import org.quartz.impl.StdSchedulerFactory
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.*
import java.nio.file.Paths
import kotlinx.coroutines.*
import org.quartz.Job

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService
) {
    /**
     * Process invoices either by attempting payment or
     * sending failed email notifications
     */
    fun processInvoices(processType:String="process") {
        //Algorithm
        // Count Unpaid Invoices
        // Retrieve Invoices in batches `50/iteration` asynchronously
        // Process Payments for retrieved invoices asynchronously
        // Retrieve next bath of invoices when done
        var limit = 50
        var offset = 0
        var lastInvoiceId:Int = 0

        // Count Unpaid Invoices
        val unpaidInvoiceCount = invoiceService.countUnpaid()

        while(offset < unpaidInvoiceCount){
            //retrieve invoices in batches
            var invoicesList:List<Invoice> = invoiceService.fetchUnpaidInBatches(lastInvoiceId,limit)

            //update last invoiceId
            //it will be used for next query
            if(invoicesList.isNotEmpty()){
                lastInvoiceId = invoicesList.last().id
            }

            runBlocking{
                //run fetch and process call concurrently
                coroutineScope{
                    // loop invoices and process
                    invoicesList.forEach {
                        if(processType == "process"){
                            //process payment
                            launch{processSingleInvoice(it,0)}
                        }
                        else if(processType == "email"){
                            launch{ sendCustomerFailedPaymentNotification(it)}
                        }
                    }
                }
            }

            //all done, increase offset
            offset += limit
        }
    }

    //Process a single customer invoice, attempt three tree times
    private suspend fun processSingleInvoice(invoice:Invoice,attempts:Int=0): Boolean {
        var processingFailed = false;

        try {
            //ALGORITHM
            // Update invoices if payment is successful
            // Send email notification to customers of successful payment
            //  Retry processing payment if failed or an exception occurred
            //println("Invoice ID: $invoice.id attempts: $attempts")

            //get invoice & customer information
            val (id, customerId) = invoice

            //mock charge invoice
            val isCharged = paymentProvider.charge(invoice);

            //update invoice for customer in database
            if(isCharged){
                //update paid status for invoice
                var successfulResult:Any = invoiceService.updatePaidStatus(id)

                if(successfulResult != null){
                    //send email
                    sendCustomerPaymentNotification(customerId);
                }
            }
            else {
                processingFailed = true;
            }
        } catch (err: NetworkException) {
            processingFailed = true;
        } catch (err: Exception) {
            //nothing happens for now.
            // job will re-run again in a few hours
        }

        //attempt charging again until three times
        if(processingFailed && attempts < 3){
            //println("trying processing again")
            return processSingleInvoice(invoice=invoice,attempts=attempts+1)
        }

        return true
    }

    //mock: send customer payment notification
    private suspend fun sendCustomerPaymentNotification(customerId:Int):Boolean{
        //send email
        return true
    }

    //mock: send failed email notification to customer
    private suspend fun sendCustomerFailedPaymentNotification(invoice:Invoice):Boolean{
        return true
    }
}

/**
 * Invoice Job Class extends quartz job
 * Implements the retrieval and processing of invoices
 */
class InvoiceJob : Job {
    @Throws(JobExecutionException::class)
    override fun execute(context: JobExecutionContext) {
        val dataMap = context.jobDetail.jobDataMap
        val billingService:BillingService = dataMap["billingService"] as BillingService

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

        //process invoices by sending emails
        billingService.processInvoices("email");
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
            // val processInvoiceCron = cronSchedule("0 0 5,10,15 1 * ?")
            // val notifyCustomerCron = cronSchedule("0 0 20 1 * ?")

            //for testing set to every minute for payment processing
            val processInvoiceCron = cronSchedule("0 * * * * ?")
            val notifyCustomerCron = cronSchedule("0 */5 * * * ?")

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