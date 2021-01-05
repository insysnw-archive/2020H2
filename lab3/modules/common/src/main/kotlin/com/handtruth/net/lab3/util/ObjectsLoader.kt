package com.handtruth.net.lab3.util

import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * Загружает объекты указанного класса в стиле [java.util.ServiceLoader],
 * но при этом не инстанцирует их самостоятельно, а пытается извлечь объекты Kotlin.
 * Использует класслоадер переданного класса [class].
 *
 * @param[class] класс сервиса, объекты которого следует загрузить
 * @return список загруженных объектов сервиса
 */
fun <T : Any> loadObjects(`class`: KClass<out T>): List<T> = loadObjects(`class`, `class`.java.classLoader)

/**
 * Загружает объекты указанного класса в стиле [java.util.ServiceLoader],
 * но при этом не инстанцирует их самостоятельно, а пытается извлечь объекты Kotlin.
 *
 * @param[class] класс сервиса, объекты которого следует загрузить
 * @param classLoader при загрузке классов сервиса использовать этот класслоадер
 * @return список загруженных объектов сервиса
 */
fun <T : Any> loadObjects(`class`: KClass<out T>, classLoader: ClassLoader): List<T> {
    val resources = classLoader.getResources("META-INF/services/${`class`.java.canonicalName}")!!
    return resources.asSequence().flatMap { resource ->
        resource.openStream().bufferedReader().readLines().map {
            val objectClass = classLoader.loadClass(it).kotlin
            assert(objectClass.isSubclassOf(`class`))
            @Suppress("UNCHECKED_CAST")
            objectClass.objectInstance as T
        }
    }.toList()
}

/**
 * Загружает объекты указанного класса в стиле [java.util.ServiceLoader],
 * но при этом не инстанцирует их самостоятельно, а пытается извлечь объекты Kotlin.
 *
 * @param[T] тип сервиса, объекты которого следует загрузить
 * @param classLoader при загрузке классов сервиса использовать этот класслоадер
 * @return список загруженных объектов сервиса
 */
inline fun <reified T : Any> loadObjects(classLoader: ClassLoader): List<T> = loadObjects(T::class, classLoader)

/**
 * Загружает объекты указанного класса в стиле [java.util.ServiceLoader],
 * но при этом не инстанцирует их самостоятельно, а пытается извлечь объекты Kotlin.
 * Использует класслоадер класса переданного типа [T].
 *
 * @param[T] тип сервиса, объекты которого следует загрузить
 * @return список загруженных объектов сервиса
 */
inline fun <reified T : Any> loadObjects(): List<T> = loadObjects(T::class)
