package domain

import protocol.Product
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class Storage {

    fun supply(id: Int, amount: Int): Int? {
        synchronized(this) {
            val good = goods[id]
            good?.let { rec ->
                val newAmount = rec.amount + amount
                goods[id] = rec.copy(amount = newAmount)
                return newAmount
            }
            return null
        }
    }

    fun buy(id: Int, amount: Int): Int? {
        synchronized(this) {
            val good = goods[id]
            good?.let { rec ->
                val newAmount = rec.amount - amount
                if (newAmount >= 0) {
                    goods[id] = rec.copy(amount = newAmount)
                    return newAmount
                }
            }
            return null
        }
    }

    fun addProduct(name: String, price: Int): Int {
        val id = getNextId()
        goods[id] = Record(Product(getNextId(), name, price), 0)
        return id
    }

    fun get(id: Int) = goods[id]

    fun getGoods() = goods.values.map { it.product }

    private val goods = ConcurrentHashMap<Int, Record>()

    private var nextId = AtomicInteger(0)

    private fun getNextId() = nextId.incrementAndGet()

    data class Record(
        val product: Product,
        val amount: Int
    )

}