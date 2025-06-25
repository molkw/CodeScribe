package com.example.codescribe

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Font
import javax.swing.*

class CodeScribeToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()
        val toolWindowContent = CodeScribeToolWindowContent(project)
        val content = contentFactory.createContent(toolWindowContent.getContentPanel(), "CodeScribe", false)
        toolWindow.contentManager.addContent(content)
    }
}

class CodeScribeToolWindowContent(private val project: Project) {
    private val contentPanel = JPanel(BorderLayout())
    private val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT))
    private val generateButton = JButton("🔍 Analyze Project")
    private val clearButton = JButton("🗑️ Clear")
    private val tabbedPane = JBTabbedPane()

    // Different tabs for different types of analysis
    private val overviewArea = JTextArea()
    private val classDetailsArea = JTextArea()
    private val patternsArea = JTextArea()
    private val relationshipsArea = JTextArea()
    private val insightsArea = JTextArea()

    init {
        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        // Make text areas non-editable
        listOf(overviewArea, classDetailsArea, patternsArea, relationshipsArea, insightsArea).forEach { area ->
            area.isEditable = false
            area.font = Font("Monospaced", Font.PLAIN, 12)
            area.lineWrap = true
            area.wrapStyleWord = true
        }

        // Add tabs
        tabbedPane.addTab("📊 Overview", JBScrollPane(overviewArea))
        tabbedPane.addTab("🎯 Class Details", JBScrollPane(classDetailsArea))
        tabbedPane.addTab("🔍 Patterns", JBScrollPane(patternsArea))
        tabbedPane.addTab("🔗 Relationships", JBScrollPane(relationshipsArea))
        tabbedPane.addTab("💡 Insights", JBScrollPane(insightsArea))

        // Setup buttons
        generateButton.toolTipText = "Analyze the entire project for patterns, relationships, and code quality"
        clearButton.toolTipText = "Clear all analysis results"

        buttonPanel.add(generateButton)
        buttonPanel.add(clearButton)

        contentPanel.add(buttonPanel, BorderLayout.NORTH)
        contentPanel.add(tabbedPane, BorderLayout.CENTER)

        // Add status label
        val statusLabel = JLabel("Ready to analyze project...")
        statusLabel.font = Font("SansSerif", Font.ITALIC, 11)
        contentPanel.add(statusLabel, BorderLayout.SOUTH)
    }

    private fun setupListeners() {
        generateButton.addActionListener {
            generateAdvancedDocumentation()
        }

        clearButton.addActionListener {
            clearAllTabs()
        }
    }

    private fun generateAdvancedDocumentation() {
        // Disable button during analysis
        generateButton.isEnabled = false
        generateButton.text = "🔄 Analyzing..."

        // Clear previous results
        clearAllTabs()

        // Show progress in overview
        overviewArea.text = "🔍 Analyzing project structure...\n\n" +
                "📁 Scanning for Java files...\n" +
                "🧠 Performing deep code analysis...\n" +
                "🔍 Detecting design patterns...\n" +
                "🔗 Mapping class relationships...\n" +
                "💡 Generating insights...\n\n" +
                "Please wait..."

        // Run analysis in background thread with proper read action
        Thread {
            try {
                // Wrap ALL PSI operations in read action
                val fullDocumentation = ApplicationManager.getApplication().runReadAction<String> {
                    val generator = DocumentationGenerator(project)
                    generator.generateProjectDocumentation()
                }

                // Parse and distribute content to different tabs
                SwingUtilities.invokeLater {
                    distributeContentToTabs(fullDocumentation)
                    generateButton.text = "🔍 Analyze Project"
                    generateButton.isEnabled = true
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    overviewArea.text = "❌ Error during analysis: ${e.message}\n\n" +
                            "Stack trace:\n${e.stackTraceToString()}"
                    generateButton.text = "🔍 Analyze Project"
                    generateButton.isEnabled = true
                }
                e.printStackTrace()
            }
        }.start()
    }

    private fun distributeContentToTabs(fullDocumentation: String) {
        val sections = parseDocumentationSections(fullDocumentation)

        // Overview tab - project summary and statistics
        overviewArea.text = buildOverviewContent(sections)

        // Class details tab - detailed class analysis
        classDetailsArea.text = buildClassDetailsContent(sections)

        // Patterns tab - design patterns found
        patternsArea.text = buildPatternsContent(sections)

        // Relationships tab - class relationships
        relationshipsArea.text = buildRelationshipsContent(sections)

        // Insights tab - code quality insights and recommendations
        insightsArea.text = buildInsightsContent(sections)
    }

    private fun parseDocumentationSections(documentation: String): Map<String, List<String>> {
        val sections = mutableMapOf<String, MutableList<String>>()
        val lines = documentation.lines()
        var currentClassContent = StringBuilder()
        var currentSection = "overview"
        var currentContent = StringBuilder()

        for (line in lines) {
            when {
                line.startsWith("# 📚 Advanced Project Analysis") -> {
                    // Start of overview section
                    currentSection = "overview"
                    currentContent.append(line).append("\n")
                }
                line.startsWith("## 🎯") -> {
                    // Save previous class content if exists
                    if (currentClassContent.isNotEmpty()) {
                        sections.getOrPut("classDetails") { mutableListOf() }.add(currentClassContent.toString())
                    }
                    // Start new class content
                    currentClassContent = StringBuilder(line).append("\n")
                }
                line.startsWith("# 📊 Project Summary") -> {
                    // Save any remaining class content
                    if (currentClassContent.isNotEmpty()) {
                        sections.getOrPut("classDetails") { mutableListOf() }.add(currentClassContent.toString())
                        currentClassContent.clear()
                    }
                    // Save any other content
                    if (currentContent.isNotEmpty()) {
                        sections.getOrPut(currentSection) { mutableListOf() }.add(currentContent.toString())
                    }
                    // Start project summary
                    currentSection = "overview"
                    currentContent = StringBuilder(line).append("\n")
                }
                line.startsWith("=".repeat(80)) -> {
                    // Class separator - save current class content
                    if (currentClassContent.isNotEmpty()) {
                        sections.getOrPut("classDetails") { mutableListOf() }.add(currentClassContent.toString())
                        currentClassContent.clear()
                    }
                }
                else -> {
                    // Regular line - add to appropriate section
                    if (currentClassContent.isNotEmpty()) {
                        // We're inside a class section
                        currentClassContent.append(line).append("\n")

                        // Also categorize specific subsections for other tabs
                        when {
                            line.startsWith("### 🔍 Design Patterns") ||
                                    line.contains("Pattern:") -> {
                                sections.getOrPut("patterns") { mutableListOf() }.add(line)
                            }
                            line.startsWith("### 🔗 Class Relationships") ||
                                    line.contains("**Inheritance") || line.contains("**Implementation") ||
                                    line.contains("**Dependency") || line.contains("**Usage") -> {
                                sections.getOrPut("relationships") { mutableListOf() }.add(line)
                            }
                            line.startsWith("### 💡 Code Quality Insights") ||
                                    line.contains("⚠️") || line.contains("💡") -> {
                                sections.getOrPut("insights") { mutableListOf() }.add(line)
                            }
                        }
                    } else {
                        // We're in overview or other sections
                        currentContent.append(line).append("\n")
                    }
                }
            }
        }

        // Add any remaining content
        if (currentClassContent.isNotEmpty()) {
            sections.getOrPut("classDetails") { mutableListOf() }.add(currentClassContent.toString())
        }
        if (currentContent.isNotEmpty()) {
            sections.getOrPut(currentSection) { mutableListOf() }.add(currentContent.toString())
        }

        return sections
    }

    private fun buildOverviewContent(sections: Map<String, List<String>>): String {
        val overview = StringBuilder()
        overview.append("🎯 CODESCRIBE ANALYSIS COMPLETE!\n")
        overview.append("=".repeat(50)).append("\n\n")

        // Add project summary content
        sections["overview"]?.forEach { section ->
            if (section.contains("Project Summary") || section.contains("Advanced Project Analysis")) {
                // Clean up debug lines and focus on summary content
                val cleanSection = section.lines()
                        .filter { line ->
                            !line.contains("🔍 DEBUG:") &&
                                    line.trim().isNotEmpty()
                        }
                        .joinToString("\n")
                overview.append(cleanSection).append("\n")
            }
        }

        // Add quick statistics
        val classCount = sections["classDetails"]?.size ?: 0
        val totalPatterns = sections["classDetails"]?.sumOf { section ->
            section.split("### 🔍 Design Patterns Detected").size - 1
        } ?: 0
        val totalRelationships = sections["classDetails"]?.sumOf { section ->
            section.split("### 🔗 Class Relationships").size - 1
        } ?: 0

        overview.append("\n📈 ANALYSIS STATISTICS\n")
        overview.append("-".repeat(25)).append("\n")
        overview.append("Classes Analyzed: $classCount\n")
        overview.append("Design Patterns Found: $totalPatterns\n")
        overview.append("Relationship Mappings: $totalRelationships\n\n")

        overview.append("💡 NAVIGATION GUIDE\n")
        overview.append("-".repeat(20)).append("\n")
        overview.append("• 'Class Details' - In-depth analysis of each class\n")
        overview.append("• 'Patterns' - Design patterns discovered in your code\n")
        overview.append("• 'Relationships' - How your classes interact\n")
        overview.append("• 'Insights' - Code quality recommendations\n")

        return overview.toString()
    }

    private fun buildClassDetailsContent(sections: Map<String, List<String>>): String {
        val details = StringBuilder()
        details.append("🎯 DETAILED CLASS ANALYSIS\n")
        details.append("=".repeat(40)).append("\n\n")

        sections["classDetails"]?.forEach { classSection ->
            // Clean up the class section content
            val cleanedSection = classSection.lines()
                    .filter { it.trim().isNotEmpty() }
                    .joinToString("\n")

            if (cleanedSection.isNotEmpty()) {
                details.append(cleanedSection).append("\n\n")
                details.append("-".repeat(60)).append("\n\n")
            }
        }

        if (details.length <= 100) {
            details.append("ℹ️ No class details available.\n")
            details.append("This could mean:\n")
            details.append("• No Java classes found in the project\n")
            details.append("• Classes couldn't be analyzed\n")
            details.append("• Parsing errors occurred\n")
        }

        return details.toString()
    }

    private fun buildPatternsContent(sections: Map<String, List<String>>): String {
        val patterns = StringBuilder()
        patterns.append("🔍 DESIGN PATTERNS DETECTED\n")
        patterns.append("=".repeat(35)).append("\n\n")

        var hasContent = false

        // Extract pattern information from class details sections
        sections["classDetails"]?.forEach { classSection ->
            val lines = classSection.lines()
            var inPatternSection = false
            var currentClassTitle = ""

            for (line in lines) {
                when {
                    line.startsWith("## 🎯") -> {
                        currentClassTitle = line.removePrefix("## 🎯 ").trim()
                        inPatternSection = false
                    }
                    line.startsWith("### 🔍 Design Patterns Detected") -> {
                        inPatternSection = true
                        if (currentClassTitle.isNotEmpty()) {
                            patterns.append("**$currentClassTitle:**\n")
                            hasContent = true
                        }
                    }
                    line.startsWith("###") && !line.contains("🔍") -> {
                        inPatternSection = false
                    }
                    inPatternSection && line.trim().startsWith("- **") -> {
                        patterns.append("  ${line.trim()}\n")
                        hasContent = true
                    }
                }
            }
            if (hasContent && inPatternSection) {
                patterns.append("\n")
            }
        }

        // Also check project summary for architectural patterns
        sections["overview"]?.forEach { overviewSection ->
            if (overviewSection.contains("🏗️ Architecture Patterns")) {
                patterns.append("🏗️ **Project-Level Architecture Patterns:**\n")
                val lines = overviewSection.lines()
                var inArchPatterns = false

                for (line in lines) {
                    when {
                        line.contains("🏗️ Architecture Patterns") -> inArchPatterns = true
                        line.startsWith("###") && !line.contains("🏗️") -> inArchPatterns = false
                        inArchPatterns && line.trim().startsWith("- **") -> {
                            patterns.append("  ${line.trim()}\n")
                            hasContent = true
                        }
                    }
                }
                patterns.append("\n")
            }
        }

        if (!hasContent) {
            patterns.append("ℹ️ No specific design patterns detected in this analysis.\n")
            patterns.append("This doesn't mean the code is bad - it might be using simpler patterns\n")
            patterns.append("or following domain-specific architectural styles that aren't covered\n")
            patterns.append("by traditional GoF design patterns.\n")
        }

        return patterns.toString()
    }

    private fun buildRelationshipsContent(sections: Map<String, List<String>>): String {
        val relationships = StringBuilder()
        relationships.append("🔗 CLASS RELATIONSHIPS\n")
        relationships.append("=".repeat(30)).append("\n\n")

        var hasContent = false

        // Extract relationship information from class details sections
        sections["classDetails"]?.forEach { classSection ->
            val lines = classSection.lines()
            var inRelationshipSection = false
            var currentClassTitle = ""

            for (line in lines) {
                when {
                    line.startsWith("## 🎯") -> {
                        currentClassTitle = line.removePrefix("## 🎯 ").trim()
                        inRelationshipSection = false
                    }
                    line.startsWith("### 🔗 Class Relationships") -> {
                        inRelationshipSection = true
                        if (currentClassTitle.isNotEmpty()) {
                            relationships.append("**$currentClassTitle:**\n")
                            hasContent = true
                        }
                    }
                    line.startsWith("###") && !line.contains("🔗") -> {
                        inRelationshipSection = false
                    }
                    inRelationshipSection && line.trim().startsWith("- **") -> {
                        relationships.append("  ${line.trim()}\n")
                        hasContent = true
                    }
                }
            }
            if (hasContent && inRelationshipSection) {
                relationships.append("\n")
            }
        }

        if (!hasContent) {
            relationships.append("ℹ️ No class relationships detected in this analysis.\n")
            relationships.append("This could mean:\n")
            relationships.append("• Classes are mostly independent\n")
            relationships.append("• Simple project structure\n")
            relationships.append("• Limited inheritance or composition patterns\n")
        }

        return relationships.toString()
    }

    private fun buildInsightsContent(sections: Map<String, List<String>>): String {
        val insights = StringBuilder()
        insights.append("💡 CODE QUALITY INSIGHTS & RECOMMENDATIONS\n")
        insights.append("=".repeat(50)).append("\n\n")

        var hasContent = false

        // Extract insights from class details sections
        sections["classDetails"]?.forEach { classSection ->
            val lines = classSection.lines()
            var inInsightsSection = false
            var inDeveloperNotesSection = false
            var currentClassTitle = ""

            for (line in lines) {
                when {
                    line.startsWith("## 🎯") -> {
                        currentClassTitle = line.removePrefix("## 🎯 ").trim()
                        inInsightsSection = false
                        inDeveloperNotesSection = false
                    }
                    line.startsWith("### 💡 Code Quality Insights") -> {
                        inInsightsSection = true
                        inDeveloperNotesSection = false
                        if (currentClassTitle.isNotEmpty()) {
                            insights.append("**$currentClassTitle:**\n")
                            hasContent = true
                        }
                    }
                    line.startsWith("### 💭 Developer Notes") -> {
                        inDeveloperNotesSection = true
                        inInsightsSection = false
                    }
                    line.startsWith("###") && !line.contains("💡") && !line.contains("💭") -> {
                        inInsightsSection = false
                        inDeveloperNotesSection = false
                    }
                    (inInsightsSection || inDeveloperNotesSection) && line.trim().startsWith("- ") -> {
                        insights.append("  ${line.trim()}\n")
                        hasContent = true
                    }
                    inDeveloperNotesSection && line.trim().isNotEmpty() && !line.startsWith("#") -> {
                        insights.append("  • ${line.trim()}\n")
                        hasContent = true
                    }
                }
            }
            if (hasContent && (inInsightsSection || inDeveloperNotesSection)) {
                insights.append("\n")
            }
        }

        if (!hasContent) {
            insights.append("ℹ️ No specific code quality issues detected.\n")
            insights.append("This suggests:\n")
            insights.append("• Well-structured code\n")
            insights.append("• Good coding practices followed\n")
            insights.append("• Classes have appropriate complexity levels\n")
        }

        return insights.toString()
    }

    private fun clearAllTabs() {
        overviewArea.text = ""
        classDetailsArea.text = ""
        patternsArea.text = ""
        relationshipsArea.text = ""
        insightsArea.text = ""
    }

    fun getContentPanel(): JPanel {
        return contentPanel
    }
}