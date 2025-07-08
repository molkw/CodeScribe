// src/main/kotlin/com/example/codescribe/ProjectToJsonConverter.kt
// CREATE this new file in your plugin

package com.example.codescribe

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import java.nio.charset.StandardCharsets

class ProjectToJsonConverter(private val project: Project) {
    private val logger = Logger.getInstance(ProjectToJsonConverter::class.java)

    /**
     * Convert the entire project to ClassInfo objects for AI analysis
     */
    fun convertProjectToClassInfo(): List<ClassInfo> {
        val baseDir = project.baseDir ?: return emptyList()

        logger.info("🔍 Converting project to JSON format for AI analysis")

        val javaFiles = mutableListOf<VirtualFile>()
        collectJavaFiles(baseDir, javaFiles)

        logger.info("📁 Found ${javaFiles.size} Java files to analyze")

        val allClasses = mutableListOf<ClassInfo>()

        javaFiles.forEach { file ->
            try {
                val psiFile = PsiManager.getInstance(project).findFile(file)
                if (psiFile != null) {
                    val classesInFile = extractClassesFromFile(psiFile, file)
                    allClasses.addAll(classesInFile)
                }
            } catch (e: Exception) {
                logger.warn("⚠️ Failed to process file ${file.name}: ${e.message}")
            }
        }

        logger.info("✅ Converted ${allClasses.size} classes for AI analysis")
        return allClasses
    }

    /**
     * Extract all classes from a single Java file
     */
    private fun extractClassesFromFile(psiFile: PsiFile, virtualFile: VirtualFile): List<ClassInfo> {
        val classes = PsiTreeUtil.findChildrenOfType(psiFile, PsiClass::class.java)

        return classes.mapNotNull { psiClass ->
            try {
                convertPsiClassToClassInfo(psiClass, psiFile, virtualFile)
            } catch (e: Exception) {
                logger.warn("⚠️ Failed to convert class ${psiClass.name}: ${e.message}")
                null
            }
        }
    }

