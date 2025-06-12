package com.example.codescribe

import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.lang.jvm.types.JvmType
import com.intellij.psi.PsiType

data class DetectedPattern(
        val name: String,
        val description: String,
        val confidence: Int // 1-10 confidence level
)

private fun getTypeText(type: JvmType): String {
    return when (type) {
        is PsiType -> try {
            type.getCanonicalText()
        } catch (_: Throwable) {
            type.toString()
        }
        else -> type.toString()
    }
}

class PatternDetector {

    private fun getTypeText(type: PsiType): String {
        return try {
            type.getCanonicalText()
        } catch (e: Exception) {
            try {
                type.toString()
            } catch (e2: Exception) {
                "Unknown"
            }
        }
    }

    fun detectPatterns(cls: PsiClass): List<DetectedPattern> {
        val patterns = mutableListOf<DetectedPattern>()

        // Creational Patterns
        patterns.addAll(detectCreationalPatterns(cls))

        // Structural Patterns
        patterns.addAll(detectStructuralPatterns(cls))

        // Behavioral Patterns
        patterns.addAll(detectBehavioralPatterns(cls))

        // Spring Patterns
        patterns.addAll(detectSpringPatterns(cls))

        return patterns.filter { it.confidence >= 6 } // Only return high-confidence patterns
    }

    fun detectArchitecturalPatterns(classes: List<PsiClass>): List<DetectedPattern> {
        val patterns = mutableListOf<DetectedPattern>()

        // MVC Pattern
        val controllers = classes.count { isController(it) }
        val services = classes.count { isService(it) }
        val repositories = classes.count { isRepository(it) }

        if (controllers > 0 && services > 0 && repositories > 0) {
            patterns.add(DetectedPattern(
                    "Model-View-Controller (MVC)",
                    "Separates application into presentation ($controllers controllers), business logic ($services services), and data access ($repositories repositories)",
                    9
            ))
        }

        // Layered Architecture
        if (services > 0 && repositories > 0) {
            patterns.add(DetectedPattern(
                    "Layered Architecture",
                    "Organizes code into distinct layers with clear separation of concerns",
                    8
            ))
        }

        // Repository Pattern
        if (repositories > 0) {
            patterns.add(DetectedPattern(
                    "Repository Pattern",
                    "Encapsulates data access logic in repository classes ($repositories found)",
                    8
            ))
        }

        // Dependency Injection
        val classesWithInjection = classes.count { hasInjectedDependencies(it) }
        if (classesWithInjection > classes.size * 0.5) {
            patterns.add(DetectedPattern(
                    "Dependency Injection",
                    "Uses IoC container to manage dependencies ($classesWithInjection classes with injection)",
                    9
            ))
        }

        return patterns
    }

    private fun detectCreationalPatterns(cls: PsiClass): List<DetectedPattern> {
        val patterns = mutableListOf<DetectedPattern>()

        // Singleton Pattern
        val singletonConfidence = detectSingleton(cls)
        if (singletonConfidence > 0) {
            patterns.add(DetectedPattern(
                    "Singleton Pattern",
                    "Ensures only one instance exists with global access point",
                    singletonConfidence
            ))
        }

        // Factory Pattern
        val factoryConfidence = detectFactory(cls)
        if (factoryConfidence > 0) {
            patterns.add(DetectedPattern(
                    "Factory Pattern",
                    "Creates objects without specifying exact classes to create",
                    factoryConfidence
            ))
        }

        // Builder Pattern
        val builderConfidence = detectBuilder(cls)
        if (builderConfidence > 0) {
            patterns.add(DetectedPattern(
                    "Builder Pattern",
                    "Constructs complex objects step by step with fluent interface",
                    builderConfidence
            ))
        }

        return patterns
    }

