package com.quizifyai.utils

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.quizifyai.domain.model.QuizifyException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset
import java.util.zip.InflaterInputStream

class SimplePdfTextExtractor(private val context: Context) {
    suspend fun extract(uri: Uri): String = withContext(Dispatchers.IO) {
        val tempFile = copyToCache(uri)
        try {
            validatePdf(tempFile)
            val bytes = tempFile.inputStream().use { it.readAtMost(MAX_PDF_BYTES) }
            val text = extractText(bytes)
                .replace(Regex("\\s+"), " ")
                .trim()
            text.take(MAX_TEXT_CHARS)
        } finally {
            tempFile.delete()
        }
    }

    private fun copyToCache(uri: Uri): File {
        val tempFile = File.createTempFile("quizify_pdf_", ".pdf", context.cacheDir)
        val input = context.contentResolver.openInputStream(uri) ?: throw QuizifyException.InvalidPdf()
        input.use { source ->
            tempFile.outputStream().use { destination ->
                source.copyTo(destination)
            }
        }
        return tempFile
    }

    private fun validatePdf(file: File) {
        if (file.length() < PDF_HEADER.length) throw QuizifyException.InvalidPdf()

        val headerBytes = file.inputStream().use { input ->
            ByteArray(PDF_HEADER.length).also { input.read(it) }
        }
        val header = String(headerBytes, Charsets.US_ASCII)

        if (header != PDF_HEADER) throw QuizifyException.InvalidPdf()

        runCatching {
            val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            descriptor.use {
                PdfRenderer(it).use { renderer ->
                    if (renderer.pageCount <= 0) throw QuizifyException.InvalidPdf()
                }
            }
        }.getOrElse { throw QuizifyException.InvalidPdf() }
    }

    private fun extractText(bytes: ByteArray): String {
        val raw = String(bytes, Charsets.ISO_8859_1)
        val chunks = mutableListOf<String>()
        chunks += extractFromTextBlocks(raw)
        extractFlateStreams(bytes, raw).forEach { streamBytes ->
            chunks += extractFromTextBlocks(String(streamBytes, Charsets.ISO_8859_1))
        }
        return chunks.joinToString(" ")
    }

    private fun extractFromTextBlocks(raw: String): List<String> {
        return textBlockRegex.findAll(raw)
            .flatMap { block ->
                val value = block.groupValues[1]
                literalStringRegex.findAll(value).map { match ->
                    decodeLiteralString(match.value.drop(1).dropLast(1))
                } + hexStringRegex.findAll(value).mapNotNull { match ->
                    decodeHexString(match.groupValues[1])
                }
            }
            .filter { it.isNotBlank() }
            .toList()
    }

    private fun extractFlateStreams(bytes: ByteArray, raw: String): List<ByteArray> {
        val output = mutableListOf<ByteArray>()
        var searchFrom = 0

        while (true) {
            val streamIndex = raw.indexOf("stream", searchFrom)
            if (streamIndex == -1) break

            val dictionaryStart = raw.lastIndexOf("<<", streamIndex)
            val dictionary = if (dictionaryStart >= 0) {
                raw.substring(dictionaryStart, streamIndex)
            } else {
                ""
            }

            val dataStart = skipStreamLineEnding(raw, streamIndex + "stream".length)
            val dataEnd = raw.indexOf("endstream", dataStart)
            if (dataEnd == -1) break

            if (dictionary.contains("/FlateDecode")) {
                val streamBytes = bytes.copyOfRange(dataStart, dataEnd)
                runCatching {
                    InflaterInputStream(ByteArrayInputStream(streamBytes)).use { it.readBytes() }
                }.onSuccess { output += it }
            }

            searchFrom = dataEnd + "endstream".length
        }

        return output
    }

    private fun skipStreamLineEnding(raw: String, index: Int): Int {
        return when {
            raw.getOrNull(index) == '\r' && raw.getOrNull(index + 1) == '\n' -> index + 2
            raw.getOrNull(index) == '\n' || raw.getOrNull(index) == '\r' -> index + 1
            else -> index
        }
    }

    private fun decodeLiteralString(value: String): String {
        val builder = StringBuilder()
        var index = 0

        while (index < value.length) {
            val char = value[index]
            if (char != '\\') {
                builder.append(char)
                index++
                continue
            }

            val next = value.getOrNull(index + 1) ?: break
            when (next) {
                'n' -> builder.append('\n')
                'r' -> builder.append('\r')
                't' -> builder.append('\t')
                'b' -> builder.append('\b')
                'f' -> builder.append('\u000C')
                '(', ')', '\\' -> builder.append(next)
                '\n', '\r' -> Unit
                in '0'..'7' -> {
                    val octal = value.substring(index + 1, minOf(index + 4, value.length))
                        .takeWhile { it in '0'..'7' }
                    builder.append(octal.toInt(8).toChar())
                    index += 1 + octal.length
                    continue
                }
                else -> builder.append(next)
            }
            index += 2
        }

        return builder.toString()
    }

    private fun decodeHexString(hex: String): String? {
        val clean = hex.filterNot { it.isWhitespace() }
        if (clean.length < 2 || clean.length % 2 != 0) return null

        val bytes = clean.chunked(2)
            .mapNotNull { it.toIntOrNull(16)?.toByte() }
            .toByteArray()

        if (bytes.isEmpty()) return null
        val charset = if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            Charset.forName("UTF-16BE")
        } else {
            Charsets.ISO_8859_1
        }
        return String(bytes, charset)
    }

    private fun InputStream.readAtMost(maxBytes: Int): ByteArray {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        val output = ByteArrayOutputStream()
        var total = 0

        while (total < maxBytes) {
            val toRead = minOf(buffer.size, maxBytes - total)
            val read = read(buffer, 0, toRead)
            if (read == -1) break
            output.write(buffer, 0, read)
            total += read
        }

        return output.toByteArray()
    }

    private companion object {
        const val PDF_HEADER = "%PDF-"
        const val MAX_PDF_BYTES = 6 * 1024 * 1024
        const val MAX_TEXT_CHARS = 18_000
        val textBlockRegex = Regex("BT(.*?)ET", setOf(RegexOption.DOT_MATCHES_ALL))
        val literalStringRegex = Regex("\\((?:\\\\.|[^\\\\()])*\\)")
        val hexStringRegex = Regex("<([0-9A-Fa-f\\s]+)>")
    }
}
