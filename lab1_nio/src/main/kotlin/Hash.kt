import java.security.MessageDigest

fun String.md5(): ByteArray {
    return hashString(this, "MD5")
}

fun String.sha256(): ByteArray {
    return hashString(this, "SHA-256")
}

private fun hashString(input: String, algorithm: String): ByteArray {
    return MessageDigest
        .getInstance(algorithm)
        .digest(input.toByteArray())
}