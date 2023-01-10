package pro.jorge.modelmapper

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class MapperFromClass(val classFrom: KClass<*>, val strict:Boolean = false)
