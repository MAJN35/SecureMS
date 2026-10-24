package com.example

import com.example.crypto.CryptoEngine
import com.example.crypto.PersianCiphertextCodec
import com.example.protocol.FragmentationEngine
import com.example.protocol.SmsPacket
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import javax.crypto.AEADBadTagException

class SecureSmsCryptoTest {

    @Test
    fun testAesGcmEncryptDecryptRoundtrip() {
        val aliceKp = CryptoEngine.generateIdentityKeyPair()
        val bobKp = CryptoEngine.generateIdentityKeyPair()

        val aliceShared = CryptoEngine.deriveSharedKey(aliceKp.private, bobKp.public)
        val bobShared = CryptoEngine.deriveSharedKey(bobKp.private, aliceKp.public)

        assertArrayEquals("Shared keys derived by both parties must match", aliceShared, bobShared)

        val plaintext = "سلام دنیا! این یک پیام فوق محرمانه است.".toByteArray(StandardCharsets.UTF_8)
        val aad = "Context-Bound-AAD-V1".toByteArray(StandardCharsets.UTF_8)

        val encrypted = CryptoEngine.encrypt(aliceShared, plaintext, aad)

        val decrypted = CryptoEngine.decrypt(bobShared, encrypted.iv, encrypted.ciphertext, aad)

        assertArrayEquals("Decrypted bytes must match original plaintext", plaintext, decrypted)
        assertEquals("سلام دنیا! این یک پیام فوق محرمانه است.", String(decrypted, StandardCharsets.UTF_8))
    }

    @Test
    fun testAuthenticationFailureWithWrongKey() {
        val aliceKp = CryptoEngine.generateIdentityKeyPair()
        val bobKp = CryptoEngine.generateIdentityKeyPair()
        val eveKp = CryptoEngine.generateIdentityKeyPair()

        val aliceShared = CryptoEngine.deriveSharedKey(aliceKp.private, bobKp.public)
        val eveShared = CryptoEngine.deriveSharedKey(eveKp.private, bobKp.public)

        val plaintext = "Secret message".toByteArray(StandardCharsets.UTF_8)
        val aad = "AAD".toByteArray(StandardCharsets.UTF_8)

        val encrypted = CryptoEngine.encrypt(aliceShared, plaintext, aad)

        try {
            CryptoEngine.decrypt(eveShared, encrypted.iv, encrypted.ciphertext, aad)
            fail("Decryption with wrong key must throw AEADBadTagException / GeneralSecurityException")
        } catch (e: GeneralSecurityException) {
            // Expected
        }
    }

    @Test
    fun testAuthenticationFailureWithTamperedCiphertext() {
        val kp = CryptoEngine.generateIdentityKeyPair()
        val key = CryptoEngine.deriveSharedKey(kp.private, kp.public)
        val plaintext = "Uncompromised Data".toByteArray(StandardCharsets.UTF_8)
        val aad = "AAD".toByteArray(StandardCharsets.UTF_8)

        val encrypted = CryptoEngine.encrypt(key, plaintext, aad)
        val tamperedCiphertext = encrypted.ciphertext.clone()
        // Flip single bit
        tamperedCiphertext[0] = (tamperedCiphertext[0].toInt() xor 0x01).toByte()

        try {
            CryptoEngine.decrypt(key, encrypted.iv, tamperedCiphertext, aad)
            fail("Decryption with tampered ciphertext must fail tag validation")
        } catch (e: GeneralSecurityException) {
            // Expected
        }
    }

    @Test
    fun testAuthenticationFailureWithTamperedAad() {
        val kp = CryptoEngine.generateIdentityKeyPair()
        val key = CryptoEngine.deriveSharedKey(kp.private, kp.public)
        val plaintext = "Uncompromised Data".toByteArray(StandardCharsets.UTF_8)
        val aad = "Sequence-1".toByteArray(StandardCharsets.UTF_8)
        val tamperedAad = "Sequence-2".toByteArray(StandardCharsets.UTF_8)

        val encrypted = CryptoEngine.encrypt(key, plaintext, aad)

        try {
            CryptoEngine.decrypt(key, encrypted.iv, encrypted.ciphertext, tamperedAad)
            fail("Decryption with tampered AAD must fail tag validation")
        } catch (e: GeneralSecurityException) {
            // Expected
        }
    }

