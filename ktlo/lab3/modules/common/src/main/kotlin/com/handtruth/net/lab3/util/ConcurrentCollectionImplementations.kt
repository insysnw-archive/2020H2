package com.handtruth.net.lab3.util

import kotlinx.coroutines.sync.Mutex
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

fun <K, V> concurrentHashMap(
    initialCapacity: Int = 0,
    loadFactor: Float = 0.75f,
    mutex: Mutex = Mutex()
): ConcurrentMap<K, V> = ConcurrentMap.wrap(ConcurrentHashMap(initialCapacity, loadFactor, 1), mutex)

fun <E> copyOnWriteArrayList(
    mutex: Mutex = Mutex()
): ConcurrentList<E> = ConcurrentList.wrap(CopyOnWriteArrayList(), mutex)

fun <E> concurrentHashSet(
    initialCapacity: Int = 0,
    loadFactor: Float = 0.75f,
    mutex: Mutex = Mutex()
): ConcurrentSet<E> = ConcurrentSet.wrap(
    Collections.newSetFromMap(ConcurrentHashMap(initialCapacity, loadFactor, 1)), mutex
)

// Реализации по умолчанию

fun <E> ConcurrentList(mutex: Mutex = Mutex()) = copyOnWriteArrayList<E>(mutex = mutex)

fun <E> ConcurrentSet(mutex: Mutex = Mutex()) = concurrentHashSet<E>(mutex = mutex)

fun <K, V> ConcurrentMap(mutex: Mutex = Mutex()) = concurrentHashMap<K, V>(mutex = mutex)
