package pro.jorge.modelmapper.sample

import pro.jorge.modelmapper.MapperFromClass
import pro.jorge.modelmapper.Parameter


data class PersonA(
    val name: String,
    val age: Int,
    val countryName: String
)

@MapperFromClass(
    classFrom = PersonA::class
)
@Parameter(
    source = "countryName",
    target = "country"
)
data class PersonB(
    val name: String,
    val age: Int,
    val country: String
)

fun main() {
    println(PersonA("Kevin",12,"UK").toPersonB().age)
}