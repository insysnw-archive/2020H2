package com.handtruth.net.lab3.types

import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Декодирует время.
 * @receiver синхронный поток данных, откуда следует считать время
 * @return декодированное время
 */
fun Input.readTime(): Long = readLong()

/**
 * Декодирует время.
 * @receiver синхронный поток данных, откуда следует считать время
 * @return декодированное время
 */
fun Input.readTimeInstant(): Instant = Instant.fromEpochSeconds(readLong())

/**
 * Декодирует время.
 * @receiver асинхронный поток данных, откуда следует считать время
 * @return декодированное время
 */
suspend fun ByteReadChannel.readTime(): Long = readLong()


/**
 * Кодирует время.
 * @receiver синхронный поток, куда следует записать закодированную последовательность
 * @param time Unix-timestamp
 */
fun Output.writeTime(time: Long) = writeLong(time)

/**
 * Кодирует время.
 * @receiver синхронный поток, куда следует записать закодированную последовательность
 * @param time Unix-timestamp
 */
fun Output.writeTime(time: Instant) = writeLong(time.epochSeconds)

/**
 * Кодирует текущее время.
 * @receiver синхронный поток, куда следует записать закодированную последовательность
 */
fun Output.writeCurrentTime() = writeLong(Clock.System.now().epochSeconds)

/**
 * Кодирует время.
 * @receiver асинхронный поток, куда следует записать закодированную последовательность
 * @param time Unix-timestamp
 */
suspend fun ByteWriteChannel.writeTime(time: Long) = writeLong(time)

/**
 * Кодирует текущее время.
 * @receiver асинхронный поток, куда следует записать закодированную последовательность
 */
suspend fun ByteWriteChannel.writeCurrentTime() = writeLong(Clock.System.now().epochSeconds)