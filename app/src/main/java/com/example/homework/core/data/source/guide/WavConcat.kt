package com.example.homework.core.data.source.guide

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal object WavConcat {
    fun concatenate(parts: List<ByteArray>): ByteArray {
        if (parts.isEmpty()) return ByteArray(0)
        if (parts.size == 1) return parts.first()
        val parsed = parts.map(::parse)
        val first = parsed.first()
        require(parsed.all { it.channels == first.channels && it.rate == first.rate && it.bits == first.bits }) {
            "WAV fragments have different formats"
        }
        val pcm = ByteArrayOutputStream()
        parsed.forEach { pcm.write(it.pcm) }
        return write(first.channels, first.rate, first.bits, pcm.toByteArray())
    }

    private fun parse(bytes: ByteArray): WavPcm {
        require(bytes.size >= 44 && bytes.copyOfRange(0, 4).decodeToString() == "RIFF") {
            "Not a WAV file"
        }
        var offset = 12
        var channels = 1
        var rate = 16_000
        var bits = 16
        var pcm = ByteArray(0)
        while (offset + 8 <= bytes.size) {
            val id = bytes.copyOfRange(offset, offset + 4).decodeToString()
            val size = ByteBuffer.wrap(bytes, offset + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
            val dataStart = offset + 8
            val dataEnd = (dataStart + size).coerceAtMost(bytes.size)
            when (id) {
                "fmt " -> {
                    val fmt = ByteBuffer.wrap(bytes, dataStart, size).order(ByteOrder.LITTLE_ENDIAN)
                    fmt.short
                    channels = fmt.short.toInt()
                    rate = fmt.int
                    fmt.int
                    fmt.short
                    bits = fmt.short.toInt()
                }
                "data" -> pcm = bytes.copyOfRange(dataStart, dataEnd)
            }
            offset = dataEnd + size % 2
        }
        return WavPcm(channels, rate, bits, pcm)
    }

    private fun write(channels: Int, rate: Int, bits: Int, pcm: ByteArray): ByteArray {
        val byteRate = rate * channels * bits / 8
        val blockAlign = channels * bits / 8
        val out = ByteArrayOutputStream()
        fun ascii(value: String) = out.write(value.toByteArray(Charsets.US_ASCII))
        fun intLe(value: Int) {
            out.write(byteArrayOf(
                (value and 0xFF).toByte(),
                (value shr 8 and 0xFF).toByte(),
                (value shr 16 and 0xFF).toByte(),
                (value shr 24 and 0xFF).toByte(),
            ))
        }
        fun shortLe(value: Int) {
            out.write(byteArrayOf((value and 0xFF).toByte(), (value shr 8 and 0xFF).toByte()))
        }
        ascii("RIFF")
        intLe(36 + pcm.size)
        ascii("WAVE")
        ascii("fmt ")
        intLe(16)
        shortLe(1)
        shortLe(channels)
        intLe(rate)
        intLe(byteRate)
        shortLe(blockAlign)
        shortLe(bits)
        ascii("data")
        intLe(pcm.size)
        out.write(pcm)
        return out.toByteArray()
    }

    private data class WavPcm(
        val channels: Int,
        val rate: Int,
        val bits: Int,
        val pcm: ByteArray,
    )
}