    private fun detectStructuralPatterns(cls: PsiClass): List<DetectedPattern> {
        val patterns = mutableListOf<DetectedPattern>()

        // Adapter Pattern
        val adapterConfidence = detectAdapter(cls)
        if (adapterConfidence > 0) {
            patterns.add(DetectedPattern(
                    "Adapter Pattern",
                    "Allows incompatible interfaces to work together",
                    adapterConfidence
            ))
        }

        // Decorator Pattern
        val decoratorConfidence = detectDecorator(cls)
        if (decoratorConfidence > 0) {
            patterns.add(DetectedPattern(
                    "Decorator Pattern",
                    "Adds new functionality to objects dynamically",
                    decoratorConfidence
            ))
        }

        // Facade Pattern
        val facadeConfidence = detectFacade(cls)
        if (facadeConfidence > 0) {
            patterns.add(DetectedPattern(
                    "Facade Pattern",
                    "Provides simplified interface to complex subsystem",
                    facadeConfidence
            ))
        }

        return patterns
    }

    private fun detectBehavioralPatterns(cls: PsiClass): List<DetectedPattern> {
        val patterns = mutableListOf<DetectedPattern>()

        // Observer Pattern
        val observerConfidence = detectObserver(cls)
        if (observerConfidence > 0) {
            patterns.add(DetectedPattern(
                    "Observer Pattern",
                    "Notifies multiple objects about state changes",
                    observerConfidence
            ))
        }

        // Strategy Pattern
        val strategyConfidence = detectStrategy(cls)
        if (strategyConfidence > 0) {
            patterns.add(DetectedPattern(
                    "Strategy Pattern",
                    "Defines family of algorithms and makes them interchangeable",
                    strategyConfidence
            ))
        }

        // Template Method Pattern
        val templateConfidence = detectTemplateMethod(cls)
        if (templateConfidence > 0) {
            patterns.add(DetectedPattern(
                    "Template Method Pattern",
                    "Defines algorithm skeleton, letting subclasses override specific steps",
                    templateConfidence
            ))
        }

        // Command Pattern
        val commandConfidence = detectCommand(cls)
        if (commandConfidence > 0) {
            patterns.add(DetectedPattern(
                    "Command Pattern",
                    "Encapsulates requests as objects to parameterize and queue operations",
                    commandConfidence
            ))
        }

        return patterns
    }

    private fun detectSpringPatterns(cls: PsiClass): List<DetectedPattern> {
        val patterns = mutableListOf<DetectedPattern>()

        // DTO Pattern
        if (isDTOClass(cls)) {
            patterns.add(DetectedPattern(
                    "Data Transfer Object (DTO)",
                    "Carries data between processes to reduce method calls",
                    8
            ))
        }

        // Service Layer Pattern
        if (isService(cls)) {
            patterns.add(DetectedPattern(
                    "Service Layer Pattern",
                    "Encapsulates business logic in service classes",
                    9
            ))
        }

        // Configuration Pattern
        if (isConfiguration(cls)) {
            patterns.add(DetectedPattern(
                    "Configuration Pattern",
                    "Centralizes application configuration using Spring @Configuration",
                    8
            ))
        }

        return patterns
    }

    // Pattern detection methods
    private fun detectSingleton(cls: PsiClass): Int {
        var confidence = 0

        // Private constructor
        val privateConstructors = cls.constructors.count { it.hasModifierProperty(PsiModifier.PRIVATE) }
        if (privateConstructors > 0) confidence += 3

        // Static instance field
        val staticInstanceFields = cls.fields.count { field ->
            field.hasModifierProperty(PsiModifier.STATIC) &&
                    field.type.presentableText.contains(cls.name ?: "")
        }
        if (staticInstanceFields > 0) confidence += 4

        // getInstance method
        val getInstanceMethods = cls.methods.count { method ->
            method.hasModifierProperty(PsiModifier.STATIC) &&
                    method.name.contains("instance", ignoreCase = true) &&
                    method.returnType?.presentableText?.contains(cls.name ?: "") == true
        }
        if (getInstanceMethods > 0) confidence += 3

        return confidence
    }

    private fun detectFactory(cls: PsiClass): Int {
        var confidence = 0

        // Class name contains "Factory"
        if (cls.name?.contains("Factory", ignoreCase = true) == true) confidence += 4

        // Static creation methods
        val creationMethods = cls.methods.count { method ->
            method.hasModifierProperty(PsiModifier.STATIC) &&
                    (method.name.startsWith("create") || method.name.startsWith("make") || method.name.startsWith("build"))
        }
        confidence += minOf(creationMethods * 2, 4)

        // Returns different types
        val returnTypes = cls.methods.map { it.returnType?.presentableText }.distinct()
        if (returnTypes.size > 2) confidence += 2

        return confidence
    }

