package com.example.crypto

import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * Reversible codec for transforming encrypted binary envelopes into:
 * 1. Base32 Persian Script characters (حروف فارسی)
 * 2. Persian Numerals and Symbols (اعداد و نمادهای فارسی)
 * 3. Standard ASCII Base64 format (قالب استاندارد)
 *
 * Implements strict Unicode normalization resilience against carrier modifications:
 * - Directional markers resilience: LRM (U+200E), RLM (U+200F), ALM (U+061C), BOM (U+FEFF),
 *   isolate marks (U+2066..U+2069), embedding marks (U+202A..U+202E).
 * - Prefix regex matching covering Persian/Arabic/Latin digits (۱, 1, ١) and punctuation (:, ؛).
 * - Maps Arabic Kaf (ك U+0643) to Persian Keheh (ک U+06A9)
 * - Maps Arabic Yeh (ي U+064A / ى U+0649 / ئ U+0626) to Persian Farsi Yeh (ی U+06CC)
 * - Maps Alef variants (آ U+0622, أ U+0623, إ U+0625, ٱ U+0671) to Alef (ا U+0627)
 * - Maps Teh Marbuta (ة U+0629) & Heh variants (ۀ U+06C0) to Heh (ه U+0647)
 * - Maps Waw with Hamza (ؤ U+0624) to Waw (و U+0648)
 * - Strips zero-width characters (ZWNJ U+200C, ZWJ U+200D, ZWSP U+200B), tatweel (ـ U+0640),
 *   and Arabic/Persian diacritics before decoding.
 */
object PersianCiphertextCodec {

    const val PREFIX_PERSIAN_LETTERS = "پر۱:" // Peh-Reh-1:
    const val PREFIX_PERSIAN_NUMERALS = "پن۱:" // Peh-Noon-1:
    const val PREFIX_STANDARD_B64 = "SSMS1:"

    // Flexible regex patterns for prefixes resilient to carrier/phone Unicode normalization
    private val PERSIAN_LETTERS_PREFIX_REGEX = Regex("^[\\u200E\\u200F\\u061C\\uFEFF\\s]*پر[۱1١]?[:؛\\s]*")
    private val PERSIAN_NUMERALS_PREFIX_REGEX = Regex("^[\\u200E\\u200F\\u061C\\uFEFF\\s]*پن[۱1١]?[:؛\\s]*")
    private val STANDARD_B64_PREFIX_REGEX = Regex("^[\\u200E\\u200F\\u061C\\uFEFF\\s]*SSMS[1۱]?[:؛\\s]*")

    enum class EncodingMode(val titleFa: String, val titleEn: String) {
        AUTOMATIC("خودکار", "Automatic"),
        STANDARD_B64("قالب استاندارد (Base64)", "Standard Base64"),
        PERSIAN_LETTERS("حروف فارسی (Base32)", "Persian Letters (Base32)"),
        PERSIAN_NUMERALS("اعداد و نمادهای فارسی", "Persian Numerals")
    }

    // 32 distinct Persian letters (Alef to Yeh) mapped to 5-bit integers (0..31)
    private val PERSIAN_ALPHABET = charArrayOf(
        'ا', 'ب', 'پ', 'ت', 'ث', 'ج', 'چ', 'ح', // 0..7
        'خ', 'د', 'ذ', 'ر', 'ز', 'ژ', 'س', 'ش', // 8..15
        'ص', 'ض', 'ط', 'ظ', 'ع', 'غ', 'ف', 'ق', // 16..23
        'ک', 'گ', 'ل', 'م', 'ن', 'و', 'ه', 'ی'  // 24..31
    )

