package com.example.codescribe

import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil

data class ClassAnalysisResult(
        val purpose: String,
        val complexityScore: Int,
        val patterns: List<String>,
        val responsibilities: List<String>
)

data class MethodAnalysisResult(
        val description: String,
        val complexity: Int,
        val patterns: List<String>,
        val potentialIssues: List<String>
)

class AdvancedCodeAnalyzer {

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

    fun analyzeClass(cls: PsiClass): ClassAnalysisResult {
        val purpose = determinePurpose(cls)
        val complexityScore = calculateClassComplexity(cls)
        val patterns = detectClassPatterns(cls)
        val responsibilities = identifyResponsibilities(cls)

        return ClassAnalysisResult(purpose, complexityScore, patterns, responsibilities)
    }

    fun analyzeMethod(method: PsiMethod): MethodAnalysisResult {
        val description = generateMethodDescription(method)
        val complexity = calculateMethodComplexity(method)
        val patterns = detectMethodPatterns(method)
        val issues = detectPotentialIssues(method)

        return MethodAnalysisResult(description, complexity, patterns, issues)
    }

    fun analyzeField(field: PsiField): String {
        val modifiers = field.modifierList?.text ?: ""
        val type = getTypeText(field.type)
        val purpose = when {
            field.hasModifierProperty(PsiModifier.STATIC) && field.hasModifierProperty(PsiModifier.FINAL) -> "Constant value"
            field.hasModifierProperty(PsiModifier.STATIC) -> "Class-level shared variable"
            field.name.contains("logger", ignoreCase = true) -> "Logging utility"
            field.name.contains("cache", ignoreCase = true) -> "Caching mechanism"
            field.name.contains("config", ignoreCase = true) -> "Configuration setting"
            getTypeText(field.type).contains("List", ignoreCase = true) -> "Collection of ${extractGenericType(field.type)}"
            getTypeText(field.type).contains("Map", ignoreCase = true) -> "Key-value mapping"
            else -> "Instance variable of type $type"
        }

        return "$purpose ($modifiers $type)"
    }

    fun analyzeCodeQuality(cls: PsiClass): List<String> {
        val insights = mutableListOf<String>()

        // Check for code smells
        if (cls.methods.size > 15) {
            insights.add("⚠️ God Class: Too many methods (${cls.methods.size})")
        }

        if (cls.fields.size > 10) {
            insights.add("⚠️ Too many fields: Consider grouping related data")
        }

        // Check for good practices
        val hasToString = cls.methods.any { it.name == "toString" }
        val hasEquals = cls.methods.any { it.name == "equals" }
        val hasHashCode = cls.methods.any { it.name == "hashCode" }

        if (!hasToString && !cls.isInterface) {
            insights.add("💡 Consider overriding toString() for better debugging")
        }

        if (hasEquals && !hasHashCode) {
            insights.add("⚠️ Equals without hashCode - violates contract")
        }

        // Check for documentation
        val documentedMethods = cls.methods.count { it.docComment != null }
        val publicMethods = cls.methods.count { it.hasModifierProperty(PsiModifier.PUBLIC) }

        if (publicMethods > 0 && documentedMethods.toDouble() / publicMethods < 0.5) {
            insights.add("📝 Low documentation coverage for public methods")
        }

        // Check for exception handling
        val methodsWithTryCatch = cls.methods.count { method ->
            PsiTreeUtil.findChildrenOfType(method, PsiTryStatement::class.java).isNotEmpty()
        }

        if (methodsWithTryCatch == 0 && cls.methods.size > 3) {
            insights.add("🛡️ No exception handling detected - consider error scenarios")
        }

        return insights
    }

    private fun determinePurpose(cls: PsiClass): String {
        // Check annotations first
        cls.annotations.forEach { annotation ->
            when {
                annotation.qualifiedName?.contains("RestController") == true ->
                    return "REST API controller handling HTTP requests and responses"
                annotation.qualifiedName?.contains("Service") == true ->
                    return "Business logic service implementing core application functionality"
                annotation.qualifiedName?.contains("Repository") == true ->
                    return "Data access layer handling database operations and queries"
                annotation.qualifiedName?.contains("Configuration") == true ->
                    return "Configuration class defining application settings and beans"
                annotation.qualifiedName?.contains("Component") == true ->
                    return "Spring-managed component providing specialized functionality"
            }
        }

        // Analyze by naming conventions and structure
        val className = cls.name?.lowercase() ?: ""
        return when {
            className.contains("controller") -> "Controller managing user interactions and request routing"
            className.contains("service") -> "Service class encapsulating business logic and workflows"
            className.contains("repository") || className.contains("dao") -> "Data access object for database operations"
            className.contains("config") -> "Configuration class for application setup"
            className.contains("util") || className.contains("helper") -> "Utility class providing reusable helper methods"
            className.contains("factory") -> "Factory class for creating and configuring objects"
            className.contains("builder") -> "Builder class for constructing complex objects"
            className.contains("exception") -> "Custom exception class for error handling"
            className.contains("dto") || className.contains("model") -> "Data transfer object representing application data"
            cls.isInterface -> "Interface defining contract for ${cls.name} implementations"
            cls.isEnum -> "Enumeration defining ${cls.name} constants and values"
            cls.hasModifierProperty(PsiModifier.ABSTRACT) -> "Abstract base class providing common functionality"
            else -> "Class implementing ${inferPurposeFromMethods(cls)}"
        }
    }

