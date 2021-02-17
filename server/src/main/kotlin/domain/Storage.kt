package domain

import protocol.Product
import java.util.concurrent.ConcurrentHashMap

class Storage {

    fun supply(id: Int, amount: Int): Int? {
        val good = goods[id]
        good?.let { rec ->
            val newamount = rec.amount + amount
            goods[id] = rec.copy(amount = newamount)
            return newamount
        }
        return null
    }

    fun buy(id: Int, amount: Int): Int? {
        val good = goods[id]
        good?.let { rec ->
            val newamount = rec.amount - amount
            if (newamount >= 0) {
                goods[id] = rec.copy(amount = newamount)
                return newamount
            }
        }
        return null
    }

    fun addProduct(name: String, price: Int): Int {
        val id = getNextId()
        goods[id] = Record(Product(getNextId(), name, price), 0)
        return id
    }

    fun get(id: Int) = goods[id]

    fun getGoods() = goods.values.map { it.product }

    private val goods = ConcurrentHashMap<Int, Record>()

    private var nextId: Int = 0

    private fun getNextId() = nextId++

    data class Record(
        val product: Product,
        val amount: Int
    )

}