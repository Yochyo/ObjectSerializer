package de.yochyo.objectserializer.annotations

/**
 * Fields with this annotation will not be parsed to or from a JsonObject
 */

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Ignore