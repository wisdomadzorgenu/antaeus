package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class InvoiceServiceTest {
    private val dal = mockk<AntaeusDal> {
        every { fetchInvoice(404) } returns null
    }

    private val invoiceService = InvoiceService(dal = dal)

    @Test
    fun `will throw if invoice is not found`() {
        assertThrows<InvoiceNotFoundException> {
            invoiceService.fetch(404)
        }
    }

    @Test
    fun `will successfully count unpaid invoices`(){
        //implement when done
    }

    @Test
    fun `will successfully retrieve unpaid invoices in batches`(){
        //implement when done
    }

    @Test
    fun `will throw if no invoice Id is provided for invoice status update`(){
        //implement when done
    }

    @Test
    fun `will update invoice status to paid`(){

    }


}
