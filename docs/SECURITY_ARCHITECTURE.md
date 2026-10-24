# SecureSMS — Security Architecture & Protocol Specification

## 1. Threat Model & Security Bounds

### 1.1 Protected Assets & Mitigations
- **Message Content Confidentiality**: Protected by authenticated AES-256-GCM encryption with NIST P-256 ECDH key agreement. Cellular carriers and SMS transit nodes only observe high-entropy ciphertext.
- **Message Integrity & Tamper Resistance**: 128-bit Poly1305 / GCM authentication tag over the ciphertext and Additional Authenticated Data (AAD) prevents bit-flipping, truncation, or ciphertext modification.
- **Replay Protection**: Strict monotonic sequence counters per conversation session and unique 32-bit random message correlation IDs.
- **Identity Binding & Spoofing Defense**: Sender and recipient public key IDs are bound into the AAD. Messages injected under a forged sender identity fail AEAD authentication.
- **Data-at-Rest Protection**: Local database payloads and private identity keys are encrypted using an AES-256 master key generated and guarded within the hardware-backed `AndroidKeyStore`.
- **Zero Cloud & Zero Network Leakage**: Application explicitly omits `android.permission.INTERNET`. Zero external servers, Firebase instances, telemetry nodes, or cloud relays exist.

### 1.2 Inherent SMS & Platform Limitations (Unpreventable Threats)
- **Cellular Traffic Analysis**: Cellular base stations and telco providers inevitably record sender/recipient MSISDN (phone numbers), timestamps, cell tower locations, and message volume.
- **SIM Swapping / Number Hijacking**: Cellular operators can reassign numbers to unauthorized SIM cards. Mitigated by SecureSMS's **Safety Number comparison** and **Identity Changed** warning banners, preventing silent MITM.
- **Denial of Service & Message Dropping**: Carriers can drop SMS segments. SecureSMS surfaces local delivery states (Queued, Sent to Carrier, Delivered, Failed, Incomplete) but cannot force carrier delivery.
- **Compromised Host Device**: A compromised Android device with root access, keyloggers, or malware can read decrypted contents in memory or capture screen contents.

---

## 2. Cryptographic Architecture & Primitives

- **Asymmetric Curve**: NIST P-256 (`secp256r1`) via native Android JCA (universal on Android API 24+ without third-party native binaries).
- **Key Agreement**: Ephemeral-static Elliptic Curve Diffie-Hellman (`ECDH`).
- **Key Derivation Function**: HKDF-SHA256 (`RFC 5869`) with context salt `"SecureSMS-v1-Salt"` and info `"SecureSMS-AES256-GCM-Session"`.
- **Symmetric Cipher**: AES-256 in Galois/Counter Mode (`AES/GCM/NoPadding`).
- **IV / Nonce**: 12-byte cryptographically secure random bytes from `SecureRandom`. Never reused.
- **Tag**: 128-bit authentication tag.
- **Safety Number**: Lexicographically ordered public key concatenation fed into SHA-512 stretched over 5,200 iterations, formatted into 12 groups of 5 decimal digits (Signal-compatible representation).
- **At-Rest Protection**: Master AES-256 key generated in `AndroidKeyStore` (`PURPOSE_ENCRYPT | PURPOSE_DECRYPT`, GCM mode).

---

## 3. SMS Wire Protocol Specification

### 3.1 Binary Envelope Header (29 bytes)
```
+---------------------------------------------------------------------------------------------+
| Field Name               | Offset | Size (Bytes) | Description                               |
+---------------------------------------------------------------------------------------------+
| Magic Bytes              | 0      | 2            | 0x53, 0x53 ('SS')                         |
| Protocol Version         | 2      | 1            | 0x01                                      |
| Message Type             | 3      | 1            | 0x01=DATA, 0x02=KEY_OFFER, 0x04=ACK       |
| Message Payload ID       | 4      | 4            | Unique uint32 correlating packet fragments |
| Fragment Index           | 8      | 1            | 0-indexed fragment number (0..Count-1)    |
| Fragment Count           | 9      | 1            | Total fragments (1..20)                   |
| Sender Key ID            | 10     | 8            | Truncated SHA-256 of sender public key    |
| Recipient Key ID         | 18     | 8            | Truncated SHA-256 of recipient public key |
| Sequence Number          | 26     | 4            | Monotonic uint32 session counter          |
| Payload Chunk            | 30     | Variable     | IV + Ciphertext + Tag slice               |
+---------------------------------------------------------------------------------------------+
```

