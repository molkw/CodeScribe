package com.example.codescribe

import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil

data class ClassRelationship(
        val type: String,
        val targetClass: String,
        val description: String,
        val strength: Int // 1-10 relationship strength
)

class CodeRelationshipAnalyzer {
    private val relationships = mutableMapOf<String, MutableList<ClassRelationship>>()

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

    fun buildRelationships(classes: List<PsiClass>) {
        relationships.clear()

        classes.forEach { cls ->
            val className = cls.name ?: return@forEach
            relationships[className] = mutableListOf()

            // Analyze different types of relationships
            analyzeInheritanceRelationships(cls, classes)
            analyzeCompositionRelationships(cls, classes)
            analyzeDependencyRelationships(cls, classes)
            analyzeAssociationRelationships(cls, classes)
            analyzeUsageRelationships(cls, classes)
        }
    }

    fun getRelationships(cls: PsiClass): List<ClassRelationship> {
        return relationships[cls.name] ?: emptyList()
    }

    private fun analyzeInheritanceRelationships(cls: PsiClass, allClasses: List<PsiClass>) {
        val className = cls.name ?: return

        // Superclass relationship
        cls.superClass?.let { superClass ->
            if (superClass.name != "Object") {
                addRelationship(className, ClassRelationship(
                        type = "Inheritance",
                        targetClass = superClass.name ?: "Unknown",
                        description = "Extends ${superClass.name}, inheriting its behavior and properties",
                        strength = 9
                ))
            }
        }

        // Interface implementation
        cls.interfaces.forEach { interfaceClass ->
            addRelationship(className, ClassRelationship(
                    type = "Implementation",
                    targetClass = interfaceClass.name ?: "Unknown",
                    description = "Implements ${interfaceClass.name} contract",
                    strength = 8
            ))
        }

        // Find subclasses
        allClasses.forEach { otherClass ->
            if (otherClass.superClass?.name == className) {
                addRelationship(className, ClassRelationship(
                        type = "Inheritance (Parent)",
                        targetClass = otherClass.name ?: "Unknown",
                        description = "Parent class for ${otherClass.name}",
                        strength = 7
                ))
            }
        }
    }

    private fun analyzeCompositionRelationships(cls: PsiClass, allClasses: List<PsiClass>) {
        val className = cls.name ?: return

        cls.fields.forEach { field ->
            val fieldType = getTypeText(field.type)
            val relatedClass = findClassByType(fieldType, allClasses)

            if (relatedClass != null) {
                val isCollection = fieldType.contains("List") || fieldType.contains("Set") ||
                        fieldType.contains("Map") || fieldType.contains("Collection")

                val relationshipType = when {
                    field.hasModifierProperty(PsiModifier.FINAL) -> "Composition"
                    isCollection -> "Aggregation"
                    else -> "Association"
                }

                val description = when {
                    isCollection -> "Contains collection of ${relatedClass.name} objects"
                    field.hasModifierProperty(PsiModifier.FINAL) -> "Owns a ${relatedClass.name} instance (strong relationship)"
                    else -> "Has reference to ${relatedClass.name} instance"
                }

                val strength = when (relationshipType) {
                    "Composition" -> 9
                    "Aggregation" -> 7
                    else -> 6
                }

                addRelationship(className, ClassRelationship(
                        type = relationshipType,
                        targetClass = relatedClass.name ?: "Unknown",
                        description = description,
                        strength = strength
                ))
            }
        }
    }

    private fun analyzeDependencyRelationships(cls: PsiClass, allClasses: List<PsiClass>) {
        val className = cls.name ?: return

        // Constructor dependencies
        cls.constructors.forEach { constructor ->
            constructor.parameters.forEach { param ->
                val paramType = getTypeText(param.type as PsiType)
                val relatedClass = findClassByType(paramType, allClasses)

                if (relatedClass != null) {
                    val isInjected = param.annotations.any {
                        it.qualifiedName?.contains("Autowired") == true ||
                                it.qualifiedName?.contains("Inject") == true
                    }

                    val description = if (isInjected) {
                        "Dependency injected ${relatedClass.name} (IoC managed)"
                    } else {
                        "Constructor requires ${relatedClass.name} instance"
                    }

                    addRelationship(className, ClassRelationship(
                            type = "Dependency Injection",
                            targetClass = relatedClass.name ?: "Unknown",
                            description = description,
                            strength = 8
                    ))
                }
            }
        }

        // Method parameter dependencies
        cls.methods.forEach { method ->
            method.parameters.forEach { param ->
                val paramType = getTypeText(param.type as PsiType)
                val relatedClass = findClassByType(paramType, allClasses)

                if (relatedClass != null) {
                    addRelationship(className, ClassRelationship(
                            type = "Method Dependency",
                            targetClass = relatedClass.name ?: "Unknown",
                            description = "Method ${method.name}() depends on ${relatedClass.name}",
                            strength = 4
                    ))
                }
            }
        }
    }