    @Test
    fun testPersianLettersCodecRoundtrip() {
        val testBytes = "Hello دنیای رمزنگاری 12345!🔒".toByteArray(StandardCharsets.UTF_8)
        val encoded = PersianCiphertextCodec.encodeToPersianLetters(testBytes)

        assertTrue("Encoded payload must start with Persian prefix", encoded.startsWith(PersianCiphertextCodec.PREFIX_PERSIAN_LETTERS))
        assertTrue("Encoded payload must only contain Persian script", encoded.any { it in '\u0600'..'\u06FF' })

        val decoded = PersianCiphertextCodec.decodeFromPersianLetters(encoded)
        assertArrayEquals("Roundtrip Persian letters decoding must yield identical bytes", testBytes, decoded)
    }

    @Test
    fun testPersianNumeralsCodecRoundtrip() {
        val testBytes = byteArrayOf(0x00, 0x1A, 0x2F, 0x7E, 0xFF.toByte(), 0x80.toByte())
        val encoded = PersianCiphertextCodec.encodeToPersianNumerals(testBytes)

        assertTrue(encoded.startsWith(PersianCiphertextCodec.PREFIX_PERSIAN_NUMERALS))
        val decoded = PersianCiphertextCodec.decodeFromPersianNumerals(encoded)
        assertArrayEquals(testBytes, decoded)
    }

    @Test
    fun testUnicodeNormalizationResilience() {
        val raw = "Test Data".toByteArray(StandardCharsets.UTF_8)
        val encoded = PersianCiphertextCodec.encodeToPersianLetters(raw)

        // Inject carrier modifications: replace Persian Yeh with Arabic Yeh, add Tatweel and ZWNJ
        val mangled = encoded
            .replace('ی', '\u064A') // Arabic Yeh
            .replace('ک', '\u0643') // Arabic Kaf
            .replace("پر۱:", "پر۱:\u200Cـ ") // Add ZWNJ, tatweel and space

        val decoded = PersianCiphertextCodec.decodeFromPersianLetters(mangled)
        assertArrayEquals("Codec must correctly normalize Arabic variants and strip carrier artifacts", raw, decoded)
    }

    @Test
    fun testSafetyNumberSymmetry() {
        val aliceKp = CryptoEngine.generateIdentityKeyPair()
        val bobKp = CryptoEngine.generateIdentityKeyPair()

        val aliceCalculated = CryptoEngine.computeSafetyNumber(aliceKp.public, bobKp.public)
        val bobCalculated = CryptoEngine.computeSafetyNumber(bobKp.public, aliceKp.public)

        assertEquals("Safety numbers calculated from both sides must be identical", aliceCalculated, bobCalculated)
        assertEquals("Safety number must be 12 groups of 5 digits separated by spaces", 71, aliceCalculated.length) // 12*5 + 11 = 71
    }

    @Test
    fun testFragmentationAndOutOfOrderReassembly() {
        val engine = FragmentationEngine()
        val largeData = ByteArray(150) { it.toByte() }

        val fragments = engine.fragment(
            fullPayload = largeData,
            messageType = SmsPacket.MessageType.DATA,
            messageId = 42,
            senderKeyId = ByteArray(8) { 1 },
            recipientKeyId = ByteArray(8) { 2 },
            sequenceNumber = 10,
            maxChunkSize = 40
        )

        assertEquals(4, fragments.size) // 150 / 40 = 3.75 -> 4 fragments

        // Ingest out of order: 2, 0, 3, 1
        val res2 = engine.ingestFragment(fragments[2])
        assertTrue(res2 is FragmentationEngine.ReassemblyResult.Pending)

        val res0 = engine.ingestFragment(fragments[0])
        assertTrue(res0 is FragmentationEngine.ReassemblyResult.Pending)

        val res3 = engine.ingestFragment(fragments[3])
        assertTrue(res3 is FragmentationEngine.ReassemblyResult.Pending)

        val res1 = engine.ingestFragment(fragments[1])
        assertTrue(res1 is FragmentationEngine.ReassemblyResult.Complete)

        val complete = res1 as FragmentationEngine.ReassemblyResult.Complete
        assertArrayEquals("Out-of-order reassembly must reconstitute exact original byte payload", largeData, complete.fullPayload)
    }