    // Lookup table for decoding (resilient to Arabic & regional Unicode variants)
    private val DECODE_MAP = HashMap<Char, Int>().apply {
        PERSIAN_ALPHABET.forEachIndexed { index, char ->
            put(char, index)
        }
        // Normalization mappings for carrier/keyboard variance
        put('\u0643', 24) // Arabic Kaf -> Persian Keheh (ک)
        put('\u064A', 31) // Arabic Yeh (ي) -> Persian Farsi Yeh (ی)
        put('\u0649', 31) // Arabic Alef Maksura (ى) -> Persian Farsi Yeh (ی)
        put('\u0626', 31) // Yeh with Hamza Above (ئ) -> Persian Farsi Yeh (ی)
        put('\u0622', 0)  // Alef with Madda (آ) -> Alef (ا)
        put('\u0623', 0)  // Alef with Hamza Above (أ) -> Alef (ا)
        put('\u0625', 0)  // Alef with Hamza Below (إ) -> Alef (ا)
        put('\u0671', 0)  // Alef Wasla (ٱ) -> Alef (ا)
        put('\u0624', 29) // Waw with Hamza (ؤ) -> Waw (و)
        put('\u0629', 30) // Teh Marbuta (ة) -> Heh (ه)
        put('\u06C0', 30) // Heh with Yeh above (ۀ) -> Heh (ه)
        put('\u06C1', 30) // Urdu Heh Goal (ہ) -> Heh (ه)
    }

    // 16 Persian numerals and symbols for 4-bit nibble encoding
    private val PERSIAN_HEX_ALPHABET = charArrayOf(
        '۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', // 0..7
        '۸', '۹', '؛', '،', '؟', '٪', '«', '»'  // 8..15
    )

    private val DECODE_HEX_MAP = HashMap<Char, Int>().apply {
        PERSIAN_HEX_ALPHABET.forEachIndexed { index, char ->
            put(char, index)
        }
        // Normalize Arabic digits (U+0660..U+0669) and ASCII digits (0..9)
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        arabicDigits.forEachIndexed { index, char ->
            put(char, index)
        }
        for (i in 0..9) {
            put(('0'.code + i).toChar(), i)
        }
        put(';', 10)
        put(',', 11)
        put('?', 12)
        put('%', 13)
    }

    /**
     * Clean leading and trailing invisible artifacts (directional marks, BOM, whitespaces).
     */
    fun cleanLeadingArtifacts(text: String): String {
        var start = 0
        var end = text.length
        while (start < end && isIgnorableLeadingChar(text[start])) {
            start++
        }
        while (end > start && isIgnorableLeadingChar(text[end - 1])) {
            end--
        }
        return if (start > 0 || end < text.length) text.substring(start, end) else text
    }

    private fun isIgnorableLeadingChar(c: Char): Boolean {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' ||
                c == '\u200E' || c == '\u200F' || c == '\u061C' || c == '\uFEFF' ||
                c == '\u200B' || c == '\u200C' || c == '\u200D' ||
                (c in '\u202A'..'\u202E') || (c in '\u2066'..'\u2069')
    }

    /**
     * Encode binary bytes into Persian Letters (Base32) with version prefix.
     */
    fun encodeToPersianLetters(bytes: ByteArray): String {
        // Prepend 2-byte big-endian length to avoid padding ambiguity
        val length = bytes.size
        require(length <= 0xFFFF) { "Payload too large for SMS envelope" }

        val prefixed = ByteArray(length + 2)
        prefixed[0] = ((length shr 8) and 0xFF).toByte()
        prefixed[1] = (length and 0xFF).toByte()
        System.arraycopy(bytes, 0, prefixed, 2, length)

        val sb = java.lang.StringBuilder(PREFIX_PERSIAN_LETTERS)
        var buffer = 0
        var bitsLeft = 0

        for (b in prefixed) {
            buffer = (buffer shl 8) or (b.toInt() and 0xFF)
            bitsLeft += 8

            while (bitsLeft >= 5) {
                bitsLeft -= 5
                val index = (buffer shr bitsLeft) and 0x1F
                sb.append(PERSIAN_ALPHABET[index])
            }
        }

        if (bitsLeft > 0) {
            val index = (buffer shl (5 - bitsLeft)) and 0x1F
            sb.append(PERSIAN_ALPHABET[index])
        }

        return sb.toString()
    }

