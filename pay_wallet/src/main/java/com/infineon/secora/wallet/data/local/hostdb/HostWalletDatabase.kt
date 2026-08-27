// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

package com.infineon.secora.wallet.data.local.hostdb

import android.content.Context
import android.database.sqlite.SQLiteBlobTooBigException
import android.util.Log
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import com.infineon.secora.wallet.client.data.models.card.common.CardDetails
import com.infineon.secora.wallet.client.data.models.common.CardList
import com.infineon.secora.wallet.client.data.models.common.GetProvisionCardResponse
import com.infineon.secora.wallet.data.local.StorageRepository
import com.infineon.secora.wallet.data.local.preference.PreferenceKey
import java.io.File

@Entity(
    tableName = "host_wallet_cards",
    indices = [Index(value = ["paymentAppInstanceId"])]
)
data class HostWalletCardEntity(
    @PrimaryKey val digitizationRef: String,
    val paymentAppInstanceId: String?,
    val payloadJson: String
)

@Entity(
    tableName = "host_wallet_card_images",
    indices = [Index(value = ["paymentAppInstanceId"]), Index(value = ["assetId"])]
)
data class HostWalletCardImageEntity(
    @PrimaryKey val digitizationRef: String,
    val paymentAppInstanceId: String?,
    val assetId: String?,
    val cardImage: String,
    val cardImageHeight: String?,
    val cardImageWidth: String?
)

@Dao
interface HostWalletCardDao {
    /**
     * Lists all cached card rows for one payment app instance.
     */
    @Query("SELECT * FROM host_wallet_cards WHERE paymentAppInstanceId = :pid")
    fun listByPaymentApp(pid: String): List<HostWalletCardEntity>

    /**
     * Returns one cached card row by digitization reference.
     */
    @Query("SELECT * FROM host_wallet_cards WHERE digitizationRef = :ref LIMIT 1")
    fun getByDigitizationRef(ref: String): HostWalletCardEntity?

    /**
     * Lists all cached card rows.
     */
    @Query("SELECT * FROM host_wallet_cards")
    fun listAll(): List<HostWalletCardEntity>

    /**
     * Deletes all cached card rows for one payment app instance.
     */
    @Query("DELETE FROM host_wallet_cards WHERE paymentAppInstanceId = :pid")
    fun deleteByPaymentApp(pid: String)

    /**
     * Deletes one cached card row by digitization reference.
     */
    @Query("DELETE FROM host_wallet_cards WHERE digitizationRef = :ref")
    fun deleteByDigitizationRef(ref: String)

    /**
     * Deletes all cached card rows.
     */
    @Query("DELETE FROM host_wallet_cards")
    fun deleteAll()

    /**
     * Inserts or replaces cached card rows.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAll(entities: List<HostWalletCardEntity>)
}

@Dao
interface HostWalletCardImageDao {
    @Query("SELECT * FROM host_wallet_card_images WHERE digitizationRef = :ref LIMIT 1")
    fun getByDigitizationRef(ref: String): HostWalletCardImageEntity?

    @Query("SELECT * FROM host_wallet_card_images WHERE assetId = :assetId LIMIT 1")
    fun getByAssetId(assetId: String): HostWalletCardImageEntity?

    @Query("DELETE FROM host_wallet_card_images WHERE digitizationRef = :ref")
    fun deleteByDigitizationRef(ref: String)

    @Query("DELETE FROM host_wallet_card_images WHERE paymentAppInstanceId = :pid")
    fun deleteByPaymentApp(pid: String)

    @Query("SELECT * FROM host_wallet_card_images WHERE paymentAppInstanceId = :pid")
    fun listByPaymentApp(pid: String): List<HostWalletCardImageEntity>

    @Query("SELECT * FROM host_wallet_card_images")
    fun listAll(): List<HostWalletCardImageEntity>

    @Query("DELETE FROM host_wallet_card_images")
    fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: HostWalletCardImageEntity)
}

@Entity(tableName = "host_wallet_user_nicknames")
data class HostWalletUserNicknameEntity(
    @PrimaryKey val digitizationRef: String,
    val nickname: String
)

@Dao
interface HostWalletUserNicknameDao {
    /**
     * Returns nickname mapping by digitization reference.
     */
    @Query("SELECT * FROM host_wallet_user_nicknames WHERE digitizationRef = :ref LIMIT 1")
    fun getByRef(ref: String): HostWalletUserNicknameEntity?

