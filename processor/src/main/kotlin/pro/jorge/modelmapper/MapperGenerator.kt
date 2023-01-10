package pro.jorge.modelmapper

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import pro.jorge.modelmapper.ksp.MapperError
import pro.jorge.modelmapper.ksp.MapperError.InvalidParametersError
import pro.jorge.modelmapper.ksp.MapperError.NoPrimaryConstructorError
import pro.jorge.modelmapper.ksp.MapperException
import pro.jorge.modelmapper.ksp.error

class MapperGenerator(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) {


    var strictMode: Boolean = false
    var manualParameters: List<ParameterData> = emptyList()
    var resolverProcessor: Resolver? = null

    private var fromClassCanonicalName = ""
    private var toClassCanonicalName = ""

    private val generatedMappers: MutableList<ClassName> = mutableListOf()

    fun generate(modelFrom: KSType, modelTo: KSType) {

        if (generatedMappers.contains(modelFrom.toClassName())) {
            return
        }

        val modelFromClassName = modelFrom.toClassName().simpleName

        val packageName = modelFrom.toClassName().packageName

        try{

            fromClassCanonicalName = modelFrom.toClassName().canonicalName
            toClassCanonicalName = modelTo.toClassName().canonicalName

            val fileSpec = FileSpec.builder(
                packageName = packageName, fileName = "${modelFromClassName}Ext"
            ).apply {
                addMapperExtensionFunction(
                    modelFrom = modelFrom,
                    modelTo = modelTo
                )
            }.build()

            fileSpec.writeTo(
                codeGenerator = codeGenerator,
                aggregating = false
            )

            generatedMappers.add(modelFrom.toClassName())
        }catch (e: MapperException){
            logger.error(e.message ?: "")
        }


    }

    private fun FileSpec.Builder.addMapperExtensionFunction(modelFrom: KSType, modelTo: KSType) {

        val functionName = getFunctionName(modelTo)

        val fromClass = modelFrom.toTypeName()
        val toClass = modelTo.toTypeName()

        val fromConstructor = (modelFrom.declaration as KSClassDeclaration).primaryConstructor
        val toConstructor = (modelTo.declaration as KSClassDeclaration).primaryConstructor

        if (fromConstructor == null) {
            logger.error(NoPrimaryConstructorError(fromClassCanonicalName)); return
        }

        if (toConstructor == null) {
            logger.error(NoPrimaryConstructorError(toClassCanonicalName)); return
        }

        if (fromConstructor.invalidArguments(toConstructor)) {
            logger.error(
                InvalidParametersError(
                    fromClassCanonicalName,
                    toClassCanonicalName
                )
            ); return
        }

        val extFunction = FunSpec.builder(functionName)
            .receiver(fromClass)
            .addMapper(
                toParameters = toConstructor.parameters,
                fromParameters = fromConstructor.parameters,
                toClassName = modelTo.toClassName()
            )
            .returns(toClass)
            .build()

        addFunction(extFunction)
    }

    private fun KSFunctionDeclaration.invalidArguments(toConstructor: KSFunctionDeclaration): Boolean {
        val fromContructor = this
        val fromParameters = parameters
        val toParameters = toConstructor.parameters
        return fromParameters.any { p1 ->
            !toConstructor.anySameNameAndTypeToConstructor(p1)
        } ||
                toParameters.any { p1 ->
                    !fromContructor.anySameNameFromConstructor(p1) &&
                            !p1.hasDefault &&
                            !p1.type.resolve().isMarkedNullable
                }
    }

