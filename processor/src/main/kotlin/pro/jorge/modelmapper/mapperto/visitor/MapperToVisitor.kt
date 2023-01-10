package pro.jorge.modelmapper.mapperto.visitor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSVisitorVoid
import pro.jorge.modelmapper.MapperGenerator
import pro.jorge.modelmapper.MapperToClass
import pro.jorge.modelmapper.Parameter
import pro.jorge.modelmapper.ParameterData

class MapperToVisitor(
    codeGenerator: CodeGenerator,
    logger: KSPLogger
) : KSVisitorVoid() {

    private val generator = MapperGenerator(codeGenerator,logger)

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {

        var modelToClass:KSType? = null
        var strictMode = false
        val parameters:MutableList<ParameterData> = mutableListOf()

        classDeclaration.annotations.iterator().forEach {annotation->
            val arguments = annotation.arguments
            when(annotation.shortName.getShortName()){
                MapperToClass::class.simpleName -> {
                    strictMode = arguments.firstOrNull { it.name?.asString() == "strict" }?.value == true
                    modelToClass = arguments.firstOrNull { it.name?.asString() == "classTo" }?.value as KSType
                }
                Parameter::class.simpleName -> {
                    val source = arguments.firstOrNull { it.name?.asString() == "source" }?.value as String
                    val target = arguments.firstOrNull { it.name?.asString() == "target" }?.value as String
                    parameters.add(ParameterData(source, target))
                }
            }
        }

        modelToClass?.let {
            generator.apply {
                this.strictMode = strictMode
                manualParameters = parameters
                generate(
                    modelFrom = classDeclaration.asType(emptyList()),
                    modelTo = it
                )
            }
        }

    }
}