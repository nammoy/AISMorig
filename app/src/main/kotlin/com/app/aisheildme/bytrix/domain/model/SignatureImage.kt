package com.app.aisheildme.bytrix.domain.model

class SignatureImage(val png: ByteArray) {
    val isEmpty: Boolean get() = png.isEmpty()

    override fun equals(other: Any?): Boolean =
        this === other || (other is SignatureImage && png.contentEquals(other.png))

    override fun hashCode(): Int = png.contentHashCode()
}
