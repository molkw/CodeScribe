package com.example.codescribe

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import java.util.*

class DocumentationGenerator(private val project: Project) {
    private val codeAnalyzer = AdvancedCodeAnalyzer()
    private val patternDetector = PatternDetector()
    private val relationshipAnalyzer = CodeRelationshipAnalyzer()

    fun generateProjectDocumentation(): String {
        val baseDir = project.baseDir ?: return "❌ No base directory found."
        val result = StringBuilder()
        result.append("# 📚 Advanced Project Analysis: ${project.name ?: "Unnamed Project"}\n\n")

        val javaFiles = mutableListOf<VirtualFile>()
        collectJavaFiles(baseDir, javaFiles)

        if (javaFiles.isEmpty()) return "⚠️ No Java files found in project."

        // First pass: collect all classes for relationship analysis
        val allClasses = mutableListOf<PsiClass>()
        javaFiles.forEach { file ->
            val psiFile = PsiManager.getInstance(project).findFile(file)
            psiFile?.let {
                allClasses.addAll(PsiTreeUtil.findChildrenOfType(it, PsiClass::class.java))
            }
        }

        // Build relationships
        relationshipAnalyzer.buildRelationships(allClasses)

        // Generate documentation for each file
        for (file in javaFiles) {
            val doc = generateAdvancedDocumentation(file)
            if (doc.isNotBlank()) {
                result.append(doc)
                result.append("\n\n" + "=".repeat(80) + "\n\n")
            }
        }

        // Add project summary
        result.append(generateProjectSummary(allClasses))

        return result.toString()
    }

    private fun generateAdvancedDocumentation(file: VirtualFile): String {
        return ApplicationManager.getApplication().runReadAction<String> {
            val psiFile = PsiManager.getInstance(project).findFile(file) ?: return@runReadAction ""
            val classes = PsiTreeUtil.findChildrenOfType(psiFile, PsiClass::class.java).toList()

            if (classes.isEmpty()) return@runReadAction ""

            val builder = StringBuilder()

            for (cls in classes) {
                builder.append("## 🎯 ${cls.name} (${file.name})\n\n")

                // Package and location info
                val packageName = getPackageName(psiFile)
                if (packageName.isNotEmpty()) {
                    builder.append("📍 **Package:** `$packageName`\n\n")
                }

                // Class type and purpose analysis
                val classAnalysis = codeAnalyzer.analyzeClass(cls)
                builder.append("### 🧠 Intelligent Analysis\n")
                builder.append("**Purpose:** ${classAnalysis.purpose}\n\n")
                builder.append("**Complexity Score:** ${classAnalysis.complexityScore}/10\n\n")

                // Design patterns detected
                val patterns = patternDetector.detectPatterns(cls)
                if (patterns.isNotEmpty()) {
                    builder.append("### 🔍 Design Patterns Detected\n")
                    patterns.forEach { pattern ->
                        builder.append("- **${pattern.name}:** ${pattern.description}\n")
                    }
                    builder.append("\n")
                }

                // Dependencies and relationships
                val relationships = relationshipAnalyzer.getRelationships(cls)
                if (relationships.isNotEmpty()) {
                    builder.append("### 🔗 Class Relationships\n")
                    relationships.forEach { rel ->
                        builder.append("- **${rel.type}:** ${rel.targetClass} - ${rel.description}\n")
                    }
                    builder.append("\n")
                }

                // Method analysis
                val methods = cls.methods.filter { !it.isConstructor }
                if (methods.isNotEmpty()) {
                    builder.append("### 🛠️ Method Analysis\n")
                    methods.forEach { method ->
                        val methodAnalysis = codeAnalyzer.analyzeMethod(method)
                        builder.append("**${method.name}()** (Complexity: ${methodAnalysis.complexity})\n")
                        builder.append("- ${methodAnalysis.description}\n")
                        if (methodAnalysis.patterns.isNotEmpty()) {
                            builder.append("- Patterns: ${methodAnalysis.patterns.joinToString(", ")}\n")
                        }
                        if (methodAnalysis.potentialIssues.isNotEmpty()) {
                            builder.append("- ⚠️ Issues: ${methodAnalysis.potentialIssues.joinToString(", ")}\n")
                        }
                        builder.append("\n")
                    }
                }

                // Field analysis
                val fields = cls.fields
                if (fields.isNotEmpty()) {
                    builder.append("### 📋 Field Analysis\n")
                    fields.forEach { field ->
                        val fieldAnalysis = codeAnalyzer.analyzeField(field)
                        builder.append("- **${field.name}:** ${fieldAnalysis}\n")
                    }
                    builder.append("\n")
                }

                // Code quality insights
                val qualityInsights = codeAnalyzer.analyzeCodeQuality(cls)
                if (qualityInsights.isNotEmpty()) {
                    builder.append("### 💡 Code Quality Insights\n")
                    qualityInsights.forEach { insight ->
                        builder.append("- ${insight}\n")
                    }
                    builder.append("\n")
                }

                // Usage examples and recommendations
                builder.append("### 💭 Developer Notes\n")
                builder.append("${generateDeveloperNotes(cls, classAnalysis)}\n\n")
            }

            builder.toString()
        }
    }

