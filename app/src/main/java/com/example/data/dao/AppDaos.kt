package com.example.data.dao

import androidx.room.*
import com.example.data.model.BatterItem
import com.example.data.model.CartItem
import com.example.data.model.Order
import kotlinx.coroutines.flow.Flow

@Dao
interface BatterDao {
    @Query("SELECT * FROM batter_items")
    fun getAllBatterItems(): Flow<List<BatterItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatterItems(items: List<BatterItem>)

    @Query("DELETE FROM batter_items")
    suspend fun clearBatterItems()
}

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items")
    fun getCartItems(): Flow<List<CartItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(item: CartItem)

    @Query("DELETE FROM cart_items WHERE itemId = :id")
    suspend fun deleteCartItemById(id: String)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    fun getAllOrders(): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE orderId = :orderId LIMIT 1")
    suspend fun getOrderById(orderId: String): Order?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order)

    @Update
    suspend fun updateOrder(order: Order)
}