    /**
     * Inserts or replaces one nickname mapping.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: HostWalletUserNicknameEntity)

    /**
     * Deletes nickname mapping by digitization reference.
     */
    @Query("DELETE FROM host_wallet_user_nicknames WHERE digitizationRef = :ref")
    fun deleteByRef(ref: String)

    /**
     * Deletes all nickname mappings.
     */
    @Query("DELETE FROM host_wallet_user_nicknames")
    fun deleteAll()
}

@Database(
    entities = [
        HostWalletCardEntity::class,
        HostWalletCardImageEntity::class,
        HostWalletUserNicknameEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppWalletDatabase : RoomDatabase() {
    /**
     * Returns DAO for host card rows.
     */
    abstract fun hostWalletCardDao(): HostWalletCardDao

    /**
     * Returns DAO for host card image rows.
     */
    abstract fun hostWalletCardImageDao(): HostWalletCardImageDao

    /**
     * Returns DAO for nickname rows keyed by digitization reference.
     */
    abstract fun hostWalletUserNicknameDao(): HostWalletUserNicknameDao

    companion object {
        @Volatile
        private var instance: AppWalletDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS host_wallet_user_nicknames (" +
                        "digitizationRef TEXT NOT NULL PRIMARY KEY, " +
                        "nickname TEXT NOT NULL)"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS host_wallet_user_nicknames_dpan (" +
                        "paymentAppInstanceId TEXT NOT NULL, " +
                        "dpanSuffix TEXT NOT NULL, " +
                        "nickname TEXT NOT NULL, " +
                        "PRIMARY KEY(paymentAppInstanceId, dpanSuffix))"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS host_wallet_user_nicknames_dpan")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS host_wallet_card_images (" +
                        "digitizationRef TEXT NOT NULL PRIMARY KEY, " +
                        "paymentAppInstanceId TEXT, " +
                        "assetId TEXT, " +
                        "cardImage TEXT NOT NULL, " +
                        "cardImageHeight TEXT, " +
                        "cardImageWidth TEXT)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_host_wallet_card_images_paymentAppInstanceId " +
                        "ON host_wallet_card_images(paymentAppInstanceId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_host_wallet_card_images_assetId " +
                        "ON host_wallet_card_images(assetId)"
                )
                // Legacy rows may contain very large base64 in payloadJson and crash CursorWindow reads.
                db.execSQL("DELETE FROM host_wallet_cards")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Drop inline base64 image cache rows; v6 stores images on filesystem.
                db.execSQL("DELETE FROM host_wallet_card_images")
            }
        }

        /**
         * Returns the singleton app-owned wallet Room database instance.
         *
         * @param context The application or activity context.
         * @return Singleton [AppWalletDatabase] instance.
         */
        fun get(context: Context): AppWalletDatabase {
            synchronized(this) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppWalletDatabase::class.java,
                        "secora_host_wallet.db"
                    )
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                        .fallbackToDestructiveMigration()
                        .allowMainThreadQueries()
                        .build()
                }
            }
            return instance!!
        }
    }
}

/**
 * App-owned card rows for UI and navigation. Populated from provision API responses and
 * updated from user actions (status, nickname, images). The SDK keeps its own Room DB internally.
 */
object HostCardCache {
    private val gson = Gson()
    private const val TAG = "HostCardCache"
    private const val LARGE_IMAGE_WARN_THRESHOLD_BYTES = 512 * 1024
    private const val IMAGE_CACHE_DIR = "host_card_images"

    /**
     * Returns DAO for host card rows.
     */
    private fun dao(context: Context) = AppWalletDatabase.get(context).hostWalletCardDao()

    /**
     * Returns DAO for host card image rows.
     */
    private fun imageDao(context: Context) = AppWalletDatabase.get(context).hostWalletCardImageDao()

    /**
     * Returns DAO for user nicknames keyed by digitization reference.
     */
    private fun userNicknameDao(context: Context) = AppWalletDatabase.get(context).hostWalletUserNicknameDao()

    /**
     * Normalizes DPAN suffix values used as lookup keys.
     */
    private fun normalizeDpan(value: String?): String = value?.trim().orEmpty()