    /**
     * Decode Persian Letters (Base32) back to exact binary bytes.
     */
    fun decodeFromPersianLetters(encodedWithPrefix: String): ByteArray {
        var cleanStr = cleanLeadingArtifacts(encodedWithPrefix)

        // Resilient prefix removal
        val match = PERSIAN_LETTERS_PREFIX_REGEX.find(cleanStr)
        if (match != null && match.range.first == 0) {
            cleanStr = cleanStr.substring(match.range.last + 1)
        } else if (cleanStr.startsWith(PREFIX_PERSIAN_LETTERS)) {
            cleanStr = cleanStr.substring(PREFIX_PERSIAN_LETTERS.length)
        }

        val filteredChars = cleanTextForDecoding(cleanStr)
        val outStream = ByteArrayOutputStream()

        var buffer = 0
        var bitsLeft = 0

        for (c in filteredChars) {
            val value = DECODE_MAP[c] ?: continue
            buffer = (buffer shl 5) or value
            bitsLeft += 5

            if (bitsLeft >= 8) {
                bitsLeft -= 8
                val byteVal = (buffer shr bitsLeft) and 0xFF
                outStream.write(byteVal)
            }
        }

        val allBytes = outStream.toByteArray()
        require(allBytes.size >= 2) { "Decoded Persian payload is truncated (missing length header)" }

        val length = ((allBytes[0].toInt() and 0xFF) shl 8) or (allBytes[1].toInt() and 0xFF)
        val available = allBytes.size - 2
        val extractLength = Math.min(length, available)
        require(extractLength >= 0) { "Invalid payload length header: $length" }

        val result = ByteArray(extractLength)
        System.arraycopy(allBytes, 2, result, 0, extractLength)
        return result
    }

    /**
     * Encode binary bytes into Persian Numerals and Symbols with version prefix.
     */
    fun encodeToPersianNumerals(bytes: ByteArray): String {
        val sb = java.lang.StringBuilder(PREFIX_PERSIAN_NUMERALS)
        for (b in bytes) {
            val high = (b.toInt() ushr 4) and 0x0F
            val low = b.toInt() and 0x0F
            sb.append(PERSIAN_HEX_ALPHABET[high])
            sb.append(PERSIAN_HEX_ALPHABET[low])
        }
        return sb.toString()
    }

    /**
     * Decode Persian Numerals back to binary bytes.
     */
    fun decodeFromPersianNumerals(encodedWithPrefix: String): ByteArray {
        var cleanStr = cleanLeadingArtifacts(encodedWithPrefix)
        val match = PERSIAN_NUMERALS_PREFIX_REGEX.find(cleanStr)
        if (match != null && match.range.first == 0) {
            cleanStr = cleanStr.substring(match.range.last + 1)
        } else if (cleanStr.startsWith(PREFIX_PERSIAN_NUMERALS)) {
            cleanStr = cleanStr.substring(PREFIX_PERSIAN_NUMERALS.length)
        }

        val filtered = cleanTextForDecoding(cleanStr)
        val outStream = ByteArrayOutputStream()
        var highNibble: Int? = null

        for (c in filtered) {
            val nibble = DECODE_HEX_MAP[c] ?: continue
            if (highNibble == null) {
                highNibble = nibble
            } else {
                val byteVal = (highNibble shl 4) or nibble
                outStream.write(byteVal)
                highNibble = null
            }
        }

        return outStream.toByteArray()
    }

