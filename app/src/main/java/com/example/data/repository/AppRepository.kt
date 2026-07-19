package com.example.data.repository

import com.example.data.dao.BatterDao
import com.example.data.dao.CartDao
import com.example.data.dao.OrderDao
import com.example.data.dao.ReviewDao
import com.example.data.model.BatterItem
import com.example.data.model.CartItem
import com.example.data.model.Order
import com.example.data.model.Review
import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val batterDao: BatterDao,
    private val cartDao: CartDao,
    private val orderDao: OrderDao,
    private val reviewDao: ReviewDao
) {
    val allBatterItems: Flow<List<BatterItem>> = batterDao.getAllBatterItems()
    val cartItems: Flow<List<CartItem>> = cartDao.getCartItems()
    val allOrders: Flow<List<Order>> = orderDao.getAllOrders()
    val allReviews: Flow<List<Review>> = reviewDao.getAllReviews()

    fun getReviewsForItem(itemId: String): Flow<List<Review>> {
        return reviewDao.getReviewsForItem(itemId)
    }

    suspend fun insertReview(review: Review) {
        reviewDao.insertReview(review)
    }

    suspend fun clearReviews() {
        reviewDao.clearReviews()
    }

    suspend fun populateBatterItems(items: List<BatterItem>) {
        batterDao.insertBatterItems(items)
    }

    suspend fun clearBatterItems() {
        batterDao.clearBatterItems()
    }

    suspend fun addCartItem(itemId: String, quantity: Int) {
        cartDao.insertCartItem(CartItem(itemId, quantity))
    }

    suspend fun deleteCartItemById(itemId: String) {
        cartDao.deleteCartItemById(itemId)
    }

    suspend fun clearCart() {
        cartDao.clearCart()
    }

    suspend fun insertOrder(order: Order) {
        orderDao.insertOrder(order)
    }

    suspend fun updateOrder(order: Order) {
        orderDao.updateOrder(order)
    }
}