    private fun inferPurposeFromMethods(cls: PsiClass): String {
        val methodNames = cls.methods.map { it.name.lowercase() }

        return when {
            methodNames.any { it.contains("crud") || it.contains("save") || it.contains("delete") } ->
                "data persistence and CRUD operations"
            methodNames.any { it.contains("validate") || it.contains("check") } ->
                "data validation and verification logic"
            methodNames.any { it.contains("calculate") || it.contains("compute") } ->
                "computational logic and calculations"
            methodNames.any { it.contains("transform") || it.contains("convert") } ->
                "data transformation and conversion"
            methodNames.any { it.contains("send") || it.contains("notify") } ->
                "communication and notification services"
            else -> "specialized business logic"
        }
    }

    private fun calculateClassComplexity(cls: PsiClass): Int {
        var complexity = 0

        // Base complexity
        complexity += cls.methods.size / 2
        complexity += cls.fields.size / 3

        // Method complexity
        cls.methods.forEach { method ->
            complexity += calculateMethodComplexity(method) / 3
        }

        // Inheritance complexity
        if (cls.superClass != null && cls.superClass?.name != "Object") complexity += 2
        complexity += cls.interfaces.size

        // Annotation complexity
        complexity += cls.annotations.size / 2

        return minOf(complexity, 10)
    }

    private fun calculateMethodComplexity(method: PsiMethod): Int {
        var complexity = 1 // Base complexity

        val body = method.body ?: return complexity

        // Control flow statements
        complexity += PsiTreeUtil.findChildrenOfType(body, PsiIfStatement::class.java).size * 2
        complexity += PsiTreeUtil.findChildrenOfType(body, PsiForStatement::class.java).size * 2
        complexity += PsiTreeUtil.findChildrenOfType(body, PsiWhileStatement::class.java).size * 2
        complexity += PsiTreeUtil.findChildrenOfType(body, PsiSwitchStatement::class.java).size * 3
        complexity += PsiTreeUtil.findChildrenOfType(body, PsiTryStatement::class.java).size * 2

        // Method calls
        complexity += PsiTreeUtil.findChildrenOfType(body, PsiMethodCallExpression::class.java).size / 3

        // Parameters
        complexity += method.parameters.size

        return minOf(complexity, 10)
    }

    private fun detectClassPatterns(cls: PsiClass): List<String> {
        val patterns = mutableListOf<String>()

        // Singleton pattern
        val privateConstructors = cls.constructors.count { it.hasModifierProperty(PsiModifier.PRIVATE) }
        val staticInstanceFields = cls.fields.count {
            it.hasModifierProperty(PsiModifier.STATIC) &&
                    it.type.presentableText.contains(cls.name ?: "")
        }
        if (privateConstructors > 0 && staticInstanceFields > 0) {
            patterns.add("Singleton Pattern")
        }

        // Builder pattern
        if (cls.methods.any { it.name == "build" } &&
                cls.methods.count { it.returnType?.presentableText == cls.name } > 2) {
            patterns.add("Builder Pattern")
        }

        // Factory pattern
        if (cls.name?.contains("Factory", ignoreCase = true) == true ||
                cls.methods.any { it.name.startsWith("create") || it.name.startsWith("make") }) {
            patterns.add("Factory Pattern")
        }

        return patterns
    }

