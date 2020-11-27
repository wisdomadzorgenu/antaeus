
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import java.io.File
import java.math.BigDecimal
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.random.Random


// This will create all schemas and setup initial data
internal fun setupInitialData(dal: AntaeusDal) {
    val customers = (1..100).mapNotNull {
        dal.createCustomer(
            currency = Currency.values()[Random.nextInt(0, Currency.values().size)]
        )
    }

    customers.forEach { customer ->
        (1..10).forEach {
            dal.createInvoice(
                amount = Money(
                    value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                    currency = customer.currency
                ),
                customer = customer,
                status = if (it == 1) InvoiceStatus.PENDING else InvoiceStatus.PAID
            )
        }
    }
}

// This is the mocked instance of the payment provider
internal fun getPaymentProvider(): PaymentProvider {
    return object : PaymentProvider {
        override fun charge(invoice: Invoice): Boolean {
                return Random.nextBoolean()
        }
    }
}

//this creates database directory instead of using temp
internal fun createDatabaseDirectory(){
    try {
        //get database app path
        val dirName: String = Paths.get("", "database").toAbsolutePath().toString()

        val directory = File(dirName)

        //only create directory if does not exist
        if (!directory.exists()) {
            directory.mkdir()
        }
    }catch (e:Exception){
        //do nothing
    }
}

//this creates an sqlite file or reads if exists
internal fun getDatabaseFile():File {
    var dbFile: File = File("")

    try {
        val dbFileName: String = "antaeus-db.sqlite"

        //get database app path
        val dbFilePath: String = Paths.get("", "database", dbFileName).toAbsolutePath().toString()

        dbFile = File(dbFilePath)

        //only create directory if does not exist
        if (!dbFile.exists()) {
            dbFile.createNewFile();
            dbFile = File(dbFilePath)
        }
    }
    catch (e:Exception){
        //do nothing
    }

    return dbFile;
}
