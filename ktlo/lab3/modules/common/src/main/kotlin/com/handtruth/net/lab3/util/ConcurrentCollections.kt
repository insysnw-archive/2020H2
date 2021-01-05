package com.handtruth.net.lab3.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface ConcurrentCollection<E> : Collection<E> {
    fun <R> modifyCollectionUnsafe(block: (MutableCollection<E>) -> R): R
    suspend fun <R> modifyCollection(block: (MutableCollection<E>) -> R): R
    suspend fun add(element: E): Boolean = modifyCollection { it.add(element) }
    suspend fun remove(element: E): Boolean = modifyCollection { it.remove(element) }
    suspend fun addAll(elements: Collection<E>): Boolean = modifyCollection { it.addAll(elements) }
    suspend fun removeAll(elements: Collection<E>): Boolean = modifyCollection { it.removeAll(elements) }
    suspend fun retainAll(elements: Collection<E>): Boolean = modifyCollection { it.retainAll(elements) }
    suspend fun clear(): Unit = modifyCollection { it.clear() }

    companion object {
        fun <E> wrap(collection: MutableCollection<E>, mutex: Mutex = Mutex()): ConcurrentCollection<E> =
            ConcurrentCollectionWrapper(collection, mutex)
    }
}

private class ConcurrentCollectionWrapper<E>(
    private val collection: MutableCollection<E>,
    private val mutex: Mutex
) : ConcurrentCollection<E>, Collection<E> by collection {

    override fun <R> modifyCollectionUnsafe(block: (MutableCollection<E>) -> R): R = collection.let(block)

    override suspend fun <R> modifyCollection(block: (MutableCollection<E>) -> R): R = mutex.withLock {
        collection.let(block)
    }

    override fun hashCode(): Int = collection.hashCode()
    override fun equals(other: Any?): Boolean = collection == other
    override fun toString() = collection.toString()
}

interface ConcurrentList<E> : List<E>, ConcurrentCollection<E> {
    fun <R> modifyListUnsafe(block: (MutableList<E>) -> R): R
    override fun <R> modifyCollectionUnsafe(block: (MutableCollection<E>) -> R): R = modifyListUnsafe(block)
    suspend fun <R> modifyList(block: (MutableList<E>) -> R): R
    override suspend fun <R> modifyCollection(block: (MutableCollection<E>) -> R): R = modifyList(block)
    suspend fun addAll(index: Int, elements: Collection<E>): Boolean = modifyList { it.addAll(index, elements) }
    suspend fun set(index: Int, element: E): E = modifyList { it.set(index, element) }
    suspend fun add(index: Int, element: E): Unit = modifyList { it.add(index, element) }
    suspend fun removeAt(index: Int): E = modifyList { it.removeAt(index) }

    companion object {
        fun <E> wrap(list: MutableList<E>, mutex: Mutex = Mutex()): ConcurrentList<E> =
            ConcurrentListWrapper(list, mutex)
    }
}

private class ConcurrentListWrapper<E>(
    private val list: MutableList<E>,
    private val mutex: Mutex
) : ConcurrentList<E>, List<E> by list {

    override fun <R> modifyListUnsafe(block: (MutableList<E>) -> R): R = list.let(block)

    override suspend fun <R> modifyList(block: (MutableList<E>) -> R): R = mutex.withLock { modifyListUnsafe(block) }

    override fun hashCode(): Int = list.hashCode()
    override fun equals(other: Any?): Boolean = list == other
    override fun toString() = list.toString()
}

interface ConcurrentSet<E> : Set<E>, ConcurrentCollection<E> {
    fun <R> modifySetUnsafe(block: (MutableSet<E>) -> R): R
    override fun <R> modifyCollectionUnsafe(block: (MutableCollection<E>) -> R): R = modifySetUnsafe(block)
    suspend fun <R> modifySet(block: (MutableSet<E>) -> R): R
    override suspend fun <R> modifyCollection(block: (MutableCollection<E>) -> R): R = modifySet(block)

    companion object {
        fun <E> wrap(set: MutableSet<E>, mutex: Mutex = Mutex()): ConcurrentSet<E> =
            ConcurrentSetWrapper(set, mutex)
    }
}

private class ConcurrentSetWrapper<E>(
    private val set: MutableSet<E>,
    private val mutex: Mutex
) : ConcurrentSet<E>, Set<E> by set {
    override fun <R> modifySetUnsafe(block: (MutableSet<E>) -> R): R = set.let(block)

    override suspend fun <R> modifySet(block: (MutableSet<E>) -> R): R = mutex.withLock { modifySetUnsafe(block) }

    override fun hashCode(): Int = set.hashCode()
    override fun equals(other: Any?): Boolean = set == other
    override fun toString() = set.toString()
}

interface ConcurrentMap<K, V> : Map<K, V> {
    fun <R> modifyMapUnsafe(block: (MutableMap<K, V>) -> R): R
    suspend fun <R> modifyMap(block: (MutableMap<K, V>) -> R): R
    suspend fun put(key: K, value: V): V? = modifyMap { it.put(key, value) }
    suspend fun remove(key: K): V? = modifyMap { it.remove(key) }
    suspend fun remove(key: K, value: V): Boolean = modifyMap { it.remove(key, value) }
    suspend fun putAll(from: Map<out K, V>): Unit = modifyMap { it.putAll(from) }
    suspend fun clear(): Unit = modifyMap { it.clear() }

    override val entries: ConcurrentSet<Map.Entry<K, V>>
    override val keys: ConcurrentSet<K>
    override val values: ConcurrentCollection<V>

    companion object {
        fun <K, V> wrap(map: MutableMap<K, V>, mutex: Mutex = Mutex()): ConcurrentMap<K, V> =
            ConcurrentMapWrapper(map, mutex)
    }
}

private class ConcurrentMapWrapper<K, V>(
    private val map: MutableMap<K, V>,
    private val mutex: Mutex
) : ConcurrentMap<K, V>, Map<K, V> by map {
    override fun <R> modifyMapUnsafe(block: (MutableMap<K, V>) -> R): R = map.let(block)

    override suspend fun <R> modifyMap(block: (MutableMap<K, V>) -> R): R = mutex.withLock { modifyMapUnsafe(block) }

    // Вообще это очень не правильно, но давайте притворимся, что этого тут нет
    @Suppress("UNCHECKED_CAST")
    override val entries = ConcurrentSet.wrap(map.entries as MutableSet<Map.Entry<K, V>>, mutex)

    override val keys = ConcurrentSet.wrap(map.keys, mutex)

    override val values = ConcurrentCollection.wrap(map.values, mutex)

    override fun hashCode(): Int = map.hashCode()
    override fun equals(other: Any?): Boolean = map == other
    override fun toString() = map.toString()
}
