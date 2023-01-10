package pro.jorge.modelmapper.mapperfrom

import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class MapperFromProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment) =
        MapperFromProcessor(
            logger = environment.logger,
            codeGenerator = environment.codeGenerator
        )
}