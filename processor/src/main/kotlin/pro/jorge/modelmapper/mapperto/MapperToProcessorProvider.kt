package pro.jorge.modelmapper.mapperto

import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class MapperToProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment) =
        MapperToProcessor(
            logger = environment.logger,
            codeGenerator = environment.codeGenerator
        )
}