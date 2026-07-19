package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "batter_items")
data class BatterItem(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val category: String, // "Traditional Batter", "Specialty Batter", "Accompaniments"
    val rating: Double,
    val prepTime: String,
    val isBestSeller: Boolean,
    val size: String
)

@Entity(tableName = "cart_items")
data class CartItem(
    @PrimaryKey val itemId: String,
    val quantity: Int
)

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey val orderId: String,
    val timestamp: Long,
    val itemsSummary: String,
    val totalAmount: Double,
    val status: String, // "PENDING", "PREPARING", "DISPATCHED", "OUT_FOR_DELIVERY", "DELIVERED"
    val progress: Float, // 0.0 to 1.0 for real-time slider
    val etaMinutes: Int,
    val deliveryPartnerName: String,
    val deliveryPartnerPhone: String,
    val paymentMethod: String,
    val paymentCardLast4: String,
    val rating: Int = 0
)

@Entity(tableName = "reviews")
data class Review(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val itemId: String, // e.g. "b1", "b2", "b3" or "dosa_idly_batter" to share among sizes
    val customerName: String,
    val rating: Int, // 1 to 5
    val feedback: String,
    val timestamp: Long = System.currentTimeMillis()
)