    @Test
    fun testDuplicateFragmentDrop() {
        val engine = FragmentationEngine()
        val data = ByteArray(80) { (it * 2).toByte() }
        val fragments = engine.fragment(
            fullPayload = data,
            messageType = SmsPacket.MessageType.DATA,
            messageId = 105,
            senderKeyId = ByteArray(8),
            recipientKeyId = ByteArray(8),
            sequenceNumber = 1,
            maxChunkSize = 40
        )

        assertEquals(2, fragments.size)

        // Ingest fragment 0 twice
        engine.ingestFragment(fragments[0])
        val resDup = engine.ingestFragment(fragments[0])
        assertTrue(resDup is FragmentationEngine.ReassemblyResult.Pending)
        assertEquals(1, (resDup as FragmentationEngine.ReassemblyResult.Pending).receivedCount)

        // Ingest fragment 1 completes it
        val resComplete = engine.ingestFragment(fragments[1])
        assertTrue(resComplete is FragmentationEngine.ReassemblyResult.Complete)
        assertArrayEquals(data, (resComplete as FragmentationEngine.ReassemblyResult.Complete).fullPayload)
    }

    @Test
    fun testSmsPacketWireSerialization() {
        val originalPacket = SmsPacket(
            version = 1,
            messageType = SmsPacket.MessageType.DATA,
            messageId = 7777,
            fragmentIndex = 1,
            fragmentCount = 3,
            senderKeyId = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8),
            recipientKeyId = byteArrayOf(9, 10, 11, 12, 13, 14, 15, 16),
            sequenceNumber = 42,
            payload = "Wire Payload Test".toByteArray(StandardCharsets.UTF_8)
        )

        val serialized = originalPacket.toByteArray()
        val deserialized = SmsPacket.fromByteArray(serialized)