    private fun imageCacheDir(context: Context): File {
        val dir = File(context.cacheDir, IMAGE_CACHE_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun imageFileForRef(context: Context, ref: String): File {
        val safeRef = ref.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(imageCacheDir(context), "$safeRef.b64")
    }

    private fun persistImageToFile(context: Context, ref: String, image: String): String? {
        return try {
            val file = imageFileForRef(context, ref)
            file.writeText(image)
            file.absolutePath
        } catch (e: Exception) {
            Log.w(TAG, "Failed writing image file for ref=$ref", e)
            null
        }
    }

    private fun readImageFromPath(path: String?): String? {
        if (path.isNullOrBlank()) return null
        return try {
            File(path).takeIf { it.exists() }?.readText()
        } catch (e: Exception) {
            Log.w(TAG, "Failed reading image file at path=$path", e)
            null
        }
    }

    private fun deleteImageFile(path: String?) {
        if (path.isNullOrBlank()) return
        try {
            val fileDeleted = File(path).takeIf { it.exists() }?.delete()
            if (fileDeleted == true) {
                Log.w(TAG, "File Deleted")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed deleting image file at path=$path", e)
        }
    }

    private fun deleteImageFileByRef(context: Context, ref: String) {
        try {
            val fileDeleted = imageFileForRef(context, ref).takeIf { it.exists() }?.delete()
            if (fileDeleted == true) {
                Log.w(TAG, "File Deleted")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed deleting image file for ref=$ref", e)
        }
    }

    private fun hydrateImagePayload(entity: HostWalletCardImageEntity): String? {
        val value = entity.cardImage
        // v6 stores file paths; older values may contain inline base64.
        if (value.startsWith("/")) {
            return readImageFromPath(value)
        }
        return value
    }

    private fun upsertCardImage(
        context: Context,
        ref: String,
        paymentAppInstanceId: String?,
        assetId: String?,
        image: String,
        height: String?,
        width: String?
    ) {
        if (ref.isBlank() || image.isBlank()) return
        val imageSize = image.toByteArray(Charsets.UTF_8).size
        if (imageSize > LARGE_IMAGE_WARN_THRESHOLD_BYTES) {
            Log.w(
                TAG,
                "Large card image payload for ref=$ref assetId=$assetId sizeBytes=$imageSize"
            )
        }
        val imagePath = persistImageToFile(context, ref, image) ?: return
        try {
            imageDao(context).getByDigitizationRef(ref)?.let { existing ->
                if (existing.cardImage != imagePath) {
                    deleteImageFile(existing.cardImage)
                }
            }
        } catch (_: SQLiteBlobTooBigException) {
            imageDao(context).deleteByDigitizationRef(ref)
        }
        imageDao(context).upsert(
            HostWalletCardImageEntity(
                digitizationRef = ref,
                paymentAppInstanceId = paymentAppInstanceId,
                assetId = assetId,
                cardImage = imagePath,
                cardImageHeight = height,
                cardImageWidth = width
            )
        )
    }

    /**
     * User-entered label keyed by digitization reference. Survives provision refresh and exists before the card row
     * is inserted (add-card flow). Removed when the card row is deleted or that payment app is cleared.
     */
    fun setUserNicknameForDigitizationRef(context: Context, digitizationReferenceNumber: String, nickname: String) {
        val ref = digitizationReferenceNumber.trim()
        if (ref.isEmpty()) return
        val nick = nickname.trim()
        userNicknameDao(context).upsert(HostWalletUserNicknameEntity(ref, nick))
    }

    /**
     * Deletes stored nickname mapped by digitization reference.
     *
     * @param context Context used to access the host database.
     * @param ref Digitization reference key.
     */
    private fun deleteUserNicknameForDigitizationRef(context: Context, ref: String) {
        if (ref.isBlank()) return
        userNicknameDao(context).deleteByRef(ref)
    }

    /**
     * Deletes all stored nickname mappings for one payment app instance.
     *
     * @param context Context used to access the host database.
     * @param paymentAppId Payment app instance ID.
     */
    private fun deleteUserNicknamesForPaymentApp(context: Context, paymentAppId: String) {
        val refs = dao(context).listByPaymentApp(paymentAppId).map { it.digitizationRef }
        val nickDao = userNicknameDao(context)
        refs.forEach { nickDao.deleteByRef(it) }
    }

    /**
     * Applies persisted user nickname overrides to a card model.
     *
     * @param context Context used to read nickname tables.
     * @param card Card to enrich with user-entered nickname.
     * @return Updated card when a stored nickname exists; otherwise the original card.
     */
    private fun applyStoredUserNickname(context: Context, card: CardDetails): CardDetails {
        val ref = card.digitizationReferenceNumber?.trim().orEmpty()
        if (ref.isNotEmpty()) {
            val byRef = userNicknameDao(context).getByRef(ref)?.nickname?.trim().orEmpty()
            if (byRef.isNotEmpty()) return card.copy(cardNickname = byRef)
        }
        return card.copy(cardNickname = "")
    }

    /**
     * Replaces all cached cards for one payment app and preserves retained fields.
     *
     * @param context Context used to access host DB.
     * @param paymentAppId Payment app instance ID owning the cards.
     * @param cards Fresh card list to persist.
     */
    fun replaceForPaymentApp(context: Context, paymentAppId: String, cards: List<CardDetails>) {
        val existingByRef =
            dao(context).listByPaymentApp(paymentAppId).associate { it.digitizationRef to it.payloadJson }
        dao(context).deleteByPaymentApp(paymentAppId)
        val entities = cards.mapNotNull { c ->
            val ref = c.digitizationReferenceNumber?.trim().orEmpty()
            if (ref.isEmpty()) return@mapNotNull null
            val merged = existingByRef[ref]?.let { json ->
                val old = gson.fromJson(json, CardDetails::class.java)
                var next = c
                if (c.cardImage.isNullOrEmpty() && !old.cardImage.isNullOrEmpty()) {
                    next = next.copy(
                        cardImage = old.cardImage,
                        cardImageHeight = old.cardImageHeight,
                        cardImageWidth = old.cardImageWidth
                    )
                }
                if (!old.cardNickname.isNullOrBlank()) {
                    next = next.copy(cardNickname = old.cardNickname)
                }
                next
            } ?: c
            val withUserNick = applyStoredUserNickname(context, merged)
            val image = withUserNick.cardImage.orEmpty()
            if (image.isNotEmpty()) {
                upsertCardImage(
                    context = context,
                    ref = ref,
                    paymentAppInstanceId = withUserNick.paymentAppInstanceId,
                    assetId = withUserNick.cardAssetId,
                    image = image,
                    height = withUserNick.cardImageHeight,
                    width = withUserNick.cardImageWidth
                )
            }
            val compact = withUserNick.copy(
                cardImage = null,
                cardImageHeight = null,
                cardImageWidth = null
            )
            HostWalletCardEntity(ref, compact.paymentAppInstanceId, gson.toJson(compact))
        }
        if (entities.isNotEmpty()) {
            dao(context).upsertAll(entities)
        }
    }

    /**
     * Saves provision API response into host cache for one payment app.
     *
     * @param context Context used to access host DB.
     * @param paymentAppId Payment app instance ID.
     * @param response Provision API response containing card list.
     */
    fun saveProvisionResponse(context: Context, paymentAppId: String, response: GetProvisionCardResponse?) {
        val list = response?.cardList.orEmpty()
        if (list.isEmpty()) {
            deleteUserNicknamesForPaymentApp(context, paymentAppId)
            imageDao(context).listByPaymentApp(paymentAppId).forEach { deleteImageFile(it.cardImage) }
            imageDao(context).deleteByPaymentApp(paymentAppId)
            dao(context).deleteByPaymentApp(paymentAppId)
            return
        }
        val sessionPur = StorageRepository.readString(PreferenceKey.PAN_UNIQUE_REFERENCE)
        val details = list.map {
            ProvisionResponseMapper.cardListToCardDetails(it, sessionPur)
        }
        replaceForPaymentApp(context, paymentAppId, details)
    }

    /**
     * Reads all cached cards for a payment app and applies user nickname overrides.
     *
     * @param context Context used to access host DB.
     * @param paymentAppId Payment app instance ID.
     * @return List of cached cards.
     */
    fun readForPaymentApp(context: Context, paymentAppId: String): List<CardDetails> {
        return dao(context).listByPaymentApp(paymentAppId)
            .map { gson.fromJson(it.payloadJson, CardDetails::class.java) }
            .map { card ->
                val ref = card.digitizationReferenceNumber?.trim().orEmpty()
                if (ref.isNotEmpty()) {
                    try {
                        imageDao(context).getByDigitizationRef(ref)?.let { image ->
                            card.cardImage = hydrateImagePayload(image)
                            card.cardImageHeight = image.cardImageHeight
                            card.cardImageWidth = image.cardImageWidth
                        }
                    } catch (e: SQLiteBlobTooBigException) {
                        Log.w(TAG, "Oversized image row for ref=$ref. Deleting cached image row.", e)
                        imageDao(context).deleteByDigitizationRef(ref)
                    }
                }
                applyStoredUserNickname(context, card)
            }
    }

    /**
     * Reads one cached card by digitization reference.
     *
     * @param context Context used to access host DB.
     * @param ref Digitization reference number.
     * @return Card details when found, otherwise null.
     */
    fun getByDigitizationRef(context: Context, ref: String): CardDetails? {
        val entity = dao(context).getByDigitizationRef(ref) ?: return null
        val card = gson.fromJson(entity.payloadJson, CardDetails::class.java)
        try {
            imageDao(context).getByDigitizationRef(ref)?.let { image ->
                card.cardImage = hydrateImagePayload(image)
                card.cardImageHeight = image.cardImageHeight
                card.cardImageWidth = image.cardImageWidth
            }
        } catch (e: SQLiteBlobTooBigException) {
            Log.w(TAG, "Oversized image row for ref=$ref. Deleting cached image row.", e)
            imageDao(context).deleteByDigitizationRef(ref)
        }
        return applyStoredUserNickname(context, card)
    }

    /**
     * Deletes one cached card and related nickname mappings.
     *
     * @param context Context used to access host DB.
     * @param ref Digitization reference number for the card.
     */
    fun deleteByDigitizationRef(context: Context, ref: String) {
        deleteUserNicknameForDigitizationRef(context, ref)
        imageDao(context).getByDigitizationRef(ref)?.let { deleteImageFile(it.cardImage) }
        deleteImageFileByRef(context, ref)
        imageDao(context).deleteByDigitizationRef(ref)
        dao(context).deleteByDigitizationRef(ref)
    }

    /**
     * Updates cached card status for one digitization reference.
     *
     * @param context Context used to access host DB.
     * @param digitizationRef Card digitization reference.
     * @param status New card status value.
     */
    fun updateCardStatus(context: Context, digitizationRef: String?, status: String?) {
        if (digitizationRef.isNullOrBlank()) return
        val entity = dao(context).getByDigitizationRef(digitizationRef) ?: return
        val card = gson.fromJson(entity.payloadJson, CardDetails::class.java)
        val updated = card.copy(cardStatus = status)
        dao(context).upsertAll(
            listOf(HostWalletCardEntity(digitizationRef, updated.paymentAppInstanceId, gson.toJson(updated)))
        )
    }

    /**
     * Updates nickname for a card identified by payment app and DPAN suffix.
     *
     * @param context Context used to access host DB.
     * @param paymentAppId Payment app instance ID.
     * @param dpanSuffix DPAN suffix to match.
     * @param nickname New user nickname value.
     */
    fun updateCardNicknameByDpan(context: Context, paymentAppId: String, dpanSuffix: String, nickname: String) {
        val target = normalizeDpan(dpanSuffix)
        if (target.isEmpty()) return
        val rows = dao(context).listByPaymentApp(paymentAppId)
        for (e in rows) {
            val card = gson.fromJson(e.payloadJson, CardDetails::class.java)
            if (normalizeDpan(card.dpanSuffix) == target) {
                e.digitizationRef.takeIf { it.isNotBlank() }?.let { ref ->
                    setUserNicknameForDigitizationRef(context, ref, nickname)
                }
                val updated = card.copy(cardNickname = nickname)
                dao(context).upsertAll(
                    listOf(HostWalletCardEntity(e.digitizationRef, updated.paymentAppInstanceId, gson.toJson(updated)))
                )
                return
            }
        }
    }

    /**
     * Removes persisted user nickname mapping for a card identifier.
     *
     * @param context Context used to access host DB.
     * @param paymentAppId Payment app instance ID.
     * @param cardId Card identifier, either DPAN suffix or digitization reference number.
     */
    fun removeUserNicknameForCard(context: Context, paymentAppId: String, cardId: String) {
        val key = cardId.trim()
        if (paymentAppId.isBlank() || key.isEmpty()) return

        deleteUserNicknameForDigitizationRef(context, key)

        dao(context).getByDigitizationRef(key)?.let { entity ->
            deleteUserNicknameForDigitizationRef(context, entity.digitizationRef)
        }
    }

    /**
     * Merges card image fields into an existing cached card row.
     *
     * @param context Context used to access host DB.
     * @param digitizationReferenceNumber Card digitization reference.
     * @param image Base64 or URL image content stored in card model.
     * @param height Optional card image height.
     * @param width Optional card image width.
     */
    fun mergeCardImage(
        context: Context,
        digitizationReferenceNumber: String,
        image: String,
        height: String?,
        width: String?
    ) {
        val entity = dao(context).getByDigitizationRef(digitizationReferenceNumber) ?: return
        val card = gson.fromJson(entity.payloadJson, CardDetails::class.java)
        val updated = card.copy(cardImage = null, cardImageHeight = null, cardImageWidth = null)
        upsertCardImage(
            context = context,
            ref = digitizationReferenceNumber,
            paymentAppInstanceId = updated.paymentAppInstanceId,
            assetId = updated.cardAssetId,
            image = image,
            height = height,
            width = width
        )
        dao(context).upsertAll(
            listOf(
                HostWalletCardEntity(
                    digitizationReferenceNumber,
                    updated.paymentAppInstanceId,
                    gson.toJson(updated)
                )
            )
        )
    }

    /**
     * Finds cached card image payload by asset ID.
     *
     * @param context Context used to access host DB.
     * @param assetId Card asset ID to match.
     * @return CardDetails carrying image and color fields, or empty CardDetails if not found.
     */
    fun findImageByAssetId(context: Context, assetId: String): CardDetails {
        val empty = CardDetails()
        if (assetId.isBlank()) return empty
        val image = try {
            imageDao(context).getByAssetId(assetId)
        } catch (e: SQLiteBlobTooBigException) {
            Log.w(TAG, "Oversized image row while reading assetId=$assetId. Clearing image cache.", e)
            imageDao(context).deleteAll()
            null
        } ?: return empty
        val card = image.digitizationRef.takeIf { it.isNotBlank() }?.let { ref ->
            dao(context).getByDigitizationRef(ref)?.let { gson.fromJson(it.payloadJson, CardDetails::class.java) }
        }
        return CardDetails(
            cardImage = hydrateImagePayload(image),
            cardImageHeight = image.cardImageHeight,
            cardImageWidth = image.cardImageWidth,
            foreGroundColor = card?.foreGroundColor,
            backGroundColor = card?.backGroundColor,
            labelColor = card?.labelColor
        )
    }

    /**
     * Creates a status API card row from cached data if payment app matches.
     *
     * @param context Context used to access host DB.
     * @param digitizationReferenceNumber Digitization reference to read.
     * @param paymentAppInstanceId Optional payment app scope check.
     * @return [CardList] payload for status API, or null when no matching card exists.
     */
    fun cardListForStatusApi(
        context: Context,
        digitizationReferenceNumber: String,
        paymentAppInstanceId: String
    ): CardList? {
        val details = getByDigitizationRef(context, digitizationReferenceNumber) ?: return null
        val storedPid = details.paymentAppInstanceId
        if (paymentAppInstanceId.isNotBlank() &&
            !storedPid.isNullOrBlank() &&
            storedPid != paymentAppInstanceId
        ) {
            return null
        }
        return ProvisionResponseMapper.cardDetailsToCardList(details)
    }

    /**
     * @param clearUserNicknames If false, only card rows are removed (e.g. pull-to-refresh) so nicknames re-apply after re-fetch.
     */
    fun clearAll(context: Context, clearUserNicknames: Boolean = true) {
        if (clearUserNicknames) {
            userNicknameDao(context).deleteAll()
        }
        imageDao(context).listAll().forEach { deleteImageFile(it.cardImage) }
        imageDao(context).deleteAll()
        dao(context).deleteAll()
    }

    /**
     * Clears all cached rows and nickname mappings for one payment app.
     *
     * @param context Context used to access host DB.
     * @param paymentAppId Payment app instance ID to clear.
     */
    fun clearPaymentApp(context: Context, paymentAppId: String) {
        deleteUserNicknamesForPaymentApp(context, paymentAppId)
        imageDao(context).listByPaymentApp(paymentAppId).forEach { deleteImageFile(it.cardImage) }
        imageDao(context).deleteByPaymentApp(paymentAppId)
        dao(context).deleteByPaymentApp(paymentAppId)
    }
}
