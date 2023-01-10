package pro.jorge.modelmapper

import com.google.devtools.ksp.processing.KSBuiltIns
import com.google.devtools.ksp.symbol.KSType

fun KSBuiltIns.toList():List<KSType> = listOf(
    annotationType,
    anyType,
    arrayType,
    booleanType,
    byteType,
    charType,
    doubleType,
    floatType,
    intType,
    iterableType,
    longType,
    nothingType,
    numberType,
    shortType,
    stringType,
    unitType
)