package io.github.kishigaki.calendar.others.cipher

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.alloc
import kotlinx.cinterop.refTo
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.ptr
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.get
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import platform.Security.kSecReturnData
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFDataCreate
import kotlinx.cinterop.readValue
import platform.CoreCrypto.kCCKeySizeAES256
import platform.CoreCrypto.kCCBlockSizeAES128
import platform.CoreCrypto.CCCrypt
import platform.CoreCrypto.kCCEncrypt
import platform.CoreCrypto.kCCDecrypt
import platform.CoreCrypto.kCCAlgorithmAES
import platform.CoreCrypto.kCCOptionPKCS7Padding
import platform.CoreCrypto.kCCSuccess
import platform.Security.SecRandomCopyBytes
import platform.Security.kSecRandomDefault
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleWhenUnlocked
import platform.Security.kSecAttrService
import platform.Security.kSecAttrAccount
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecClass
import platform.Security.kSecValueData
import platform.Security.SecItemDelete
import platform.Security.SecItemAdd
import platform.Security.errSecItemNotFound
import platform.Security.SecItemCopyMatching
import platform.CoreFoundation.CFGetTypeID
import platform.CoreFoundation.CFDataGetTypeID
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.CFStringCreateWithCString
import platform.posix.size_tVar
import kotlinx.cinterop.value

actual class AesCommonKey(val rawValue: ByteArray)

