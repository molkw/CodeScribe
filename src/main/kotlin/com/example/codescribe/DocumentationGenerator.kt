package com.example.codescribe

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope

class DocumentationGenerator(private val project: Project) {

    fun generateProjectDocumentation(): String {
        val baseDir = project.baseDir ?: return "❌ No base directory found."
        val result = StringBuilder()
        result.append("# 📚 Project Documentation: ${project.name ?: "Unnamed Project"}\n\n")
        val javaFiles = mutableListOf<VirtualFile>()
        collectJavaFiles(baseDir, javaFiles)
        if (javaFiles.isEmpty()) return "⚠️ No Java files found in project."
        for (file in javaFiles) {
            val doc = generateDocumentation(file)
            if (doc.isNotBlank()) {
                result.append(doc)
                result.append("\n\n---\n\n")
            }
        }
        return result.toString()
    }

    fun generateDocumentation(file: VirtualFile): String {
        val psiFile = PsiManager.getInstance(project).findFile(file) ?: return ""
        val classes = psiFile.children.filterIsInstance<PsiClass>()
        if (classes.isEmpty()) return ""
        val builder = StringBuilder()
        for (cls in classes) {
            builder.append("## 📝 ${file.name}\n")
            val packageName = JavaPsiFacade.getInstance(project).findPackage(cls.qualifiedName ?: "")?.qualifiedName
            if (!packageName.isNullOrEmpty()) builder.append("📍 *Location: $packageName*\n\n")
            builder.append("### 🤔 What This Does\n")
            builder.append(describeClass(cls)).append("\n\n")
            builder.append("✨ Key Features\n")
            builder.append(listFeatures(cls)).append("\n\n")
            builder.append("🛠️ How It Works\n")
            builder.append(describeImplementation(cls)).append("\n\n")
            builder.append("📚 Beginner's Explanation\n")
            builder.append(simpleAnalogy(cls)).append("\n\n")
            builder.append("🔍 Code Example\n```java\n")
            builder.append(extractClassHeader(cls)).append("\n```\n")
        }
        return builder.toString()
    }

    private fun collectJavaFiles(dir: VirtualFile, list: MutableList<VirtualFile>) {
        if (dir.isDirectory) {
            dir.children.forEach { collectJavaFiles(it, list) }
        } else if (dir.name.endsWith(".java")) {
            list.add(dir)
        }
    }

    private fun describeClass(cls: PsiClass): String {
        return when {
            cls.annotations.any { it.qualifiedName?.contains("RestController") == true } -> "This class is a Spring REST controller exposing HTTP endpoints."
            cls.annotations.any { it.qualifiedName?.contains("Service") == true } -> "This class is a service that handles business logic."
            cls.annotations.any { it.qualifiedName?.contains("Repository") == true } -> "This class is responsible for database operations."
            cls.annotations.any { it.qualifiedName?.contains("Configuration") == true } -> "This class provides Spring Boot configuration."
            cls.annotations.any { it.qualifiedName?.contains("Component") == true } -> "This is a Spring component managed by the application context."
            else -> "This class contributes functionality to the Spring application."
        }
    }

    private fun listFeatures(cls: PsiClass): String {
        val features = mutableListOf<String>()
        cls.annotations.forEach {
            features.add("- Annotation: @${it.nameReferenceElement?.text}")
        }
        cls.methods.forEach {
            features.add("- Method: ${it.name}() - ${describeMethod(it)}")
        }
        return if (features.isEmpty()) "No specific features detected" else features.joinToString("\n")
    }

    private fun describeMethod(method: PsiMethod): String {
        return when {
            method.name.startsWith("get") -> "Returns data"
            method.name.startsWith("set") -> "Updates data"
            method.name.startsWith("save") -> "Persists data"
            method.name.startsWith("delete") -> "Deletes data"
            method.name.startsWith("update") -> "Modifies data"
            method.name.startsWith("create") -> "Initializes resources"
            else -> "Executes specific logic"
        }
    }

    private fun describeImplementation(cls: PsiClass): String {
        return when {
            cls.annotations.any { it.qualifiedName?.contains("RestController") == true } ->
                "Spring maps HTTP requests to methods in this controller using annotations like @GetMapping or @PostMapping."
            cls.annotations.any { it.qualifiedName?.contains("Service") == true } ->
                "Methods in this class perform business logic and are reused across multiple components."
            cls.annotations.any { it.qualifiedName?.contains("Repository") == true } ->
                "This class interacts with the database using Spring Data methods or custom queries."
            cls.annotations.any { it.qualifiedName?.contains("Configuration") == true } ->
                "This class defines beans and settings loaded during application startup."
            else -> "Implements behavior according to its role in the application."
        }
    }

    private fun simpleAnalogy(cls: PsiClass): String {
        return when {
            cls.annotations.any { it.qualifiedName?.contains("RestController") == true } -> "Like a receptionist handling and responding to incoming messages."
            cls.annotations.any { it.qualifiedName?.contains("Service") == true } -> "Like a chef preparing the meal behind the scenes."
            cls.annotations.any { it.qualifiedName?.contains("Repository") == true } -> "Like a librarian helping you find and store books."
            cls.annotations.any { it.qualifiedName?.contains("Configuration") == true } -> "Like a setup guide that configures how things start up."
            else -> "Like a tool used behind the scenes to support other components."
        }
    }

    private fun extractClassHeader(cls: PsiClass): String {
        return cls.text.lines().take(10).joinToString("\n")
    }
}
