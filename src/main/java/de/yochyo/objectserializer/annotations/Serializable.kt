package de.yochyo.objectserializer.annotations

/**
 * Field with this field will be parsed as  ByteArray, should be used for lambdas/runnables/...
 */

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Serializable