actual fun createKeyProvider() = object : PersistentAesCommonKeyProvider {
    override fun loadOrCreateKey(): AesCommonKey {
        @Suppress("SpellCheckingInspection")
        val secureKeyTag = "com.kishigaki.calendar.key.aes"
        var key = loadAesKeyFromKeychain(secureKeyTag)
        if (key == null) {
            key = generateRandomBytes(kCCKeySizeAES256.toInt())
            storeAesKeyInKeychain(secureKeyTag, key)
        }
        return AesCommonKey(key)
    }
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun aesCbcEncrypt(key: AesCommonKey, iv: ByteArray, data: ByteArray): ByteArray = memScoped {
    val dataOutSize = data.size + kCCBlockSizeAES128.toInt()
    val dataOut = allocArray<ByteVar>(dataOutSize)
    val dataOutMoved = alloc<size_tVar>()

    val status = CCCrypt(
        op = kCCEncrypt,
        alg = kCCAlgorithmAES,
        options = kCCOptionPKCS7Padding,
        key = key.rawValue.refTo(0),
        keyLength = key.rawValue.size.convert(),
        iv = iv.refTo(0),
        dataIn = data.refTo(0),
        dataInLength = data.size.convert(),
        dataOut = dataOut,
        dataOutAvailable = dataOutSize.convert(),
        dataOutMoved = dataOutMoved.ptr
    )

    if (status != kCCSuccess) {
        error("Encryption failed. status = $status")
    }

    val encryptedSize = dataOutMoved.value.toInt()
    val encryptedBytes = ByteArray(encryptedSize)
    for (i in 0 until encryptedSize) {
        encryptedBytes[i] = dataOut[i]
    }
    return encryptedBytes
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun aesCbcDecrypt(key: AesCommonKey, iv: ByteArray, data: ByteArray): ByteArray? = memScoped {
    val dataOutSize = data.size
    val dataOut = allocArray<ByteVar>(dataOutSize)
    val dataOutMoved = alloc<size_tVar>()

    val status = CCCrypt(
        op = kCCDecrypt,
        alg = kCCAlgorithmAES,
        options = kCCOptionPKCS7Padding,
        key = key.rawValue.refTo(0),
        keyLength = key.rawValue.size.convert(),
        iv = iv.refTo(0),
        dataIn = data.refTo(0),
        dataInLength = data.size.convert(),
        dataOut = dataOut,
        dataOutAvailable = dataOutSize.convert(),
        dataOutMoved = dataOutMoved.ptr
    )

    if (status != kCCSuccess) {
        error("Decryption failed. status = $status")
    }

    val decryptedSize = dataOutMoved.value.toInt()

    val decryptedBytes = ByteArray(decryptedSize)
    for (i in 0 until decryptedSize) {
        decryptedBytes[i] = dataOut[i]
    }
    return decryptedBytes
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun generateRandomBytes(size: Int): ByteArray {
    val key = ByteArray(size)
    key.usePinned {
        val result = SecRandomCopyBytes(kSecRandomDefault, size.toULong(), it.addressOf(0))
        if (result != errSecSuccess) {
            error("SecRandomCopyBytes failed with code: $result")
        }
    }
    return key
}

@OptIn(ExperimentalForeignApi::class)
private fun storeAesKeyInKeychain(tag: String, key: ByteArray) = memScoped {
    // Delete the existing key if it exists
    val deleteQuery = mapOf(
        kSecClass to kSecClassGenericPassword,
        kSecAttrService to "key",
        kSecAttrAccount to tag
    ).toCFDictionary()
    SecItemDelete(deleteQuery)

    val addQuery = mapOf(
        kSecClass to kSecClassGenericPassword,
        kSecAttrService to "key",
        kSecAttrAccount to tag,
        kSecValueData to key,
        kSecAttrAccessible to kSecAttrAccessibleWhenUnlocked
    ).toCFDictionary()
    val status = SecItemAdd(addQuery, null)
    if (status != errSecSuccess) {
        error("SecItemAdd failed, status: $status")
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun loadAesKeyFromKeychain(tag: String): ByteArray? = memScoped {
    val query = mapOf(
        kSecClass to kSecClassGenericPassword,
        kSecAttrService to "key",
        kSecAttrAccount to tag,
        kSecReturnData to kCFBooleanTrue
    ).toCFDictionary()

    val resultPtr = alloc<CFTypeRefVar>()
    when (val status = SecItemCopyMatching(query, resultPtr.ptr)) {
        errSecSuccess -> {
            val value = resultPtr.value ?: return null
            val cfData: CFDataRef = if (CFGetTypeID(value) == CFDataGetTypeID()) {
                value.reinterpret()
            } else {
                return null
            }
            cfData.reinterpret<ByteVar>().readBytes(CFDataGetLength(cfData).toInt())
        }
        errSecItemNotFound -> {
            null
        }
        else -> {
            error("SecItemCopyMatching failed, status: $status")
        }
    }
}


@OptIn(ExperimentalForeignApi::class)
private fun Map<CFStringRef?, Any?>.toCFDictionary(): CFDictionaryRef? = memScoped {
    val dictionary: CFMutableDictionaryRef? = CFDictionaryCreateMutable(
        allocator = kCFAllocatorDefault,
        capacity = this@toCFDictionary.size.toLong(),
        keyCallBacks = kCFTypeDictionaryKeyCallBacks.readValue(),
        valueCallBacks = kCFTypeDictionaryValueCallBacks.readValue()
    )

    for ((keyCFString, value) in this@toCFDictionary) {
        val cfValue = value?.toCFTypeRef()

        if (dictionary != null && keyCFString != null && cfValue != null) {
            CFDictionarySetValue(dictionary, keyCFString, cfValue)
        }
    }

    dictionary
}

@OptIn(ExperimentalForeignApi::class)
private fun Any.toCFTypeRef(): CFTypeRef? = when (this) {
    is CFTypeRef -> this
    is String -> CFStringCreateWithCString(
        kCFAllocatorDefault,
        this,
        kCFStringEncodingUTF8
    )
    is ByteArray -> usePinned { pinned ->
        CFDataCreate(
            allocator = kCFAllocatorDefault,
            bytes = pinned.addressOf(0).reinterpret(),
            length = this.size.toLong()
        )
    }
    else -> {
        throw IllegalArgumentException("Unsupported type: $this")
    }
}
