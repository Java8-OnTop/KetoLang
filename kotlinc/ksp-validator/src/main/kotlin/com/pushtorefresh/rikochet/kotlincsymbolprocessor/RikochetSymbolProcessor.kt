package com.pushtorefresh.ketolang.kotlincsymbolprocessor

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.Modifier

class ketolangSymbolProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {

    private lateinit var resolver: Resolver
    private val TYPE_MUTABLE_COLLECTION by lazy(LazyThreadSafetyMode.NONE) { resolver.getClassDeclarationByName("kotlin.collections.MutableCollection")!! }
    private val TYPE_COLLECTION by lazy(LazyThreadSafetyMode.NONE) { resolver.getClassDeclarationByName("kotlin.collections.Collection")!! }
    private val TYPE_MAP by lazy(LazyThreadSafetyMode.NONE) { resolver.getClassDeclarationByName("kotlin.collections.Map")!! }
    private val TYPE_ANY by lazy(LazyThreadSafetyMode.NONE) { resolver.getClassDeclarationByName("kotlin.Any")!! }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        this.resolver = resolver

        val ketolangValidationErrors = resolver
            .getAllFiles()
            .flatMap { file ->
                file
                    .declarations
                    .flatMap { declaration -> validateDeclaration(declaration, resolver) }
            }
            .filterNotNull()
            .toList()

        if (ketolangValidationErrors.isNotEmpty()) {
            ketolangValidationErrors.forEach {
                environment.logger.warn(
                    "${it.message}, node name = '${it.node.printableName()}'",
                    it.node
                )
            }
            environment.logger.error("ketolang validation errors were found, aborting compilation!")
        }

