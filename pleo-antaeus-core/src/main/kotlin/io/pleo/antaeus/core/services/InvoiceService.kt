/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus

class InvoiceService(private val dal: AntaeusDal) {
    fun countUnpaid():Int {
        return dal.countUnpaidInvoices()
    }

    fun fetchUnpaidInBatches(lastInvoiceId:Int,limit:Int=1): List<Invoice>{
        return dal.fetchUnpaidInvoiceInBatches(lastInvoiceId,limit)
    }

    fun updatePaidStatus(invoiceId:Int):Any{
        return dal.updateInvoicePaidStatus(invoiceId)
    }

    fun fetchAll(): List<Invoice> {
        return dal.fetchInvoices()
    }

    fun fetch(id: Int): Invoice {
        return dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }
}