    /**
     * Standard Base64 encoding.
     */
    fun encodeToStandardBase64(bytes: ByteArray): String {
        return PREFIX_STANDARD_B64 + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * Decode Standard Base64.
     */
    fun decodeFromStandardBase64(encodedWithPrefix: String): ByteArray {
        var clean = cleanLeadingArtifacts(encodedWithPrefix)
        val match = STANDARD_B64_PREFIX_REGEX.find(clean)
        if (match != null && match.range.first == 0) {
            clean = clean.substring(match.range.last + 1)
        } else if (clean.startsWith(PREFIX_STANDARD_B64)) {
            clean = clean.substring(PREFIX_STANDARD_B64.length)
        }
        // Remove whitespace and clean up
        val sb = StringBuilder()
        for (c in clean) {
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                sb.append(c)
            }
        }
        return Base64.decode(sb.toString(), Base64.NO_WRAP)
    }

    /**
     * Identify and decode any supported SecureSMS payload format automatically.
     */
    fun decodePayload(rawSmsText: String): Pair<ByteArray, EncodingMode> {
        val cleaned = cleanLeadingArtifacts(rawSmsText)

        return when {
            PERSIAN_LETTERS_PREFIX_REGEX.containsMatchIn(cleaned) || cleaned.startsWith(PREFIX_PERSIAN_LETTERS) -> {
                Pair(decodeFromPersianLetters(cleaned), EncodingMode.PERSIAN_LETTERS)
            }
            PERSIAN_NUMERALS_PREFIX_REGEX.containsMatchIn(cleaned) || cleaned.startsWith(PREFIX_PERSIAN_NUMERALS) -> {
                Pair(decodeFromPersianNumerals(cleaned), EncodingMode.PERSIAN_NUMERALS)
            }
            STANDARD_B64_PREFIX_REGEX.containsMatchIn(cleaned) || cleaned.startsWith(PREFIX_STANDARD_B64) -> {
                Pair(decodeFromStandardBase64(cleaned), EncodingMode.STANDARD_B64)
            }
            else -> {
                // Heuristic check: if contains Persian letters, attempt Persian decode, else Base64
                val hasPersian = cleaned.any { it in '\u0600'..'\u06FF' }
                if (hasPersian) {
                    try {
                        Pair(decodeFromPersianLetters(cleaned), EncodingMode.PERSIAN_LETTERS)
                    } catch (e: Exception) {
                        Pair(decodeFromPersianNumerals(cleaned), EncodingMode.PERSIAN_NUMERALS)
                    }
                } else {
                    Pair(decodeFromStandardBase64(cleaned), EncodingMode.STANDARD_B64)
                }
            }
        }
    }

    fun isSecureSmsPayload(text: String): Boolean {
        val cleaned = cleanLeadingArtifacts(text)
        return PERSIAN_LETTERS_PREFIX_REGEX.containsMatchIn(cleaned) ||
                PERSIAN_NUMERALS_PREFIX_REGEX.containsMatchIn(cleaned) ||
                STANDARD_B64_PREFIX_REGEX.containsMatchIn(cleaned) ||
                cleaned.startsWith(PREFIX_PERSIAN_LETTERS) ||
                cleaned.startsWith(PREFIX_PERSIAN_NUMERALS) ||
                cleaned.startsWith(PREFIX_STANDARD_B64)
    }

    /**
     * Strips carrier modifications: spaces, tabs, newlines, zero-width characters,
     * directional markers, tatweel, and diacritics.
     */
    fun cleanTextForDecoding(text: String): CharArray {
        val sb = java.lang.StringBuilder(text.length)
        for (c in text) {
            when (c) {
                ' ', '\t', '\n', '\r' -> {} // whitespace
                '\u200B', '\u200C', '\u200D', '\u200E', '\u200F', '\u061C', '\uFEFF' -> {} // zero-width & directional
                in '\u202A'..'\u202E' -> {} // directional embed/override
                in '\u2066'..'\u2069' -> {} // directional isolate
                '\u0640' -> {}              // Tatweel
                in '\u064B'..'\u065F' -> {}  // Arabic/Persian harakat diacritics
                else -> sb.append(c)
            }
        }
        return sb.toString().toCharArray()
    }
}
