package com.shayaankhalid.marketplace

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "MarketplaceCache.db"
        private const val DATABASE_VERSION = 1

        // Products table
        private const val TABLE_PRODUCTS = "products"
        private const val TABLE_MY_PRODUCTS = "my_products"
        private const val TABLE_PENDING_PRODUCTS = "pending_products"
        private const val TABLE_MESSAGES = "messages"

        private const val COLUMN_ID = "id"
        private const val COLUMN_USER_ID = "user_id"
        private const val COLUMN_TITLE = "name"
        private const val COLUMN_DESCRIPTION = "description"
        private const val COLUMN_PRICE = "price"
        private const val COLUMN_IMAGE = "image"
        private const val COLUMN_CATEGORY = "category"


        private const val SQL_CREATE_PRODUCTS_TABLE = """
            CREATE TABLE $TABLE_PRODUCTS (
                $COLUMN_ID INTEGER PRIMARY KEY,
                $COLUMN_USER_ID INTEGER NOT NULL,
                $COLUMN_TITLE TEXT NOT NULL,
                $COLUMN_DESCRIPTION TEXT,
                $COLUMN_PRICE TEXT NOT NULL,
                $COLUMN_IMAGE LONGTEXT,
                $COLUMN_CATEGORY TEXT
            )
        """

        private const val SQL_CREATE_MY_PRODUCTS_TABLE = """
            CREATE TABLE $TABLE_MY_PRODUCTS (
                $COLUMN_ID INTEGER PRIMARY KEY,
                $COLUMN_USER_ID INTEGER NOT NULL,
                $COLUMN_TITLE TEXT NOT NULL,
                $COLUMN_DESCRIPTION TEXT,
                $COLUMN_PRICE TEXT NOT NULL,
                $COLUMN_IMAGE LONGTEXT,
                $COLUMN_CATEGORY TEXT
            )
        """

        private const val SQL_CREATE_PENDING_TABLE = """
        CREATE TABLE $TABLE_PENDING_PRODUCTS (
        local_id INTEGER PRIMARY KEY AUTOINCREMENT,
        $COLUMN_USER_ID INTEGER NOT NULL,
        $COLUMN_TITLE TEXT NOT NULL,
        $COLUMN_DESCRIPTION TEXT,
        $COLUMN_PRICE TEXT NOT NULL,
        $COLUMN_IMAGE LONGTEXT,
        $COLUMN_CATEGORY TEXT
    )
        """
        // Messages fields
        private const val COLUMN_SENDER_ID = "sender_id"
        private const val COLUMN_SENDER_NAME = "sender_name"
        private const val COLUMN_RECEIVER_ID = "receiver_id"
        private const val COLUMN_MESSAGE = "message"
        private const val COLUMN_TIMESTAMP = "timestamp"

        private const val SQL_CREATE_MESSAGES_TABLE = """
            CREATE TABLE $TABLE_MESSAGES (
                $COLUMN_ID INTEGER PRIMARY KEY,
                $COLUMN_SENDER_ID INTEGER NOT NULL,
                $COLUMN_SENDER_NAME TEXT NOT NULL,
                $COLUMN_RECEIVER_ID INTEGER NOT NULL,
                $COLUMN_MESSAGE TEXT NOT NULL,
                $COLUMN_TIMESTAMP INTEGER NOT NULL
            )
        """

    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_PRODUCTS_TABLE)
        db.execSQL(SQL_CREATE_MY_PRODUCTS_TABLE)
        db.execSQL(SQL_CREATE_PENDING_TABLE)
        db.execSQL(SQL_CREATE_MESSAGES_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PRODUCTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MY_PRODUCTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PENDING_PRODUCTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MESSAGES")
        onCreate(db)
    }

    fun getMessagesBetween(user1: Int, user2: Int): List<ChatMessageModel> {
        val messages = mutableListOf<ChatMessageModel>()
        val db = readableDatabase

        val query = """
        SELECT * FROM $TABLE_MESSAGES
        WHERE (
            ($COLUMN_SENDER_ID = ? AND $COLUMN_RECEIVER_ID = ?)
            OR ($COLUMN_SENDER_ID = ? AND $COLUMN_RECEIVER_ID = ?)
        )
        AND $COLUMN_MESSAGE != ''
        ORDER BY $COLUMN_TIMESTAMP ASC
    """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(
            user1.toString(), user2.toString(),
            user2.toString(), user1.toString()
        ))

        if (cursor.moveToFirst()) {
            do {
                val senderId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SENDER_ID))
                val receiverId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_RECEIVER_ID))
                val message = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE))
                val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                messages.add(ChatMessageModel(senderId, receiverId, message, timestamp))
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return messages
    }


    fun insertMessage(
        id: Int,
        senderId: Int,
        senderName: String,
        receiverId: Int,
        message: String,
        timestamp: Long
    ) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID, id)
            put(COLUMN_SENDER_ID, senderId)
            put(COLUMN_SENDER_NAME, senderName)
            put(COLUMN_RECEIVER_ID, receiverId)
            put(COLUMN_MESSAGE, message)
            put(COLUMN_TIMESTAMP, timestamp)
        }
        db.insertWithOnConflict(TABLE_MESSAGES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getConversations(userId: Int): List<MessagesModel> {
        val db = readableDatabase
        val list = mutableListOf<MessagesModel>()
        val seenUserIds = mutableSetOf<Int>()

        val query = """
            SELECT * FROM $TABLE_MESSAGES
            WHERE $COLUMN_SENDER_ID = ? OR $COLUMN_RECEIVER_ID = ?
            ORDER BY $COLUMN_TIMESTAMP DESC
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(userId.toString(), userId.toString()))

        while (cursor.moveToNext()) {
            val senderId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SENDER_ID))
            val senderName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SENDER_NAME))
            val receiverId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_RECEIVER_ID))

            val otherUserId = if (senderId == userId) receiverId else senderId
            if (seenUserIds.contains(otherUserId)) continue
            seenUserIds.add(otherUserId)

            val name = senderName
            list.add(MessagesModel(id = otherUserId, name = name, pfp = "DEFAULT"))
        }

        cursor.close()
        return list
    }

    fun addPendingProduct(product: Product): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USER_ID, product.u_id)
            put(COLUMN_TITLE, product.title)
            put(COLUMN_DESCRIPTION, product.description)
            put(COLUMN_PRICE, product.price)
            put(COLUMN_IMAGE, product.imageBase64)
            put(COLUMN_CATEGORY, product.category)
        }
        return db.insert(TABLE_PENDING_PRODUCTS, null, values)
    }

    fun getPendingProducts(): List<Product> {
        val products = mutableListOf<Product>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_PENDING_PRODUCTS,
            null, null, null, null, null, null
        )

        cursor.use {
            while (it.moveToNext()) {
                products.add(Product(
                    p_id = it.getInt(it.getColumnIndexOrThrow("local_id")),
                    u_id = it.getInt(it.getColumnIndexOrThrow(COLUMN_USER_ID)),
                    title = it.getString(it.getColumnIndexOrThrow(COLUMN_TITLE)),
                    description = it.getString(it.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                    price = it.getString(it.getColumnIndexOrThrow(COLUMN_PRICE)),
                    imageBase64 = it.getString(it.getColumnIndexOrThrow(COLUMN_IMAGE)),
                    category = it.getString(it.getColumnIndexOrThrow(COLUMN_CATEGORY))
                ))

            }
        }
        return products
    }

    fun deletePendingProduct(localId: Int) {
        val db = writableDatabase
        db.delete(
            TABLE_PENDING_PRODUCTS,
            "local_id = ?",
            arrayOf(localId.toString())
        )
    }

    fun addOrUpdateProduct(product: Product): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID, product.p_id)
            put(COLUMN_USER_ID, product.u_id)
            put(COLUMN_TITLE, product.title)
            put(COLUMN_DESCRIPTION, product.description)
            put(COLUMN_PRICE, product.price)
            put(COLUMN_IMAGE, product.imageBase64)
            put(COLUMN_CATEGORY, product.category)
        }

        val rowsAffected = db.update(
            TABLE_PRODUCTS,
            values,
            "$COLUMN_ID = ?",
            arrayOf(product.p_id.toString())
        )

        return if (rowsAffected == 0) {
            db.insert(TABLE_PRODUCTS, null, values)
        } else {
            product.p_id.toLong()
        }
    }

    fun addOrUpdateProducts(products: List<Product>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            products.forEach { product ->
                addOrUpdateProduct(product)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun addOrUpdateMyProduct(product: Product): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID, product.p_id)
            put(COLUMN_USER_ID, product.u_id)
            put(COLUMN_TITLE, product.title)
            put(COLUMN_DESCRIPTION, product.description)
            put(COLUMN_PRICE, product.price)
            put(COLUMN_IMAGE, product.imageBase64)
            put(COLUMN_CATEGORY, product.category)
        }

        val rowsAffected = db.update(
            TABLE_MY_PRODUCTS,
            values,
            "$COLUMN_ID = ?",
            arrayOf(product.p_id.toString())
        )

        return if (rowsAffected == 0) {
            db.insert(TABLE_MY_PRODUCTS, null, values)
        } else {
            product.p_id.toLong()
        }
    }

    fun addOrUpdateMyProducts(products: List<Product>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            products.forEach { product ->
                addOrUpdateMyProduct(product)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getAllProducts(): List<Product> {
        val products = mutableListOf<Product>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_PRODUCTS,
            null, // all columns
            null, // where clause
            null, // where args
            null, // group by
            null, // having
            null  // order by
        )

        cursor.use {
            while (it.moveToNext()) {
                val product = Product(
                    p_id = it.getInt(it.getColumnIndexOrThrow(COLUMN_ID)),
                    u_id = it.getInt(it.getColumnIndexOrThrow(COLUMN_USER_ID)),
                    title = it.getString(it.getColumnIndexOrThrow(COLUMN_TITLE)),
                    description = it.getString(it.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                    price = it.getString(it.getColumnIndexOrThrow(COLUMN_PRICE)),
                    imageBase64 = it.getString(it.getColumnIndexOrThrow(COLUMN_IMAGE)),
                    category = it.getString(it.getColumnIndexOrThrow(COLUMN_CATEGORY))
                )
                products.add(product)
            }
        }
        return products
    }

    fun getProductsByCategory(category: String): List<Product> {
        val products = mutableListOf<Product>()
        val db = readableDatabase
        val selection = if (category.equals("All", ignoreCase = true)) {
            null
        } else {
            "$COLUMN_CATEGORY = ?"
        }
        val selectionArgs = if (category.equals("All", ignoreCase = true)) {
            null
        } else {
            arrayOf(category)
        }

        val cursor = db.query(
            TABLE_PRODUCTS,
            null,
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        cursor.use {
            while (it.moveToNext()) {
                val product = Product(
                    p_id = it.getInt(it.getColumnIndexOrThrow(COLUMN_ID)),
                    u_id = it.getInt(it.getColumnIndexOrThrow(COLUMN_USER_ID)),
                    title = it.getString(it.getColumnIndexOrThrow(COLUMN_TITLE)),
                    description = it.getString(it.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                    price = it.getString(it.getColumnIndexOrThrow(COLUMN_PRICE)),
                    imageBase64 = it.getString(it.getColumnIndexOrThrow(COLUMN_IMAGE)),
                    category = it.getString(it.getColumnIndexOrThrow(COLUMN_CATEGORY))
                )
                products.add(product)
            }
        }
        return products
    }

    fun clearProducts() {
        val db = writableDatabase
        db.delete(TABLE_PRODUCTS, null, null)
    }

    fun clearMyProducts() {
        val db = writableDatabase
        db.delete(TABLE_MY_PRODUCTS, null, null)
    }
    fun clearChats() {
        val db = writableDatabase
        db.delete(TABLE_MESSAGES, null, null)
    }
    fun getProductById(id: Int): Product? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_PRODUCTS,
            null,
            "$COLUMN_ID = ?",
            arrayOf(id.toString()),
            null,
            null,
            null,
            "1" // limit to 1 result
        )

        cursor.use {
            if (it.moveToFirst()) {
                return Product(
                    p_id = it.getInt(it.getColumnIndexOrThrow(COLUMN_ID)),
                    u_id = it.getInt(it.getColumnIndexOrThrow(COLUMN_USER_ID)),
                    title = it.getString(it.getColumnIndexOrThrow(COLUMN_TITLE)),
                    description = it.getString(it.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                    price = it.getString(it.getColumnIndexOrThrow(COLUMN_PRICE)),
                    imageBase64 = it.getString(it.getColumnIndexOrThrow(COLUMN_IMAGE)),
                    category = it.getString(it.getColumnIndexOrThrow(COLUMN_CATEGORY))
                )
            }
        }
        return null
    }

    fun getOtherProducts(currentUserId: Int): List<Product> {
        val products = mutableListOf<Product>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_PRODUCTS,
            null,
            "$COLUMN_USER_ID != ?",
            arrayOf(currentUserId.toString()),
            null,
            null,
            null
        )

        cursor.use {
            while (it.moveToNext()) {
                val product = Product(
                    p_id = it.getInt(it.getColumnIndexOrThrow(COLUMN_ID)),
                    u_id = it.getInt(it.getColumnIndexOrThrow(COLUMN_USER_ID)),
                    title = it.getString(it.getColumnIndexOrThrow(COLUMN_TITLE)),
                    description = it.getString(it.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                    price = it.getString(it.getColumnIndexOrThrow(COLUMN_PRICE)),
                    imageBase64 = it.getString(it.getColumnIndexOrThrow(COLUMN_IMAGE)),
                    category = it.getString(it.getColumnIndexOrThrow(COLUMN_CATEGORY))
                )
                products.add(product)
            }
        }
        return products
    }

    fun getUserProducts(currentUserId: Int): List<Product> {
        val products = mutableListOf<Product>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_MY_PRODUCTS,
            null,
            "$COLUMN_USER_ID = ?",
            arrayOf(currentUserId.toString()),
            null,
            null,
            null
        )

        cursor.use {
            while (it.moveToNext()) {
                val product = Product(
                    p_id = it.getInt(it.getColumnIndexOrThrow(COLUMN_ID)),
                    u_id = it.getInt(it.getColumnIndexOrThrow(COLUMN_USER_ID)),
                    title = it.getString(it.getColumnIndexOrThrow(COLUMN_TITLE)),
                    description = it.getString(it.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                    price = it.getString(it.getColumnIndexOrThrow(COLUMN_PRICE)),
                    imageBase64 = it.getString(it.getColumnIndexOrThrow(COLUMN_IMAGE)),
                    category = it.getString(it.getColumnIndexOrThrow(COLUMN_CATEGORY))
                )
                products.add(product)
            }
        }
        return products
    }

    fun deleteProduct(productId: Int): Int {
        val db = writableDatabase
        return db.delete(
            TABLE_PRODUCTS,
            "$COLUMN_ID = ?",
            arrayOf(productId.toString())
        )
    }
}