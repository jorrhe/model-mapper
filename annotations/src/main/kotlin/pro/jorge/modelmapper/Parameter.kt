package pro.jorge.modelmapper

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class Parameter(val source:String,val target:String)