    private fun FunSpec.Builder.addMapper(
        fromParameters: List<KSValueParameter>,
        toParameters: List<KSValueParameter>,
        toClassName: ClassName
    ): FunSpec.Builder {

        val parameters: MutableList<String> = mutableListOf()

        fromParameters.forEach { param ->
            param.name?.let { name ->

                val type = param.type.resolve()
                val isBuiltIn = resolverProcessor?.builtIns?.toList()?.contains(type)
                val isIterable =  resolverProcessor?.builtIns?.iterableType?.isAssignableFrom(param.type.resolve())
                if (isBuiltIn == true && isIterable == false){
                    if (toParameters.any { it.name == name }) {
                        parameters.add("${name.asString()}=${name.asString()}")
                    } else {
                        val manualParameter =
                            manualParameters.firstOrNull { it.source == name.asString() }
                        if (manualParameter != null) {
                            parameters.add("${manualParameter.target}=${manualParameter.source}")
                        }
                    }
                }else if(isIterable == false){

                    var nameLeft = name.asString()

                    val newType = if(toParameters.any { it.name == name }){
                        toParameters.first { it.name == name }.type.resolve()
                    }else{
                        val manual = manualParameters.first { it.source == name.asString() }
                        nameLeft = manual.target
                        toParameters.first { it.name?.asString() == manual.target }.type.resolve()
                    }

                    val prevManualParameters = manualParameters.toList()
                    manualParameters = emptyList()

                    generate(modelTo = newType, modelFrom = type)
                    parameters.add("$nameLeft=${name.asString()}.to${getFunctionName(newType)}()")
                    manualParameters = prevManualParameters

                }else{

                    var nameLeft = name.asString()

                    val newType = if(toParameters.any { it.name == name }){
                        toParameters.first { it.name == name }.type.resolve().arguments.firstOrNull()?.type?.resolve()
                    }else{
                        val manual = manualParameters.first { it.source == name.asString() }
                        nameLeft = manual.target
                        toParameters.first { it.name?.asString() == manual.target }.type.resolve().arguments.firstOrNull()?.type?.resolve()
                    }

                    val fromArgument = type.arguments.firstOrNull()?.type?.resolve()

                    if(newType != null && fromArgument != null){
                        val prevManualParameters = manualParameters.toList()
                        manualParameters = emptyList()
                        generate(modelTo = newType, modelFrom = fromArgument)
                        parameters.add("$nameLeft=${name.asString()}.map{ it.${getFunctionName(newType)}() }")
                        manualParameters = prevManualParameters
                    }

                }

            }
        }

        // We check if toModel has any nullable param
        if (parameters.size < toParameters.size) {
            toParameters.filter { p1 ->
                !fromParameters.any { p2 -> p1.name == p2.name } &&
                        p1.type.resolve().isMarkedNullable
            }.forEach { param ->
                param.name?.let {
                    parameters.add("${it.asString()}=null")
                }
            }
        }

        addStatement("return %T(${parameters.joinToString()})", toClassName)

        return this
    }

    private fun KSFunctionDeclaration.anySameNameAndTypeToConstructor(
        fromParameter: KSValueParameter
    ): Boolean{

        val manualParameterTarget =
            manualParameters.firstOrNull { it.source == fromParameter.name?.asString() }?.target
                ?: ""

        val fromParameterType = fromParameter.type.resolve()

        var anySameName = false

        parameters.forEach { toParameter ->

            if(fromParameter.name?.asString() == toParameter.name?.asString() &&
                manualParameterTarget == toParameter.name?.asString()){

                anySameName = fromParameterType == toParameter.type.resolve() &&
                        fromParameterType.sameArguments(toParameter.type.resolve())

                if(!anySameName){
                    throw MapperException(
                        MapperError.InvalidTypeError(
                            fromParameter.name?.asString() ?: "",
                            fromClassCanonicalName,
                            toParameter.name?.asString() ?: "",
                            toClassCanonicalName
                        )
                    )
                }

            }else if(strictMode){
                anySameName = false
            }

        }

        return anySameName || !strictMode
    }

    private fun KSType.sameArguments(type: KSType): Boolean {
        return (arguments.isEmpty() && type.arguments.isEmpty()) ||
                arguments.any { arg -> type.arguments.any { arg.type?.resolve() == it.type?.resolve() } }
    }

    private fun KSFunctionDeclaration.anySameNameFromConstructor(toParameter: KSValueParameter): Boolean {
        val manualParameterSource =
            manualParameters.firstOrNull { it.target == toParameter.name?.asString() }?.source ?: ""
        return parameters.any { fromParameter ->
            toParameter.name?.asString() == fromParameter.name?.asString() ||
                    manualParameterSource == fromParameter.name?.asString()
        }
    }

    private fun getFunctionName(type: KSType) = "to${type.toClassName().simpleName}"

}
