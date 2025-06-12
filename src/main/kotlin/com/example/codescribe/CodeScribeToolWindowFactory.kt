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
                val generator = DocumentationGenerator(project)
                // Wrap PSI operations in read action
                val fullDocumentation = com.intellij.openapi.application.ApplicationManager.getApplication().runReadAction<String> {
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
        var currentSection = "overview"
        var currentContent = StringBuilder()

        for (line in lines) {
            when {
                line.startsWith("# 📚 Advanced Project Analysis") -> {
                    currentSection = "overview"
                    currentContent.append(line).append("\n")
                }
                line.startsWith("## 🎯") -> {
                    if (currentContent.isNotEmpty()) {
                        sections.getOrPut(currentSection) { mutableListOf() }.add(currentContent.toString())
                    }
                    currentSection = "classDetails"
                    currentContent = StringBuilder(line).append("\n")
                }
                line.startsWith("### 🔍 Design Patterns") -> {
                    currentSection = "patterns"
                    currentContent.append(line).append("\n")
                }
                line.startsWith("### 🔗 Class Relationships") -> {
                    currentSection = "relationships"
                    currentContent.append(line).append("\n")
                }
                line.startsWith("### 💡 Code Quality Insights") -> {
                    currentSection = "insights"
                    currentContent.append(line).append("\n")
                }
                line.startsWith("# 📊 Project Summary") -> {
                    if (currentContent.isNotEmpty()) {
                        sections.getOrPut(currentSection) { mutableListOf() }.add(currentContent.toString())
                    }
                    currentSection = "overview"
                    currentContent = StringBuilder(line).append("\n")
                }
                else -> {
                    currentContent.append(line).append("\n")
                }
            }
        }

        // Add the last section
        if (currentContent.isNotEmpty()) {
            sections.getOrPut(currentSection) { mutableListOf() }.add(currentContent.toString())
        }

        return sections
    }

    private fun buildOverviewContent(sections: Map<String, List<String>>): String {
        val overview = StringBuilder()
        overview.append("🎯 CODESCRIBE ANALYSIS COMPLETE!\n")
        overview.append("=".repeat(50)).append("\n\n")

        // Add project summary
        sections["overview"]?.forEach { section ->
            if (section.contains("Project Summary")) {
                overview.append(section).append("\n")
            }
        }

        // Add quick stats
        val classCount = sections["classDetails"]?.size ?: 0
        val patternCount = sections["patterns"]?.sumOf { it.split("Design Patterns Detected").size - 1 } ?: 0
        val relationshipCount = sections["relationships"]?.sumOf { it.split("Class Relationships").size - 1 } ?: 0

        overview.append("📈 QUICK STATISTICS\n")
        overview.append("-".repeat(20)).append("\n")
        overview.append("Classes Analyzed: $classCount\n")
        overview.append("Design Patterns Found: $patternCount\n")
        overview.append("Relationships Mapped: $relationshipCount\n\n")

        overview.append("💡 NEXT STEPS\n")
        overview.append("-".repeat(15)).append("\n")
        overview.append("• Check 'Class Details' tab for in-depth analysis\n")
        overview.append("• Review 'Patterns' tab for design pattern insights\n")
        overview.append("• Explore 'Relationships' tab for architecture overview\n")
        overview.append("• Read 'Insights' tab for improvement recommendations\n")

        return overview.toString()
    }

    private fun buildClassDetailsContent(sections: Map<String, List<String>>): String {
        val details = StringBuilder()
        details.append("🎯 DETAILED CLASS ANALYSIS\n")
        details.append("=".repeat(40)).append("\n\n")

        sections["classDetails"]?.forEach { classSection ->
            details.append(classSection).append("\n")
        }

        return details.toString()
    }

    private fun buildPatternsContent(sections: Map<String, List<String>>): String {
        val patterns = StringBuilder()
        patterns.append("🔍 DESIGN PATTERNS DETECTED\n")
        patterns.append("=".repeat(35)).append("\n\n")

        // Extract pattern information from all sections
        sections.values.flatten().forEach { section ->
            if (section.contains("Design Patterns Detected")) {
                val patternSection = section.substringAfter("Design Patterns Detected")
                        .substringBefore("### 🔗")
                        .substringBefore("### 💡")
                        .substringBefore("### 🛠️")
                patterns.append(patternSection).append("\n")
            }
        }

        if (patterns.length <= 50) {
            patterns.append("ℹ️ No specific design patterns detected in this analysis.\n")
            patterns.append("This doesn't mean the code is bad - it might be using simpler patterns\n")
            patterns.append("or following domain-specific architectural styles.\n")
        }

        return patterns.toString()
    }

    private fun buildRelationshipsContent(sections: Map<String, List<String>>): String {
        val relationships = StringBuilder()
        relationships.append("🔗 CLASS RELATIONSHIPS\n")
        relationships.append("=".repeat(30)).append("\n\n")

        // Extract relationship information
        sections.values.flatten().forEach { section ->
            if (section.contains("Class Relationships")) {
                val relationshipSection = section.substringAfter("Class Relationships")
                        .substringBefore("### 🛠️")
                        .substringBefore("### 💡")
                        .substringBefore("### 📋")
                relationships.append(relationshipSection).append("\n")
            }
        }

        return relationships.toString()
    }

    private fun buildInsightsContent(sections: Map<String, List<String>>): String {
        val insights = StringBuilder()
        insights.append("💡 CODE QUALITY INSIGHTS & RECOMMENDATIONS\n")
        insights.append("=".repeat(50)).append("\n\n")

        // Extract insights from all sections
        sections.values.flatten().forEach { section ->
            if (section.contains("Code Quality Insights")) {
                val insightsSection = section.substringAfter("Code Quality Insights")
                        .substringBefore("### 💭")
                        .substringBefore("### 🎯")
                insights.append(insightsSection).append("\n")
            }
            if (section.contains("Developer Notes")) {
                val notesSection = section.substringAfter("Developer Notes")
                insights.append(notesSection).append("\n")
            }
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