    private fun detectBuilder(cls: PsiClass): Int {
        var confidence = 0

        // Build method
        if (cls.methods.any { it.name == "build" }) confidence += 4

        // Fluent interface (methods returning this)
        val fluentMethods = cls.methods.count { method ->
            method.returnType?.let { getTypeText(it) } == cls.name
        }
        if (fluentMethods > 2) confidence += 4

        // Class name contains "Builder"
        if (cls.name?.contains("Builder", ignoreCase = true) == true) confidence += 2

        return confidence
    }

    private fun detectAdapter(cls: PsiClass): Int {
        var confidence = 0

        // Class name contains "Adapter"
        if (cls.name?.contains("Adapter", ignoreCase = true) == true) confidence += 4

        // Implements interface and has composition
        if (cls.interfaces.isNotEmpty() && cls.fields.isNotEmpty()) confidence += 3

        // Delegates to composed object
        val delegationMethods = cls.methods.count { method ->
            val body = method.body
            body?.let {
                PsiTreeUtil.findChildrenOfType(it, PsiMethodCallExpression::class.java)
                        .any { call -> !call.methodExpression.text.startsWith("this.") }
            } ?: false
        }
        if (delegationMethods > 0) confidence += 3

        return confidence
    }

    private fun detectDecorator(cls: PsiClass): Int {
        var confidence = 0

        // Class name contains "Decorator"
        if (cls.name?.contains("Decorator", ignoreCase = true) == true) confidence += 3

        // Implements same interface as composed object
        val interfaces = cls.interfaces.map { it.qualifiedName }
        val composedObjectTypes = cls.fields.map { it.type.presentableText }

        val hasMatchingInterface = interfaces.any { interfaceName ->
            composedObjectTypes.any { it.contains(interfaceName?.substringAfterLast(".") ?: "") }
        }
        if (hasMatchingInterface) confidence += 5

        // Constructor takes interface type
        cls.constructors.forEach { constructor ->
            val paramTypes = constructor.parameters.map { getTypeText(it.type) }
            if (
                    paramTypes.any { type ->
                        interfaces.any { iface ->
                            type.contains(iface?.substringAfterLast(".") ?: "")
                        }
                    }
            ) {
                confidence += 2
            }
        }

        return confidence
    }

    private fun detectFacade(cls: PsiClass): Int {
        var confidence = 0

        // Class name contains "Facade"
        if (cls.name?.contains("Facade", ignoreCase = true) == true) confidence += 4

        // Has multiple dependencies (composition)
        if (cls.fields.size > 2) confidence += 3

        // Methods delegate to multiple objects
        val methodsWithMultipleCalls = cls.methods.count { method ->
            val body = method.body
            body?.let {
                PsiTreeUtil.findChildrenOfType(it, PsiMethodCallExpression::class.java).size > 2
            } ?: false
        }
        if (methodsWithMultipleCalls > 0) confidence += 3

        return confidence
    }

    private fun detectObserver(cls: PsiClass): Int {
        var confidence = 0

        // Has notify/update methods
        val observerMethods = cls.methods.count { method ->
            method.name.contains("notify", ignoreCase = true) ||
                    method.name.contains("update", ignoreCase = true) ||
                    method.name.contains("observe", ignoreCase = true)
        }
        confidence += minOf(observerMethods * 3, 6)

        // Has list of observers
        val observerLists = cls.fields.count { field ->
            field.type.presentableText.contains("List") &&
                    (field.name.contains("observer", ignoreCase = true) ||
                            field.name.contains("listener", ignoreCase = true))
        }
        if (observerLists > 0) confidence += 4

        return confidence
    }

