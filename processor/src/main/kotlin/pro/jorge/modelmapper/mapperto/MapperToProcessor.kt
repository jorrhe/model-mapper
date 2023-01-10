package pro.jorge.modelmapper.mapperto

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.validate
import pro.jorge.modelmapper.MapperToClass
import pro.jorge.modelmapper.mapperto.visitor.MapperToVisitor

class MapperToProcessor(
    logger: KSPLogger,
    codeGenerator: CodeGenerator
) : SymbolProcessor {

    private val visitor = MapperToVisitor(codeGenerator,logger)

    override fun process(resolver: Resolver): List<KSAnnotated> {

        var unresolvedClases: List<KSAnnotated> = emptyList()
        val annotationMapperToClass = MapperToClass::class.qualifiedName

        annotationMapperToClass?.let { annotation->
            val classes = resolver.getSymbolsWithAnnotation(annotation).toList()

            val validatedClasses = classes.filter { it.validate() }.toList()

            validatedClasses.forEach {
                it.accept(visitor,Unit)
            }

            unresolvedClases = classes - validatedClasses.toSet()
        }

        return unresolvedClases
    }
}