### 3.2 Additional Authenticated Data (AAD) Binding
The AAD fed into `Cipher.updateAAD(...)` binds:
`[Version (1B) | Type (1B) | MessageId (4B) | SenderKeyId (8B) | RecipientKeyId (8B) | SeqNum (4B)]` (26 bytes).

---

## 4. Persian-Script Ciphertext Codec (پشتیبانی از متن رمزگذاریشده فارسی)

### 4.1 Base32 Persian Letter Mapping
Binary chunks are grouped into 5-bit increments (values 0..31) mapped directly to 32 distinct Persian alphabet characters:
`['ا', 'ب', 'پ', 'ت', 'ث', 'ج', 'چ', 'ح', 'خ', 'د', 'ذ', 'ر', 'ز', 'ژ', 'س', 'ش', 'ص', 'ض', 'ط', 'ظ', 'ع', 'غ', 'ف', 'ق', 'ک', 'گ', 'ل', 'م', 'ن', 'و', 'ه', 'ی']`

### 4.2 Carrier Normalization Resilience
- Maps Arabic Kaf (`ك` U+0643) to Persian Keheh (`ک` U+06A9)
- Maps Arabic Yeh (`ي` U+064A / `ى` U+0649) to Persian Farsi Yeh (`ی` U+06CC)
- Maps Alef with Madda (`آ` U+0622) and Hamza (`أ` U+0623) to Alef (`ا` U+0627)
- Strips zero-width characters (ZWNJ U+200C, ZWJ U+200D), tatweel (`ـ` U+0640), and diacritics before decoding.
- Prefix: `پر۱:` (`PREFIX_PERSIAN_LETTERS`).

---

## 5. iOS Platform Feasibility Assessment

Direct programmatic transmission and background interception of SMS are strictly restricted on iOS:
1. **Background SMS Interception**: Public iOS APIs (`MessageUI`, `CoreTelephony`) do not provide a `BroadcastReceiver` or background daemon to silently capture incoming SMS. The iOS SMS Filtering Extension (`ILMessageFilterExtension`) only allows classifying SMS as Junk/Promotion, but cannot decrypt and inject messages into a private app database.
2. **Programmatic Sending**: `MFMessageComposeViewController` requires user interaction and display of the system SMS composition sheet. Silent SMS background dispatch without user tapping "Send" is restricted to carrier-entitled or MDM applications.
3. **Conclusion**: Production-grade zero-interaction background SMS encryption cannot be safely or officially implemented on stock iOS without private entitlements or jailbreaking. Android provides the only supported, secure architecture for background SMS encryption via `BroadcastReceiver` and `SmsManager`.

---

## 6. Security Audit Checklist

- [x] Zero network calls / No `android.permission.INTERNET`.
- [x] Cryptographic randomness from `SecureRandom`.
- [x] Standard NIST P-256 ECDH + HKDF-SHA256 + AES-256-GCM.
- [x] No nonce or IV reuse (12-byte random IV per message).
- [x] Strict AEAD tag validation with context-bound AAD.
- [x] Replay attack rejection via monotonic sequence counters.
- [x] Out-of-order fragment reassembly with 24-hour expiration purging.
- [x] Carrier character normalization for Persian/Arabic Unicode variants.
- [x] Local storage encrypted via hardware-backed `AndroidKeyStore`.
- [x] Notification message preview hiding option.
- [x] In-app Virtual SMS Lab for automated end-to-end testing without external network.