    /**
     * Convert a single PSI class to ClassInfo format
     */
    private fun convertPsiClassToClassInfo(
            psiClass: PsiClass,
            psiFile: PsiFile,
            virtualFile: VirtualFile
    ): ClassInfo {
        val className = psiClass.name ?: "Unknown"
        val packageName = getPackageName(psiFile)

        // Read source code safely
        val sourceCode = try {
            String(virtualFile.contentsToByteArray(), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            logger.debug("Could not read source for $className: ${e.message}")
            "// Source code unavailable"
        }

        // Extract annotations
        val annotations = psiClass.annotations.mapNotNull { annotation ->
            try {
                annotation.qualifiedName
            } catch (e: Exception) {
                logger.debug("Could not get annotation for $className: ${e.message}")
                null
            }
        }

        // Extract methods
        val methods = psiClass.methods.mapNotNull { method ->
            try {
                convertPsiMethodToMethodInfo(method)
            } catch (e: Exception) {
                logger.debug("Could not convert method ${method.name} in $className: ${e.message}")
                null
            }
        }

        // Extract fields
        val fields = psiClass.fields.mapNotNull { field ->
            try {
                convertPsiFieldToFieldInfo(field)
            } catch (e: Exception) {
                logger.debug("Could not convert field ${field.name} in $className: ${e.message}")
                null
            }
        }

        return ClassInfo(
                name = className,
                packageName = packageName,
                sourceCode = sourceCode,
                annotations = annotations,
                methods = methods,
                fields = fields
        )
    }

    /**
     * Convert PSI method to MethodInfo format
     */
    private fun convertPsiMethodToMethodInfo(psiMethod: PsiMethod): MethodInfo {
        val methodName = psiMethod.name
        val signature = buildMethodSignature(psiMethod)
        val complexity = calculateMethodComplexity(psiMethod)

        val annotations = psiMethod.annotations.mapNotNull { annotation ->
            try {
                annotation.qualifiedName
            } catch (e: Exception) {
                logger.debug("Could not get annotation for method $methodName: ${e.message}")
                null
            }
        }

        return MethodInfo(
                name = methodName,
                signature = signature,
                annotations = annotations,
                complexity = complexity
        )
    }

    /**
     * Convert PSI field to FieldInfo format
     */
    private fun convertPsiFieldToFieldInfo(psiField: PsiField): FieldInfo {
        val fieldName = psiField.name
        val fieldType = getTypeText(psiField.type)

        val annotations = psiField.annotations.mapNotNull { annotation ->
            try {
                annotation.qualifiedName
            } catch (e: Exception) {
                logger.debug("Could not get annotation for field $fieldName: ${e.message}")
                null
            }
        }

        return FieldInfo(
                name = fieldName,
                type = fieldType,
                annotations = annotations
        )
    }

    /**
     * Build method signature string
     */
    private fun buildMethodSignature(method: PsiMethod): String {
        return try {
            val parameters = method.parameters.joinToString(", ") { param ->
                "${getTypeText(param.type as PsiType)} ${param.name}"
            }
            val returnType = method.returnType?.let { getTypeText(it) } ?: "void"
            val modifiers = method.modifierList?.text?.trim() ?: ""

            "$modifiers $returnType ${method.name}($parameters)".trim()
        } catch (e: Exception) {
            logger.debug("Could not build signature for ${method.name}: ${e.message}")
            "${method.name}(...)"
        }
    }

    /**
     * Calculate method complexity (cyclomatic complexity)
     */
    private fun calculateMethodComplexity(method: PsiMethod): Int {
        return try {
            var complexity = 1 // Base complexity
            val body = method.body ?: return complexity

            // Count control flow statements
            complexity += PsiTreeUtil.findChildrenOfType(body, PsiIfStatement::class.java).size * 2
            complexity += PsiTreeUtil.findChildrenOfType(body, PsiForStatement::class.java).size * 2
            complexity += PsiTreeUtil.findChildrenOfType(body, PsiWhileStatement::class.java).size * 2
            complexity += PsiTreeUtil.findChildrenOfType(body, PsiSwitchStatement::class.java).size * 2
            complexity += PsiTreeUtil.findChildrenOfType(body, PsiTryStatement::class.java).size

            // Cap complexity at 10 for readability
            minOf(complexity, 10)
        } catch (e: Exception) {
            logger.debug("Could not calculate complexity for ${method.name}: ${e.message}")
            1 // Default complexity
        }
    }

    /**
     * Get type text safely
     */
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

    /**
     * Get package name from PSI file
     */
    private fun getPackageName(psiFile: PsiFile): String {
        return try {
            (psiFile as? PsiJavaFile)?.packageName ?: ""
        } catch (e: Exception) {
            logger.debug("Could not get package name: ${e.message}")
            ""
        }
    }

    /**
     * Recursively collect all Java files in project
     */
    private fun collectJavaFiles(dir: VirtualFile, list: MutableList<VirtualFile>) {
        try {
            if (dir.isDirectory) {
                // Skip common non-source directories
                val dirName = dir.name
                if (dirName in listOf("target", "build", ".git", ".idea", "node_modules")) {
                    return
                }

                dir.children.forEach { child ->
                    collectJavaFiles(child, list)
                }
            } else if (dir.name.endsWith(".java") && dir.isValid) {
                list.add(dir)
            }
        } catch (e: Exception) {
            logger.debug("Could not process directory ${dir.name}: ${e.message}")
        }
    }

    /**
     * Get project statistics for logging
     */
    fun getProjectStats(): String {
        val classes = convertProjectToClassInfo()
        val totalMethods = classes.sumOf { it.methods.size }
        val totalFields = classes.sumOf { it.fields.size }
        val springClasses = classes.count { classInfo ->
            classInfo.annotations.any { it.contains("Component") || it.contains("Service") ||
                    it.contains("Repository") || it.contains("Controller") }
        }

        return """
            📊 Project Statistics:
            • Classes: ${classes.size}
            • Methods: $totalMethods
            • Fields: $totalFields
            • Spring Components: $springClasses
        """.trimIndent()
    }
}