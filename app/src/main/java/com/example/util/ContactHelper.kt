package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
import android.util.LruCache
import java.io.File
import java.io.FileOutputStream

data class DeviceContact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val photoUri: String? = null
)

object ContactHelper {

    private val avatarBitmapCache = object : LruCache<String, Bitmap>(32) {}

    /**
     * Normalizes phone numbers by removing spaces, dashes, parentheses and other formatting.
     */
    fun normalizePhoneNumber(phone: String): String {
        return phone.replace(Regex("[^0-9+]"), "").trim()
    }

    /**
     * Extracts only numeric digits from a phone number string.
     */
    fun cleanDigits(phone: String): String {
        return phone.replace(Regex("[^0-9]"), "")
    }

    /**
     * Determines whether two phone number strings refer to the same person.
     * Handles national vs international prefixes, e.g.:
     * - Iranian numbers: +989123456789, 09123456789, 9123456789
     * - US/International: +1 (555) 123-4567, 5551234567
     */
    fun isSamePhoneNumber(phone1: String, phone2: String): Boolean {
        val d1 = cleanDigits(phone1)
        val d2 = cleanDigits(phone2)

        if (d1.isEmpty() || d2.isEmpty()) return false
        if (d1 == d2) return true

        // Check if one is a suffix of the other with at least 8-9 significant digits
        val minLen = Math.min(d1.length, d2.length)
        if (minLen >= 9) {
            val suffix1 = d1.takeLast(9)
            val suffix2 = d2.takeLast(9)
            if (suffix1 == suffix2) return true
        }

        return false
    }

    /**
     * Reads contacts with phone numbers and optional photo URIs from the device's local address book.
     */
    fun getDeviceContacts(context: Context): List<DeviceContact> {
        val contactsList = mutableListOf<DeviceContact>()
        val seenPhones = mutableSetOf<String>()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
            ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
        )

        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use {
                val idIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                val thumbIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

                while (it.moveToNext()) {
                    val id = if (idIdx >= 0) it.getString(idIdx) ?: "" else ""
                    val name = if (nameIdx >= 0) it.getString(nameIdx) ?: "مخاطب" else "مخاطب"
                    val rawNumber = if (numberIdx >= 0) it.getString(numberIdx) ?: "" else ""
                    val photoUri = (if (photoIdx >= 0) it.getString(photoIdx) else null)
                        ?: (if (thumbIdx >= 0) it.getString(thumbIdx) else null)

                    val cleanNumber = normalizePhoneNumber(rawNumber)
                    if (cleanNumber.isNotBlank() && cleanNumber !in seenPhones) {
                        seenPhones.add(cleanNumber)
                        contactsList.add(
                            DeviceContact(
                                id = id,
                                name = name.trim(),
                                phoneNumber = cleanNumber,
                                photoUri = photoUri?.takeIf { u -> u.isNotBlank() }
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Permission denied or query failed
        }

        return contactsList
    }

    /**
     * Resolves the contact display name from the device phonebook for a given phone number.
     */
    fun getContactNameByPhone(context: Context, phoneNumber: String): String? {
        val clean = cleanDigits(phoneNumber)
        if (clean.isBlank()) return null

        try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            val cursor = context.contentResolver.query(uri, projection, null, null, null)

            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIdx = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIdx >= 0) {
                        val name = it.getString(nameIdx)
                        if (!name.isNullOrBlank()) return name.trim()
                    }
                }
            }
        } catch (e: Exception) {
            // PhoneLookup error
        }

        try {
            val allContacts = getDeviceContacts(context)
            for (contact in allContacts) {
                if (isSamePhoneNumber(contact.phoneNumber, phoneNumber)) {
                    return contact.name
                }
            }
        } catch (e: Exception) {
            // Fallback error
        }

        return null
    }

    /**
     * Resolves the contact photo URI from the device phonebook for a given phone number.
     */
    fun getContactPhotoUriByPhone(context: Context, phoneNumber: String): String? {
        val clean = cleanDigits(phoneNumber)
        if (clean.isBlank()) return null

        try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(
                ContactsContract.PhoneLookup.PHOTO_URI,
                ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI
            )
            val cursor = context.contentResolver.query(uri, projection, null, null, null)

            cursor?.use {
                if (it.moveToFirst()) {
                    val photoIdx = it.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI)
                    val thumbIdx = it.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI)
                    val photo = (if (photoIdx >= 0) it.getString(photoIdx) else null)
                        ?: (if (thumbIdx >= 0) it.getString(thumbIdx) else null)
                    if (!photo.isNullOrBlank()) return photo
                }
            }
        } catch (e: Exception) {
            // PhoneLookup error
        }

        try {
            val allContacts = getDeviceContacts(context)
            for (contact in allContacts) {
                if (isSamePhoneNumber(contact.phoneNumber, phoneNumber) && !contact.photoUri.isNullOrBlank()) {
                    return contact.photoUri
                }
            }
        } catch (e: Exception) {
            // Fallback error
        }

        return null
    }

    /**
     * Copies an image from a gallery picker or contact photo URI into internal app storage,
     * center-cropping and scaling to 320x320 so it remains permanently accessible offline.
     */
    fun copyUriToInternalAvatarStorage(
        context: Context,
        sourceUri: Uri,
        identifier: String
    ): String? {
        return try {
            val rawBitmap = context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return null

            val croppedSquare = centerCropSquare(rawBitmap, 320)
            val avatarsDir = File(context.filesDir, "avatars")
            if (!avatarsDir.exists()) {
                avatarsDir.mkdirs()
            }

            val safeId = identifier.replace(Regex("[^a-zA-Z0-9_]"), "_").take(24)
            val destFile = File(avatarsDir, "avatar_${safeId}_${System.currentTimeMillis()}.jpg")

            FileOutputStream(destFile).use { out ->
                croppedSquare.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            val fileUri = Uri.fromFile(destFile).toString()
            avatarBitmapCache.put(fileUri, croppedSquare)
            fileUri
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Loads and caches a Bitmap from a local file:// URI, path, or content:// URI.
     */
    fun loadAvatarBitmap(context: Context, avatarUri: String): Bitmap? {
        if (avatarUri.isBlank() || avatarUri.startsWith("preset:")) return null
        avatarBitmapCache.get(avatarUri)?.let { return it }

        return try {
            val bmp = when {
                avatarUri.startsWith("file://") -> {
                    val path = Uri.parse(avatarUri).path ?: return null
                    BitmapFactory.decodeFile(path)
                }
                avatarUri.startsWith("/") -> {
                    BitmapFactory.decodeFile(avatarUri)
                }
                else -> {
                    val uri = Uri.parse(avatarUri)
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }
            }
            if (bmp != null) {
                val square = centerCropSquare(bmp, 320)
                avatarBitmapCache.put(avatarUri, square)
                square
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun centerCropSquare(src: Bitmap, targetSize: Int): Bitmap {
        val width = src.width
        val height = src.height
        if (width <= 0 || height <= 0) return src
        val minEdge = Math.min(width, height)
        val xOffset = (width - minEdge) / 2
        val yOffset = (height - minEdge) / 2
        val cropped = Bitmap.createBitmap(src, xOffset, yOffset, minEdge, minEdge)
        return if (minEdge != targetSize) {
            Bitmap.createScaledBitmap(cropped, targetSize, targetSize, true)
        } else {
            cropped
        }
    }

    /**
     * Resolves display name, returning the resolved contact name or the formatted phone number.
     */
    fun resolveContactName(context: Context, phoneNumber: String): String {
        return getContactNameByPhone(context, phoneNumber) ?: phoneNumber
    }
}
