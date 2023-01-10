package pro.jorge.modelmapper.mapperfrom.visitor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSVisitorVoid
import pro.jorge.modelmapper.MapperFromClass
import pro.jorge.modelmapper.MapperGenerator
import pro.jorge.modelmapper.Parameter
import pro.jorge.modelmapper.ParameterData

class MapperFromVisitor(
    codeGenerator: CodeGenerator,
    logger: KSPLogger
) : KSVisitorVoid() {

    private val generator = MapperGenerator(codeGenerator,logger)

    var resolver: Resolver? = null

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {

        var modelFromClass:KSType? = null
        var strictMode = false
        val parameters:MutableList<ParameterData> = mutableListOf()

        classDeclaration.annotations.iterator().forEach {annotation->
            val arguments = annotation.arguments
            when(annotation.shortName.getShortName()){
                MapperFromClass::class.simpleName -> {
                    strictMode = arguments.firstOrNull { it.name?.asString() == "strict" }?.value == true
                    modelFromClass = arguments.firstOrNull { it.name?.asString() == "classFrom" }?.value as KSType
                }
                Parameter::class.simpleName -> {
                    val source = arguments.firstOrNull { it.name?.asString() == "source" }?.value as String
                    val target = arguments.firstOrNull { it.name?.asString() == "target" }?.value as String
                    parameters.add(ParameterData(source, target))
                }
            }
        }

        modelFromClass?.let {
            generator.apply {
                this.strictMode = strictMode
                manualParameters = parameters
                resolverProcessor = resolver
                generate(
                    modelFrom = it,
                    modelTo = classDeclaration.asType(emptyList())
                )
            }
        }

    }
}