    private fun analyzeAssociationRelationships(cls: PsiClass, allClasses: List<PsiClass>) {
        val className = cls.name ?: return

        // Method return type associations
        cls.methods.forEach { method ->
            val returnType = method.returnType?.let { getTypeText(it) }
            if (returnType != null && returnType != "void") {
                val relatedClass = findClassByType(returnType, allClasses)
                if (relatedClass != null) {
                    addRelationship(className, ClassRelationship(
                            type = "Factory/Creator",
                            targetClass = relatedClass.name ?: "Unknown",
                            description = "Creates/returns ${relatedClass.name} instances via ${method.name}()",
                            strength = 5
                    ))
                }
            }
        }
    }

    private fun analyzeUsageRelationships(cls: PsiClass, allClasses: List<PsiClass>) {
        val className = cls.name ?: return
        val usageCounts = mutableMapOf<String, Int>()

        // Count method calls to other classes
        cls.methods.forEach { method ->
            val methodBody = method.body
            if (methodBody != null) {
                val methodCalls = PsiTreeUtil.findChildrenOfType(methodBody, PsiMethodCallExpression::class.java)
                methodCalls.forEach { call ->
                    val receiverType = call.methodExpression.qualifierExpression?.type?.let { getTypeText(it) }
                    if (receiverType != null) {
                        val relatedClass = findClassByType(receiverType, allClasses)
                        if (relatedClass != null) {
                            val targetClassName = relatedClass.name ?: return@forEach
                            usageCounts[targetClassName] = (usageCounts[targetClassName] ?: 0) + 1
                        }
                    }
                }
            }
        }

        // Add usage relationships for frequently used classes
        usageCounts.forEach { (targetClassName, count) ->
            if (count >= 2) { // Only show significant usage
                val strength = minOf(count + 2, 8)
                addRelationship(className, ClassRelationship(
                        type = "Usage",
                        targetClass = targetClassName,
                        description = "Frequently calls methods on $targetClassName ($count usages)",
                        strength = strength
                ))
            }
        }
    }

    private fun findClassByType(typeName: String, allClasses: List<PsiClass>): PsiClass? {
        // Remove generic type parameters
        val cleanTypeName = typeName.substringBefore("<").trim()

        // Try exact match first
        allClasses.forEach { cls ->
            if (cls.name == cleanTypeName) return cls
        }

        // Try simple name match (for qualified names)
        val simpleTypeName = cleanTypeName.substringAfterLast(".")
        allClasses.forEach { cls ->
            if (cls.name == simpleTypeName) return cls
        }

        return null
    }

    private fun addRelationship(className: String, relationship: ClassRelationship) {
        // Avoid duplicate relationships
        val existingRelationships = relationships[className] ?: return
        val isDuplicate = existingRelationships.any { existing ->
            existing.type == relationship.type &&
                    existing.targetClass == relationship.targetClass
        }

        if (!isDuplicate) {
            existingRelationships.add(relationship)
        }
    }

    // Additional analysis methods
    fun getClassCoupling(cls: PsiClass): Int {
        val className = cls.name ?: return 0
        val classRelationships = relationships[className] ?: return 0

        return classRelationships.sumOf { it.strength } / maxOf(classRelationships.size, 1)
    }

    fun getMostConnectedClasses(limit: Int = 5): List<Pair<String, Int>> {
        return relationships.entries
                .map { (className, rels) ->
                    className to rels.sumOf { it.strength }
                }
                .sortedByDescending { it.second }
                .take(limit)
    }

    fun getClassesByRelationshipType(relationshipType: String): Map<String, List<String>> {
        val result = mutableMapOf<String, MutableList<String>>()

        relationships.forEach { (className, rels) ->
            val matchingRels = rels.filter { it.type == relationshipType }
            if (matchingRels.isNotEmpty()) {
                result[className] = matchingRels.map { it.targetClass }.toMutableList()
            }
        }

        return result
    }

    fun getDependencyChain(className: String, visited: MutableSet<String> = mutableSetOf()): List<String> {
        if (className in visited) return emptyList() // Circular dependency

        visited.add(className)
        val chain = mutableListOf<String>()

        val classRelationships = relationships[className] ?: return chain
        val dependencies = classRelationships.filter {
            it.type == "Dependency Injection" || it.type == "Composition"
        }

        dependencies.forEach { dep ->
            chain.add(dep.targetClass)
            chain.addAll(getDependencyChain(dep.targetClass, visited))
        }

        return chain.distinct()
    }

    fun generateRelationshipSummary(): String {
        val summary = StringBuilder()
        val totalRelationships = relationships.values.sumOf { it.size }

        summary.append("🔗 **Relationship Summary**\n")
        summary.append("Total relationships: $totalRelationships\n\n")

        // Count by type
        val typeCount = mutableMapOf<String, Int>()
        relationships.values.flatten().forEach { rel ->
            typeCount[rel.type] = (typeCount[rel.type] ?: 0) + 1
        }

        typeCount.entries.sortedByDescending { it.value }.forEach { (type, count) ->
            summary.append("- $type: $count\n")
        }

        return summary.toString()
    }
}