        assertEquals(originalPacket, deserialized)
        assertArrayEquals(originalPacket.payload, deserialized.payload)
    }

    @Test
    fun testLargeMessageAbove1KbRoundtrip() {
        val aliceKp = CryptoEngine.generateIdentityKeyPair()
        val bobKp = CryptoEngine.generateIdentityKeyPair()

        val aliceShared = CryptoEngine.deriveSharedKey(aliceKp.private, bobKp.public)
        val bobShared = CryptoEngine.deriveSharedKey(bobKp.private, aliceKp.public)

        // Generate large text > 2.5KB
        val sb = StringBuilder()
        for (i in 1..80) {
            sb.append("این سطر شماره $i از پیام طولانی امنیتی است. ")
        }
        val largePlaintext = sb.toString()
        val plaintextBytes = largePlaintext.toByteArray(StandardCharsets.UTF_8)
        assertTrue("Plaintext must be > 1KB (1024 bytes)", plaintextBytes.size > 2000)

        val messageId = 99999
        val dummyPacket = SmsPacket(
            version = 1,
            messageType = SmsPacket.MessageType.DATA,
            messageId = messageId,
            fragmentIndex = 0,
            fragmentCount = 1,
            senderKeyId = CryptoEngine.computeKeyId(aliceKp.public),
            recipientKeyId = CryptoEngine.computeKeyId(bobKp.public),
            sequenceNumber = 50,
            payload = ByteArray(0)
        )

        // Encrypt
        val encrypted = CryptoEngine.encrypt(aliceShared, plaintextBytes, dummyPacket.computeAad())
        val combined = encrypted.toCombined()

        // Fragment into chunks (e.g. 65 bytes per SMS segment)
        val engine = FragmentationEngine()
        val fragments = engine.fragment(
            fullPayload = combined,
            messageType = SmsPacket.MessageType.DATA,
            messageId = messageId,
            senderKeyId = CryptoEngine.computeKeyId(aliceKp.public),
            recipientKeyId = CryptoEngine.computeKeyId(bobKp.public),
            sequenceNumber = 50,
            maxChunkSize = 65
        )

        assertTrue("Multi-KB message must be split into multiple chunks (>15 chunks)", fragments.size > 15)

        // Stream chunk by chunk and verify progressive reassembly
        var finalResult: FragmentationEngine.ReassemblyResult? = null
        for ((idx, fragment) in fragments.withIndex()) {
            // Encode each fragment to Persian Letters Base32 as transmitted over SMS
            val encodedSms = PersianCiphertextCodec.encodeToPersianLetters(fragment.toByteArray())

            // Decode from SMS text back to wire bytes
            val (decodedBytes, _) = PersianCiphertextCodec.decodePayload(encodedSms)
            val parsedFragment = SmsPacket.fromByteArray(decodedBytes)

            val res = engine.ingestFragment(parsedFragment)
            if (idx < fragments.size - 1) {
                assertTrue("Intermediate chunks must be pending", res is FragmentationEngine.ReassemblyResult.Pending)
                val pending = res as FragmentationEngine.ReassemblyResult.Pending
                assertEquals(idx + 1, pending.receivedCount)
                assertEquals(fragments.size, pending.totalCount)
            } else {
                assertTrue("Final chunk must trigger complete reassembly", res is FragmentationEngine.ReassemblyResult.Complete)
                finalResult = res
            }
        }

        val complete = finalResult as FragmentationEngine.ReassemblyResult.Complete
        assertArrayEquals("Reassembled payload must match original ciphertext", combined, complete.fullPayload)

        // Decrypt with Bob's key
        val unpacked = CryptoEngine.EncryptedPayload.fromCombined(complete.fullPayload)
        val decrypted = CryptoEngine.decrypt(
            sharedKey = bobShared,
            iv = unpacked.iv,
            ciphertextWithTag = unpacked.ciphertext,
            aad = complete.representativePacket.computeAad()
        )

        assertEquals("Decrypted >1KB message must match original text exactly", largePlaintext, String(decrypted, StandardCharsets.UTF_8))
    }

    @Test
    fun testDirectionalMarkersAndPrefixVariants() {
        val testBytes = "تست نشانه‌های دوجهته و پیشوندهای مختلف".toByteArray(StandardCharsets.UTF_8)
        val encoded = PersianCiphertextCodec.encodeToPersianLetters(testBytes)

        // Case 1: LRM (\u200E) prefix
        val withLrm = "\u200E$encoded"
        assertTrue(PersianCiphertextCodec.isSecureSmsPayload(withLrm))
        assertArrayEquals(testBytes, PersianCiphertextCodec.decodeFromPersianLetters(withLrm))

        // Case 2: RLM (\u200F) prefix
        val withRlm = "\u200F$encoded"
        assertTrue(PersianCiphertextCodec.isSecureSmsPayload(withRlm))
        assertArrayEquals(testBytes, PersianCiphertextCodec.decodeFromPersianLetters(withRlm))

        // Case 3: Latin digit in prefix (پر1: instead of پر۱:)
        val withLatinDigit = encoded.replace("پر۱:", "پر1:")
        assertTrue(PersianCiphertextCodec.isSecureSmsPayload(withLatinDigit))
        assertArrayEquals(testBytes, PersianCiphertextCodec.decodeFromPersianLetters(withLatinDigit))

        // Case 4: Arabic digit in prefix (پر١:)
        val withArabicDigit = encoded.replace("پر۱:", "پر١:")
        assertTrue(PersianCiphertextCodec.isSecureSmsPayload(withArabicDigit))
        assertArrayEquals(testBytes, PersianCiphertextCodec.decodeFromPersianLetters(withArabicDigit))

        // Case 5: Space before colon (پر۱ :)
        val withSpaceColon = encoded.replace("پر۱:", "پر۱ :")
        assertTrue(PersianCiphertextCodec.isSecureSmsPayload(withSpaceColon))
        assertArrayEquals(testBytes, PersianCiphertextCodec.decodeFromPersianLetters(withSpaceColon))

        // Case 6: Automatic decodePayload with LRM
        val (decodedPayload, mode) = PersianCiphertextCodec.decodePayload(withLrm)
        assertEquals(PersianCiphertextCodec.EncodingMode.PERSIAN_LETTERS, mode)
        assertArrayEquals(testBytes, decodedPayload)
    }

    @Test
    fun testKeyboardVariantsToPersianLetters() {
        val testBytes = "تست حروف کیبورد".toByteArray(StandardCharsets.UTF_8)
        val encoded = PersianCiphertextCodec.encodeToPersianLetters(testBytes)

        // Replace Persian letters with Arabic / alternative keyboard chars
        val mangled = encoded
            .replace('ک', 'ك') // Arabic Kaf
            .replace('ی', 'ي') // Arabic Yeh
            .replace('ه', 'ة') // Teh Marbuta

        val decoded = PersianCiphertextCodec.decodeFromPersianLetters(mangled)
        assertArrayEquals(testBytes, decoded)
    }
}