    private fun detectStrategy(cls: PsiClass): Int {
        var confidence = 0

        // Has strategy field
        val strategyFields = cls.fields.count { field ->
            field.name.contains("strategy", ignoreCase = true) ||
                    field.name.contains("algorithm", ignoreCase = true)
        }
        if (strategyFields > 0) confidence += 4

        // Constructor injection of strategy
        cls.constructors.forEach { constructor ->
            val hasStrategyParam = constructor.parameters.any { param ->
                param.name?.contains("strategy", ignoreCase = true) == true ||
                        getTypeText(param.type).contains("Strategy")
            }
            if (hasStrategyParam) confidence += 3
        }

        // Delegates to strategy
        val delegationToStrategy = cls.methods.any { method ->
            val body = method.body
            body?.let {
                PsiTreeUtil.findChildrenOfType(it, PsiMethodCallExpression::class.java)
                        .any { call -> call.methodExpression.text.contains("strategy") }
            } ?: false
        }
        if (delegationToStrategy) confidence += 3

        return confidence
    }

    private fun detectTemplateMethod(cls: PsiClass): Int {
        var confidence = 0

        // Abstract class
        if (cls.hasModifierProperty(PsiModifier.ABSTRACT)) confidence += 3

        // Has abstract methods
        val abstractMethods = cls.methods.count { it.hasModifierProperty(PsiModifier.ABSTRACT) }
        if (abstractMethods > 0) confidence += 4

        // Has template method calling abstract methods
        val templateMethods = cls.methods.count { method ->
            val body = method.body
            body?.let {
                PsiTreeUtil.findChildrenOfType(it, PsiMethodCallExpression::class.java)
                        .any { call ->
                            cls.methods.any { m ->
                                m.hasModifierProperty(PsiModifier.ABSTRACT) &&
                                        call.methodExpression.text.contains(m.name)
                            }
                        }
            } ?: false
        }
        if (templateMethods > 0) confidence += 3

        return confidence
    }

    private fun detectCommand(cls: PsiClass): Int {
        var confidence = 0

        // Class name contains "Command"
        if (cls.name?.contains("Command", ignoreCase = true) == true) confidence += 4

        // Has execute method
        if (cls.methods.any { it.name.equals("execute", ignoreCase = true) }) confidence += 4

        // Implements Command interface
        if (cls.interfaces.any { it.name?.contains("Command") == true }) confidence += 2

        return confidence
    }

    // Helper methods
    private fun isController(cls: PsiClass): Boolean {
        return cls.annotations.any {
            it.qualifiedName?.contains("Controller") == true ||
                    it.qualifiedName?.contains("RestController") == true
        } || cls.name?.contains("Controller", ignoreCase = true) == true
    }

    private fun isService(cls: PsiClass): Boolean {
        return cls.annotations.any {
            it.qualifiedName?.contains("Service") == true
        } || cls.name?.contains("Service", ignoreCase = true) == true
    }

    private fun isRepository(cls: PsiClass): Boolean {
        return cls.annotations.any {
            it.qualifiedName?.contains("Repository") == true
        } || cls.name?.contains("Repository", ignoreCase = true) == true ||
                cls.name?.contains("DAO", ignoreCase = true) == true
    }

    private fun isConfiguration(cls: PsiClass): Boolean {
        return cls.annotations.any {
            it.qualifiedName?.contains("Configuration") == true
        }
    }

    private fun isDTOClass(cls: PsiClass): Boolean {
        // Simple data class indicators
        val publicFields = cls.fields.count { it.hasModifierProperty(PsiModifier.PUBLIC) }
        val getters = cls.methods.count { it.name.startsWith("get") }
        val setters = cls.methods.count { it.name.startsWith("set") }
        val businessMethods = cls.methods.count {
            !it.name.startsWith("get") && !it.name.startsWith("set") &&
                    !it.name.equals("toString") && !it.name.equals("equals") &&
                    !it.name.equals("hashCode") && !it.isConstructor
        }

        return (publicFields > 0 || (getters > 0 && setters > 0)) && businessMethods == 0
    }

    private fun hasInjectedDependencies(cls: PsiClass): Boolean {
        // Check for dependency injection annotations
        val hasInjectedFields = cls.fields.any { field ->
            field.annotations.any { annotation ->
                annotation.qualifiedName?.contains("Autowired") == true ||
                        annotation.qualifiedName?.contains("Inject") == true
            }
        }

        val hasInjectedConstructors = cls.constructors.any { constructor ->
            constructor.annotations.any { annotation ->
                annotation.qualifiedName?.contains("Autowired") == true ||
                        annotation.qualifiedName?.contains("Inject") == true
            }
        }

        return hasInjectedFields || hasInjectedConstructors
    }
}