    private fun generateProjectSummary(allClasses: List<PsiClass>): String {
        val builder = StringBuilder()
        builder.append("# 📊 Project Summary\n\n")

        val totalClasses = allClasses.size
        val avgComplexity = allClasses.mapNotNull {
            codeAnalyzer.analyzeClass(it).complexityScore
        }.average()

        builder.append("**Total Classes:** $totalClasses\n")
        builder.append("**Average Complexity:** ${String.format("%.1f", avgComplexity)}/10\n\n")

        // Architecture analysis
        val architecturalPatterns = patternDetector.detectArchitecturalPatterns(allClasses)
        if (architecturalPatterns.isNotEmpty()) {
            builder.append("### 🏗️ Architecture Patterns\n")
            architecturalPatterns.forEach { pattern ->
                builder.append("- **${pattern.name}:** ${pattern.description}\n")
            }
            builder.append("\n")
        }

        // Hot spots (high complexity classes)
        val hotSpots = allClasses.filter {
            codeAnalyzer.analyzeClass(it).complexityScore >= 7
        }
        if (hotSpots.isNotEmpty()) {
            builder.append("### 🔥 Complexity Hot Spots\n")
            builder.append("Consider refactoring these classes:\n")
            hotSpots.forEach { cls ->
                val score = codeAnalyzer.analyzeClass(cls).complexityScore
                builder.append("- **${cls.name}** (${score}/10)\n")
            }
            builder.append("\n")
        }

        return builder.toString()
    }

    private fun generateDeveloperNotes(cls: PsiClass, analysis: ClassAnalysisResult): String {
        val notes = mutableListOf<String>()

        if (analysis.complexityScore >= 7) {
            notes.add("🔴 High complexity - consider breaking into smaller classes")
        } else if (analysis.complexityScore >= 4) {
            notes.add("🟡 Moderate complexity - ensure good test coverage")
        } else {
            notes.add("🟢 Low complexity - well-structured class")
        }

        if (cls.methods.size > 10) {
            notes.add("📝 Large number of methods - consider if class has single responsibility")
        }

        if (cls.fields.size > 8) {
            notes.add("📦 Many fields - ensure proper encapsulation")
        }

        return notes.joinToString("\n")
    }

    private fun collectJavaFiles(dir: VirtualFile, list: MutableList<VirtualFile>) {
        if (dir.isDirectory) {
            dir.children.forEach { collectJavaFiles(it, list) }
        } else if (dir.name.endsWith(".java")) {
            list.add(dir)
        }
    }

    private fun getPackageName(psiFile: PsiFile): String {
        return (psiFile as? PsiJavaFile)?.packageName ?: ""
    }
}