        return emptyList()
    }

    private fun validateDeclaration(declaration: KSDeclaration, resolver: Resolver): List<ketolangValidationError?> {
        return when (declaration) {
            is KSPropertyDeclaration -> listOf(validateProperty(declaration))
            is KSFunctionDeclaration -> listOf(validateFunction(declaration, resolver))
            is KSTypeAlias -> listOf(validateTypeAlias(declaration))
            is KSClassDeclaration -> {
                validateClass(declaration).let {
                    if (it == null) {
                        declaration.declarations.flatMap { validateDeclaration(it, resolver) }.toList()
                    } else {
                        listOf(it)
                    }
                }
            }

            else -> emptyList()
        }
    }

    private fun validateProperty(
        property: KSPropertyDeclaration
    ): ketolangValidationError? {
        return when (val parentDeclaration = property.parentDeclaration) {
            is KSClassDeclaration -> {
                when {
                    parentDeclaration.modifiers.size == 1 && parentDeclaration.modifiers.contains(Modifier.DATA) -> validateDataClassProperty(
                        property
                    )

                    parentDeclaration.classKind == ClassKind.ENUM_CLASS -> validateEnumProperty(property)
                    else -> ketolangValidationError(
                        "ketolang error: property looks suspicious! Perhaps ketolang needs an update to validate it",
                        property
                    )
                }
            }

            null -> validateTopLevelProperty(property)
            else -> ketolangValidationError(
                "ketolang error: property looks suspicious! Perhaps ketolang needs an update to validate it",
                property
            )
        }
    }

    private fun validateTopLevelProperty(
        property: KSPropertyDeclaration,
    ): ketolangValidationError? {
        val type by lazy(LazyThreadSafetyMode.NONE) { property.type.resolve() }

        if (property.modifiers.contains(Modifier.CONST)) {
            return null
        } else if (type.isPrimitive() || type.isString()) {
            return ketolangValidationError(
                "ketolang error: primitive and String properties must be declared as 'const'",
                property
            )
        }

        if (property.modifiers.contains(Modifier.LATEINIT)) {
            return ketolangValidationError("ketolang error: lateinit properties are not allowed!", property)
        }

        if (property.isMutable) {
            return ketolangValidationError(
                "ketolang error: mutable properties are not allowed!",
                property
            )
        }

        if (type.isCollection()) {
            if (type.isImmutableCollection()) {
                return null
            } else {
                return ketolangValidationError(
                    "ketolang error: mutable collection properties are not allowed!",
                    property
                )
            }
        }

        if (type.isArray()) {
            return ketolangValidationError(
                "ketolang error: top-level array properties are not allowed because arrays are mutable",
                property
            )
        }

        if (property.isDelegated()) {
            return ketolangValidationError("ketolang error: delegated properties are not allowed!", property)
        }

        return ketolangValidationError(
            "ketolang error: property looks suspicious! Perhaps ketolang needs an update to validate it",
            property
        )
    }

    private fun validateDataClassProperty(
        property: KSPropertyDeclaration,
    ): ketolangValidationError? {
        val type by lazy(LazyThreadSafetyMode.NONE) { property.type.resolve() }

        if (property.modifiers.contains(Modifier.LATEINIT)) {
            return ketolangValidationError("ketolang error: lateinit properties are not allowed!", property)
        }

        if (property.isMutable) {
            return ketolangValidationError(
                "ketolang error: mutable properties are not allowed!",
                property
            )
        }

        if (type.isPrimitive() || type.isString()) {
            return null
        }

        if (type.isCollection()) {
            if (type.isImmutableCollection()) {
                return null
            } else {
                return ketolangValidationError(
                    "ketolang error: mutable collection properties are not allowed!",
                    property
                )
            }
        }

        if (type.isArray()) {
            return ketolangValidationError(
                "ketolang error: array properties are not allowed because arrays are mutable",
                property
            )
        }

        if (property.isDelegated()) {
            return ketolangValidationError("ketolang error: delegated properties are not allowed!", property)
        }

        return ketolangValidationError(
            "ketolang error: property looks suspicious! Perhaps ketolang needs an update to validate it",
            property
        )
    }

    private fun validateEnumProperty(
        property: KSPropertyDeclaration
    ): ketolangValidationError? {
        return validateDataClassProperty(property)
    }

    private fun validateTypeAlias(typeAlias: KSTypeAlias): ketolangValidationError? {
        return ketolangValidationError("ketolang error: type-aliases are not allowed!", typeAlias)
    }

    private fun validateFunction(function: KSFunctionDeclaration, resolver: Resolver): ketolangValidationError? {
        return when (function.parentDeclaration) {
            is KSClassDeclaration -> validateClassFunction(function)
            null -> validateTopLevelFunction(function, resolver)
            else -> ketolangValidationError(
                "ketolang error: function looks suspicious! Perhaps ketolang needs an update to validate it",
                function
            )
        }
    }

    private fun validateClassFunction(function: KSFunctionDeclaration): ketolangValidationError? {
        if (function.isConstructor()) {
            // TODO: validate constructors too.
            return null
        } else {
            return ketolangValidationError(
                "ketolang error: functions in classes are not allowed!",
                function
            )
        }
    }

    private fun validateTopLevelFunction(
        function: KSFunctionDeclaration,
        resolver: Resolver
    ): ketolangValidationError? {
        if (function.modifiers.contains(Modifier.SUSPEND)) {
            return ketolangValidationError("ketolang error: suspend functions are not allowed!", function)
        }

        val returnType = function.returnType!!.resolve()

        if (returnType == resolver.builtIns.unitType) {
            return ketolangValidationError("ketolang error: functions returning Unit are not allowed!", function)
        }

        if (returnType.declaration == TYPE_ANY) {
            return ketolangValidationError("ketolang error: functions returning Any are not allowed!", function)
        }

        if (returnType.isCollection()) {
            if (returnType.isImmutableCollection()) {
                return null
            } else {
                return ketolangValidationError(
                    "ketolang error: functions returning mutable collections are not allowed!",
                    function
                )
            }
        }

        if (function.parameters.isEmpty()) {
            return ketolangValidationError(
                "ketolang error: functions without parameters are not allowed!",
                function
            )
        }

        if (function.parameters.map { it.type.resolve() }
                .all { it.isPrimitive() || it.isString() || it.isImmutableCollection() }) {
            return null
        } else {
            return ketolangValidationError(
                "ketolang error: functions accepting mutable parameters are not allowed!",
                function
            )
        }

        /*return ketolangValidationError(
            "ketolang error: function looks suspicious! Perhaps ketolang needs an update to validate it.",
            function
        )*/
    }

    private fun validateClass(clazz: KSClassDeclaration): ketolangValidationError? {
        if (clazz.isAbstract()) {
            return ketolangValidationError("ketolang error: abstract classes and interfaces are not allowed!", clazz)
        }

        if (clazz.modifiers.size == 1 && clazz.modifiers.contains(Modifier.DATA)) {
            return null
        } else if (clazz.modifiers.size == 1 && clazz.modifiers.contains(Modifier.ENUM) || clazz.classKind == ClassKind.ENUM_ENTRY) {
            return null
        }

        return ketolangValidationError(
            "ketolang error: regular classes are not allowed, only data classes and enums are allowed!",
            clazz
        )
    }

    private fun KSType.isPrimitive(): Boolean {
        val qualifiedName = declaration.qualifiedName?.asString()
        return qualifiedName == "kotlin.Int"
                || qualifiedName == "kotlin.Long"
                || qualifiedName == "kotlin.Short"
                || qualifiedName == "kotlin.Byte"
    }

    private fun KSType.isString(): Boolean {
        return declaration.qualifiedName?.asString() == "kotlin.String"
    }

    private fun KSType.isArray(): Boolean {
        return declaration.qualifiedName?.asString() == "kotlin.Array"
    }

    private fun KSType.isImmutableCollection(): Boolean {
        val qualifiedName = declaration.qualifiedName?.asString()

        val isImmutableCollection = qualifiedName == "kotlin.collections.List"
                || qualifiedName == "kotlin.collections.Set"
                || qualifiedName == "kotlin.collections.Map"

        if (!isImmutableCollection) {
            return false
        }

        return arguments.all { it.type!!.resolve().isPrimitive() || it.type!!.resolve().isString() }
    }

    private fun KSType.isCollection(): Boolean {
        return TYPE_COLLECTION.asStarProjectedType().isAssignableFrom(this)
                || TYPE_MAP.asStarProjectedType().isAssignableFrom(this)
    }

    private fun KSNode.printableName(): String? {
        return when (this) {
            is KSDeclaration -> simpleName.asString()
            else -> "no printable name"
        }
    }
}