    private fun detectMethodPatterns(method: PsiMethod): List<String> {
        val patterns = mutableListOf<String>()
        val methodName = method.name.lowercase()
        val body = method.body

        // Template method pattern
        if (body != null && PsiTreeUtil.findChildrenOfType(body, PsiMethodCallExpression::class.java)
                        .any { it.methodExpression.text.startsWith("super.") }) {
            patterns.add("Template Method")
        }

        // Strategy pattern (method delegates to other objects)
        if (body != null && PsiTreeUtil.findChildrenOfType(body, PsiMethodCallExpression::class.java).size > 3) {
            patterns.add("Delegation")
        }

        // Observer pattern
        if (methodName.contains("notify") || methodName.contains("update") || methodName.contains("observe")) {
            patterns.add("Observer Pattern")
        }

        return patterns
    }

    private fun generateMethodDescription(method: PsiMethod): String {
        val name = method.name
        val paramCount = method.parameters.size
        val hasReturnValue = method.returnType?.let { getTypeText(it) } != "void"

        val baseDescription = when {
            name.startsWith("get") && paramCount == 0 -> "Retrieves ${extractPropertyName(name)} value"
            name.startsWith("set") && paramCount == 1 -> "Updates ${extractPropertyName(name)} with new value"
            name.startsWith("is") && paramCount == 0 -> "Checks if ${extractPropertyName(name)} condition is true"
            name.startsWith("has") && paramCount == 0 -> "Verifies if ${extractPropertyName(name)} exists"
            name.startsWith("create") -> "Creates new ${name.removePrefix("create")} instance"
            name.startsWith("delete") || name.startsWith("remove") -> "Removes ${name.removePrefix("delete").removePrefix("remove")} from system"
            name.startsWith("update") -> "Modifies existing ${name.removePrefix("update")} record"
            name.startsWith("find") || name.startsWith("search") -> "Searches for ${name.removePrefix("find").removePrefix("search")} matching criteria"
            name.startsWith("validate") -> "Validates ${name.removePrefix("validate")} input data"
            name.startsWith("calculate") || name.startsWith("compute") -> "Computes ${name.removePrefix("calculate").removePrefix("compute")} result"
            name.startsWith("process") -> "Processes ${name.removePrefix("process")} data or request"
            name == "toString" -> "Returns string representation of object"
            name == "equals" -> "Compares objects for equality"
            name == "hashCode" -> "Generates hash code for object"
            else -> "Executes $name operation"
        }

        val complexity = calculateMethodComplexity(method)
        val complexityNote = when {
            complexity > 7 -> " (High complexity - consider refactoring)"
            complexity > 4 -> " (Moderate complexity)"
            else -> ""
        }

        return baseDescription + complexityNote
    }

    private fun detectPotentialIssues(method: PsiMethod): List<String> {
        val issues = mutableListOf<String>()
        val body = method.body ?: return issues

        // Long method
        if (calculateMethodComplexity(method) > 7) {
            issues.add("High complexity")
        }

        // Too many parameters
        if (method.parameters.size > 5) {
            issues.add("Too many parameters")
        }

        // No exception handling
        val hasTryCatch = PsiTreeUtil.findChildrenOfType(body, PsiTryStatement::class.java).isNotEmpty()
        val hasMethodCalls = PsiTreeUtil.findChildrenOfType(body, PsiMethodCallExpression::class.java).size > 2

        if (!hasTryCatch && hasMethodCalls) {
            issues.add("Missing exception handling")
        }

        // Empty catch blocks
        PsiTreeUtil.findChildrenOfType(body, PsiTryStatement::class.java).forEach { tryStmt ->
            tryStmt.catchSections.forEach { catchSection ->
                if (catchSection.catchBlock?.statements?.isEmpty() == true) {
                    issues.add("Empty catch block")
                }
            }
        }

        return issues
    }

    private fun identifyResponsibilities(cls: PsiClass): List<String> {
        val responsibilities = mutableListOf<String>()

        cls.methods.forEach { method ->
            val name = method.name.lowercase()
            when {
                name.contains("save") || name.contains("persist") ->
                    responsibilities.add("Data persistence")
                name.contains("validate") ->
                    responsibilities.add("Input validation")
                name.contains("transform") || name.contains("convert") ->
                    responsibilities.add("Data transformation")
                name.contains("send") || name.contains("notify") ->
                    responsibilities.add("Communication")
                name.contains("log") || name.contains("audit") ->
                    responsibilities.add("Logging/Auditing")
            }
        }

        return responsibilities.distinct()
    }

    private fun extractPropertyName(methodName: String): String {
        return methodName.removePrefix("get").removePrefix("set").removePrefix("is").removePrefix("has")
                .replaceFirstChar { it.lowercase() }
    }

    private fun extractGenericType(type: PsiType): String {
        return getTypeText(type).substringAfter("<").substringBefore(">").ifEmpty { "items" }
    }
}