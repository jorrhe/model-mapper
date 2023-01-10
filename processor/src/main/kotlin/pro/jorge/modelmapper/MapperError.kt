package pro.jorge.modelmapper.ksp

import com.google.devtools.ksp.processing.KSPLogger

sealed class MapperError(val error:String){
    class NoPrimaryConstructorError(vararg replace:String) : MapperError("No primary constructor for class <%s>.".format(*replace))
    class InvalidParametersError(vararg replace:String) : MapperError("Invalid parameter: the class <%s> doesn't have the same number of parameters as class <%s> or arguments that are named the same.".format(*replace))
    class InvalidTypeError(vararg replace:String) : MapperError("Invalid parameter type: %s from class <%s> doesn't have the same type as parameter %s of class <%s>.".format(*replace))
}

class MapperException(error:MapperError) : Exception(error.error)

fun KSPLogger.error(mapperError:MapperError) = error(mapperError.error)