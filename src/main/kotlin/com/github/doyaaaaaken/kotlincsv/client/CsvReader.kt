package com.github.doyaaaaaken.kotlincsv.client

import com.github.doyaaaaaken.kotlincsv.dsl.context.CsvReaderContext
import com.github.doyaaaaaken.kotlincsv.dsl.context.ICsvReaderContext
import com.github.doyaaaaaken.kotlincsv.parser.CsvParser
import com.github.doyaaaaaken.kotlincsv.util.MalformedCSVException
import java.io.BufferedReader
import java.io.File
import java.io.InputStream

/**
 * CSV Reader class, which decides where to read from and how to read.
 *
 * @author doyaaaaaken
 */
class CsvReader(ctx: CsvReaderContext = CsvReaderContext()) : ICsvReaderContext by ctx {

    private val parser = CsvParser()

    private val lineSeparator = System.lineSeparator()

    fun read(data: String): List<List<String>> {
        return readAsSequence(data).toList()
    }

    fun read(file: File): List<List<String>> {
        return readAsSequence(file).toList()
    }

    fun read(ips: InputStream): List<List<String>> {
        return readAsSequence(ips).toList()
    }

    fun readWithHeader(data: String): List<Map<String, String>> {
        val br = data.byteInputStream(charset).bufferedReader(charset)
        return readWithHeader(br)
    }

    fun readWithHeader(file: File): List<Map<String, String>> {
        val br = file.inputStream().bufferedReader(charset)
        return readWithHeader(br)
    }

    fun readWithHeader(ips: InputStream): List<Map<String, String>> {
        val br = ips.bufferedReader(charset)
        return readWithHeader(br)
    }

    fun readAsSequence(data: String): Sequence<List<String>> {
        val br = data.byteInputStream(charset).bufferedReader(charset)
        return readWithBufferedReader(br)
    }

    fun readAsSequence(file: File): Sequence<List<String>> {
        val br = file.inputStream().bufferedReader(charset)
        return readWithBufferedReader(br)
    }

    fun readAsSequence(ips: InputStream): Sequence<List<String>> {
        val br = ips.bufferedReader(charset)
        return readWithBufferedReader(br)
    }

    private fun readWithBufferedReader(br: BufferedReader): Sequence<List<String>> {
        return generateSequence { readNext(br) }
    }

    private fun readWithHeader(br: BufferedReader): List<Map<String, String>> {
        val headers = readNext(br)
        val duplicated = headers?.let(::findDuplicate)
        if (duplicated != null) throw MalformedCSVException("header '$duplicated' is duplicated")

        return readWithBufferedReader(br).map { fields ->
            if (requireNotNull(headers).size != fields.size) {
                throw MalformedCSVException("fields num  ${fields.size} is not matched with header num ${headers.size}")
            }
            headers.zip(fields).toMap()
        }.toList()
    }

    private tailrec fun readNext(br: BufferedReader, leftOver: String = ""): List<String>? {
        val nextLine = br.readLine()
        return if (nextLine == null) {
            if (leftOver.isNotEmpty()) {
                throw MalformedCSVException("Malformed format: leftOver \"$leftOver\" on the tail of file")
            } else {
                null
            }
        } else {
            val value = if (leftOver.isEmpty()) {
                "$nextLine$lineSeparator"
            } else {
                "$leftOver$lineSeparator$nextLine$lineSeparator"
            }
            val parsedLine = parser.parseRow(value, quoteChar, delimiter, escapeChar)
            if (parsedLine == null) {
                readNext(br, "$leftOver$nextLine")
            } else {
                parsedLine
            }
        }
    }

    private fun findDuplicate(headers: List<String>): String? {
        val set = mutableSetOf<String>()
        headers.forEach { h ->
            if (set.contains(h)) {
                return h
            } else {
                set.add(h)
            }
        }
        return null
    }
}
