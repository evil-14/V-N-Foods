package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.BatterItem
import com.example.data.model.CartItem
import com.example.data.model.Order
import com.example.data.model.Review
import com.example.data.repository.AppRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

sealed class Screen {
    object Shop : Screen()
    object Cart : Screen()
    object Orders : Screen()
    object Profile : Screen()
}

data class CartDisplayItem(
    val item: BatterItem,
    val quantity: Int
)

data class CartSummary(
    val subtotal: Double,
    val tax: Double,
    val deliveryFee: Double,
    val total: Double
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository

    val allBatterItems: StateFlow<List<BatterItem>>
    val cartItems: StateFlow<List<CartItem>>
    val allOrders: StateFlow<List<Order>>
    val allReviews: StateFlow<List<Review>>

    // Selected screen/tab
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Shop)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Active order being tracked currently
    private val _activeTrackOrderId = MutableStateFlow<String?>(null)
    val activeTrackOrderId: StateFlow<String?> = _activeTrackOrderId.asStateFlow()

    // Secure Credit Card state
    val cardNumber = MutableStateFlow("")
    val cardExpiry = MutableStateFlow("")
    val cardCvv = MutableStateFlow("")
    val cardHolder = MutableStateFlow("")

    // Checkout UI overlays
    private val _showApplePaySheet = MutableStateFlow(false)
    val showApplePaySheet: StateFlow<Boolean> = _showApplePaySheet.asStateFlow()

    private val _isCartDrawerOpen = MutableStateFlow(false)
    val isCartDrawerOpen: StateFlow<Boolean> = _isCartDrawerOpen.asStateFlow()

    private val _isProcessingPayment = MutableStateFlow(false)
    val isProcessingPayment: StateFlow<Boolean> = _isProcessingPayment.asStateFlow()

    private val _paymentSuccess = MutableStateFlow(false)
    val paymentSuccess: StateFlow<Boolean> = _paymentSuccess.asStateFlow()

    // Google Translate / Multi-language support state
    private val _currentLanguage = MutableStateFlow("en")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    fun setLanguage(langCode: String) {
        _currentLanguage.value = langCode
    }

    fun translate(text: String): String {
        return com.example.ui.translation.AppTranslation.translateDynamic(text, _currentLanguage.value)
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AppRepository(
            database.batterDao(),
            database.cartDao(),
            database.orderDao(),
            database.reviewDao()
        )

        allBatterItems = repository.allBatterItems.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        cartItems = repository.cartItems.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allOrders = repository.allOrders.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allReviews = repository.allReviews.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Seed data if empty or outdated
        viewModelScope.launch {
            allBatterItems.first { true } // wait for first emission
            delay(300)
            val hasDasaIdly = allBatterItems.value.any { it.price == 26.0 }
            if (allBatterItems.value.isEmpty() || !hasDasaIdly) {
                seedDefaultBatterItems()
            }
        }

        // Seed reviews if empty
        viewModelScope.launch {
            allReviews.first { true } // wait for first emission
            delay(300)
            if (allReviews.value.isEmpty()) {
                seedDefaultReviews()
            }
        }
    }

    fun submitReview(customerName: String, rating: Int, feedback: String) {
        viewModelScope.launch {
            repository.insertReview(
                Review(
                    itemId = "dosa_idly_batter",
                    customerName = customerName,
                    rating = rating,
                    feedback = feedback,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    private suspend fun seedDefaultReviews() {
        repository.clearReviews()
        val defaultReviews = listOf(
            Review(
                itemId = "dosa_idly_batter",
                customerName = "Ramesh Kumar",
                rating = 5,
                feedback = "Excellent quality batter. Naturally fermented and perfectly ground. The idlis were as soft as clouds!",
                timestamp = System.currentTimeMillis() - 86400000 * 2 // 2 days ago
            ),
            Review(
                itemId = "dosa_idly_batter",
                customerName = "Anjali Sharma",
                rating = 5,
                feedback = "Dosas turned out extremely golden and crispy. Best batter in town, strictly premium and chemical free.",
                timestamp = System.currentTimeMillis() - 86400000 * 5 // 5 days ago
            ),
            Review(
                itemId = "dosa_idly_batter",
                customerName = "Karthik Subbaraj",
                rating = 4,
                feedback = "Slightly sour but perfect for crispy dosas! Packaging is neat and spill-proof. Fast delivery too.",
                timestamp = System.currentTimeMillis() - 86400000 * 8 // 8 days ago
            ),
            Review(
                itemId = "dosa_idly_batter",
                customerName = "Priya R.",
                rating = 5,
                feedback = "Tastes exactly like home-ground batter in a stone mortar. Very soft and aromatic. Highly recommended!",
                timestamp = System.currentTimeMillis() - 86400000 * 12 // 12 days ago
            )
        )
        for (review in defaultReviews) {
            repository.insertReview(review)
        }
    }

    // Helper to join items with quantities
    val cartDisplayItems: StateFlow<List<CartDisplayItem>> = combine(
        allBatterItems,
        cartItems
    ) { items, cart ->
        cart.mapNotNull { cartItem ->
            val matchingItem = items.find { it.id == cartItem.itemId }
            if (matchingItem != null) {
                CartDisplayItem(matchingItem, cartItem.quantity)
            } else {
                null
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Derived prices/calculations
    val cartSummary: StateFlow<CartSummary> = cartDisplayItems.map { displayList ->
        val subtotal = displayList.sumOf { it.item.price * it.quantity }
        val tax = subtotal * 0.05 // 5% GST/tax
        val deliveryFee = if (subtotal > 15.00 || subtotal == 0.0) 0.00 else 2.99
        val total = if (subtotal == 0.0) 0.0 else subtotal + tax + deliveryFee
        CartSummary(subtotal, tax, deliveryFee, total)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CartSummary(0.0, 0.0, 0.0, 0.0)
    )

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun setTrackingOrderId(orderId: String?) {
        _activeTrackOrderId.value = orderId
        if (orderId != null) {
            _currentScreen.value = Screen.Orders
        }
    }

    fun addToCart(itemId: String, change: Int = 1) {
        viewModelScope.launch {
            val existing = cartItems.value.find { it.itemId == itemId }
            val newQty = (existing?.quantity ?: 0) + change
            if (newQty <= 0) {
                repository.deleteCartItemById(itemId)
            } else {
                repository.addCartItem(itemId, newQty)
            }
        }
    }

    fun removeFromCart(itemId: String) {
        viewModelScope.launch {
            repository.deleteCartItemById(itemId)
        }
    }

    fun updateCartItemWeight(oldId: String, newId: String) {
        viewModelScope.launch {
            if (oldId == newId) return@launch
            val existingOld = cartItems.value.find { it.itemId == oldId } ?: return@launch
            val existingNew = cartItems.value.find { it.itemId == newId }
            
            repository.deleteCartItemById(oldId)
            val finalQty = (existingNew?.quantity ?: 0) + existingOld.quantity
            repository.addCartItem(newId, finalQty)
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            repository.clearCart()
        }
    }

    fun setApplePaySheetVisible(visible: Boolean) {
        _showApplePaySheet.value = visible
    }

    fun setCartDrawerOpen(open: Boolean) {
        _isCartDrawerOpen.value = open
    }

    // Process secure payment and place order
    fun checkout(paymentMethod: String, cardLast4: String = "4032") {
        viewModelScope.launch {
            _isProcessingPayment.value = true
            delay(1500) // Simulate secure processor handshake

            val summary = cartSummary.value
            val cartList = cartDisplayItems.value
            if (cartList.isEmpty()) {
                _isProcessingPayment.value = false
                return@launch
            }

            val summaryText = cartList.joinToString(", ") { "${it.item.name} x${it.quantity}" }
            val orderId = "VN-" + UUID.randomUUID().toString().substring(0, 8).uppercase()

            val newOrder = Order(
                orderId = orderId,
                timestamp = System.currentTimeMillis(),
                itemsSummary = summaryText,
                totalAmount = summary.total,
                status = "PENDING",
                progress = 0.05f,
                etaMinutes = 25,
                deliveryPartnerName = "Karthik R.",
                deliveryPartnerPhone = "+91 98452 10243",
                paymentMethod = paymentMethod,
                paymentCardLast4 = cardLast4
            )

            // Save to database
            repository.insertOrder(newOrder)

            // Clear Cart
            repository.clearCart()

            // Reset secure payment form
            cardNumber.value = ""
            cardExpiry.value = ""
            cardCvv.value = ""
            cardHolder.value = ""

            _isProcessingPayment.value = false
            _showApplePaySheet.value = false
            _paymentSuccess.value = true

            // Set as active tracking order
            _activeTrackOrderId.value = orderId
            _currentScreen.value = Screen.Orders

            // Launch tracking simulation in background
            simulateRealTimeTracking(orderId)
        }
    }

    fun resetPaymentSuccess() {
        _paymentSuccess.value = false
    }

    // Simulation of actual status transitions in database
    private fun simulateRealTimeTracking(orderId: String) {
        viewModelScope.launch {
            // Step 1: Preparing
            delay(6000)
            updateOrderInDatabase(orderId, "PREPARING", 0.25f, 20)

            // Step 2: Dispatched
            delay(8000)
            updateOrderInDatabase(orderId, "DISPATCHED", 0.55f, 12)

            // Step 3: Out for Delivery
            delay(8000)
            updateOrderInDatabase(orderId, "OUT_FOR_DELIVERY", 0.85f, 4)

            // Step 4: Delivered
            delay(8000)
            updateOrderInDatabase(orderId, "DELIVERED", 1.0f, 0)
        }
    }

    private suspend fun updateOrderInDatabase(orderId: String, status: String, progress: Float, eta: Int) {
        val currentOrders = allOrders.value
        val existing = currentOrders.find { it.orderId == orderId }
        if (existing != null) {
            val updated = existing.copy(
                status = status,
                progress = progress,
                etaMinutes = eta
            )
            repository.insertOrder(updated)
        }
    }

    private suspend fun seedDefaultBatterItems() {
        repository.clearBatterItems()
        val seedList = listOf(
            BatterItem(
                id = "b1",
                name = "Dasa Idly Batter (0.5 kg)",
                description = "Perfect blend of stone-ground lentils & short-grain rice, naturally fermented. Ideal for making soft idlis & crispy dosas.",
                price = 26.0,
                category = "Traditional Batter",
                rating = 4.9,
                prepTime = "Ready to pour",
                isBestSeller = false,
                size = "0.5 kg"
            ),
            BatterItem(
                id = "b2",
                name = "Dasa Idly Batter (1 kg)",
                description = "Perfect blend of stone-ground lentils & short-grain rice, naturally fermented. Ideal for making soft idlis & crispy dosas.",
                price = 50.0,
                category = "Traditional Batter",
                rating = 4.9,
                prepTime = "Ready to pour",
                isBestSeller = true,
                size = "1 kg"
            ),
            BatterItem(
                id = "b3",
                name = "Dasa Idly Batter (2 kg)",
                description = "Perfect blend of stone-ground lentils & short-grain rice, naturally fermented. Ideal for making soft idlis & crispy dosas.",
                price = 100.0,
                category = "Traditional Batter",
                rating = 4.9,
                prepTime = "Ready to pour",
                isBestSeller = false,
                size = "2 kg"
            )
        )
        repository.populateBatterItems(seedList)
    }
}
