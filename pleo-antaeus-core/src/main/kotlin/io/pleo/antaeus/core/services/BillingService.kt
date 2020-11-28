package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import kotlinx.coroutines.*

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
        println("Processing invoice, type=$processType")

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
            val isCharged = paymentProvider.charge(invoice)

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

