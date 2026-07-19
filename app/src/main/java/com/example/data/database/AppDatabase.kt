package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.BatterDao
import com.example.data.dao.CartDao
import com.example.data.dao.OrderDao
import com.example.data.dao.ReviewDao
import com.example.data.model.BatterItem
import com.example.data.model.CartItem
import com.example.data.model.Order
import com.example.data.model.Review

@Database(entities = [BatterItem::class, CartItem::class, Order::class, Review::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun batterDao(): BatterDao
    abstract fun cartDao(): CartDao
    abstract fun orderDao(): OrderDao
    abstract fun reviewDao(): ReviewDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vn_foods_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
