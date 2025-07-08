// src/main/kotlin/com/example/codescribe/CodeScribeToolWindowFactory.kt
// REPLACE your existing file with this final version

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

    // Main controls
    private val analyzeButton = JButton("🤖 AI Analysis")
    private val statusButton = JButton("📡 Check Status")
    private val clearButton = JButton("🗑️ Clear")

    // Content tabs
    private val tabbedPane = JBTabbedPane()
    private val overviewArea = JTextArea()
    private val classDetailsArea = JTextArea()
    private val patternsArea = JTextArea()
    private val relationshipsArea = JTextArea()
    private val insightsArea = JTextArea()

    // Status display
    private val statusLabel = JLabel("Ready to analyze project...")

    // Generator
    private val generator = AIIntegratedGenerator(project)

    init {
        setupUI()
        setupListeners()
        checkInitialStatus()
    }

    private fun setupUI() {
        // Configure text areas
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
        analyzeButton.toolTipText = "Analyze project with AI + advanced static analysis"
        statusButton.toolTipText = "Check AI backend connection status"
        clearButton.toolTipText = "Clear all analysis results"

        buttonPanel.add(analyzeButton)
        buttonPanel.add(statusButton)
        buttonPanel.add(clearButton)

        // Layout
        contentPanel.add(buttonPanel, BorderLayout.NORTH)
        contentPanel.add(tabbedPane, BorderLayout.CENTER)

        statusLabel.font = Font("SansSerif", Font.ITALIC, 11)
        contentPanel.add(statusLabel, BorderLayout.SOUTH)
    }

    private fun setupListeners() {
        analyzeButton.addActionListener {
            runAnalysis()
        }

        statusButton.addActionListener {
            checkAIStatus()
        }

        clearButton.addActionListener {
            clearAllTabs()
            statusLabel.text = "Ready to analyze project..."
        }
    }

    private fun checkInitialStatus() {
        // Show project summary and initial status
        statusLabel.text = generator.getProjectSummary()
        checkAIStatus()
    }

    private fun checkAIStatus() {
        statusButton.isEnabled = false
        statusButton.text = "⏳ Checking..."

        Thread {
            val aiService = AIDocumentationService()
            val status = aiService.getServiceStatus()
            val isAvailable = aiService.isServiceAvailable()

            SwingUtilities.invokeLater {
                statusLabel.text = status
                statusButton.text = "📡 Check Status"
                statusButton.isEnabled = true

                // Update analyze button based on AI availability
                analyzeButton.text = if (isAvailable) {
                    "🤖 AI Analysis"
                } else {
                    "📊 Static Analysis"
                }

                analyzeButton.toolTipText = if (isAvailable) {
                    "AI-enhanced analysis with qwen2.5-coder + static analysis"
                } else {
                    "AI unavailable - using advanced static analysis only"
                }
            }
        }.start()
    }

    private fun runAnalysis() {
        analyzeButton.isEnabled = false
        val originalText = analyzeButton.text
        analyzeButton.text = "🔄 Analyzing..."

        clearAllTabs()

        // Show progress
        overviewArea.text = """
            🚀 Starting comprehensive project analysis...
            
            📁 Scanning Java source files...
            🔍 Extracting classes, methods, and annotations...
            🤖 Sending to AI backend for analysis...
            🧠 Processing with qwen2.5-coder model...
            📊 Generating insights and recommendations...
            
            Please wait... This may take 30-90 seconds depending on project size.
        """.trimIndent()

        // Run analysis in background with proper read action
        Thread {
            try {
                val documentation = ApplicationManager.getApplication().runReadAction<String> {
                    generator.generateDocumentation()
                }

                SwingUtilities.invokeLater {
                    distributeContentToTabs(documentation)
                    analyzeButton.text = originalText
                    analyzeButton.isEnabled = true
                    statusLabel.text = "✅ Analysis completed successfully!"
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    overviewArea.text = """
                        ❌ Analysis Error: ${e.message}
                        
                        Possible causes:
                        • Project not fully loaded/indexed by IntelliJ
                        • AI backend connection issues
                        • Invalid project structure
                        
                        Solutions:
                        1. Wait for IntelliJ indexing to complete
                        2. Check AI backend: curl http://localhost:8000/api/v1/health
                        3. Restart AI backend: python main.py
                        4. Try again with a smaller project first
                        
                        Detailed error:
                        ${e.stackTraceToString()}
                    """.trimIndent()

                    analyzeButton.text = originalText
                    analyzeButton.isEnabled = true
                    statusLabel.text = "❌ Analysis failed"
                }
                e.printStackTrace()
            }
        }.start()
    }

    private fun distributeContentToTabs(fullDocumentation: String) {
        val sections = parseDocumentationSections(fullDocumentation)

        overviewArea.text = buildOverviewContent(sections, fullDocumentation)
        classDetailsArea.text = buildClassDetailsContent(sections)
        patternsArea.text = buildPatternsContent(sections)
        relationshipsArea.text = buildRelationshipsContent(sections)
        insightsArea.text = buildInsightsContent(sections)
    }

    private fun parseDocumentationSections(documentation: String): Map<String, List<String>> {
        val sections = mutableMapOf<String, MutableList<String>>()
        val lines = documentation.lines()
        var currentContent = StringBuilder()
        var currentSection = "overview"

        for (line in lines) {
            when {
                line.startsWith("# 🤖 AI-Enhanced Project Analysis") -> {
                    currentSection = "overview"
                    currentContent.append(line).append("\n")
                }
                line.startsWith("## 🤖 AI Analysis") ||
                        line.startsWith("## 💡 Key Insights") ||
                        line.startsWith("## 🎯 Recommendations") ||
                        line.startsWith("## 🏗️ Architecture Patterns") -> {
                    currentSection = "ai_analysis"
                    currentContent.append(line).append("\n")
                }
                line.startsWith("## 🎯") -> {
                    // Class details from static analysis
                    sections.getOrPut("classDetails") { mutableListOf() }.add(currentContent.toString())
                    currentContent = StringBuilder(line).append("\n")
                }
                line.startsWith("---") -> {
                    // Section separator
                    if (currentContent.isNotEmpty()) {
                        sections.getOrPut(currentSection) { mutableListOf() }.add(currentContent.toString())
                        currentContent.clear()
                    }
                    currentSection = "detailed_analysis"
                }
                else -> {
                    currentContent.append(line).append("\n")

                    // Categorize specific content for tabs
                    when {
                        line.contains("🔍 Design Patterns") || line.contains("Pattern") -> {
                            sections.getOrPut("patterns") { mutableListOf() }.add(line)
                        }
                        line.contains("🔗 Class Relationships") || line.contains("Relationship") -> {
                            sections.getOrPut("relationships") { mutableListOf() }.add(line)
                        }
                        line.contains("💡") || line.contains("Insight") || line.contains("Recommendation") -> {
                            sections.getOrPut("insights") { mutableListOf() }.add(line)
                        }
                    }
                }
            }
        }

        // Add remaining content
        if (currentContent.isNotEmpty()) {
            sections.getOrPut(currentSection) { mutableListOf() }.add(currentContent.toString())
        }

        return sections
    }

    private fun buildOverviewContent(sections: Map<String, List<String>>, fullDoc: String): String {
        val overview = StringBuilder()

        val isAIEnhanced = fullDoc.contains("🤖 AI-Enhanced")

        overview.append(if (isAIEnhanced) "🤖 AI-ENHANCED ANALYSIS COMPLETE!\n" else "📊 STATIC ANALYSIS COMPLETE!\n")
        overview.append("=".repeat(50)).append("\n\n")

        // Add main content
        sections["overview"]?.forEach { overview.append(it).append("\n") }
        sections["ai_analysis"]?.forEach { overview.append(it).append("\n") }

        overview.append("\n💡 NAVIGATION GUIDE\n")
        overview.append("-".repeat(20)).append("\n")
        overview.append("• 'Overview' - Project summary and AI analysis\n")
        overview.append("• 'Class Details' - Detailed class-by-class analysis\n")
        overview.append("• 'Patterns' - Design patterns detected\n")
        overview.append("• 'Relationships' - Class relationships and dependencies\n")
        overview.append("• 'Insights' - Code quality insights and recommendations\n")

        return overview.toString()
    }

    private fun buildClassDetailsContent(sections: Map<String, List<String>>): String {
        val details = StringBuilder()
        details.append("🎯 DETAILED CLASS ANALYSIS\n")
        details.append("=".repeat(40)).append("\n\n")

        sections["classDetails"]?.forEach { classSection ->
            details.append(classSection).append("\n\n")
            details.append("-".repeat(60)).append("\n\n")
        }

        // Also include detailed analysis if available
        sections["detailed_analysis"]?.forEach { details.append(it).append("\n") }

        if (details.length <= 100) {
            details.append("ℹ️ No detailed class analysis available.\n")
            details.append("This could mean:\n")
            details.append("• No Java classes found in the project\n")
            details.append("• Project not fully loaded\n")
            details.append("• Analysis focused on high-level overview\n")
        }

        return details.toString()
    }

    private fun buildPatternsContent(sections: Map<String, List<String>>): String {
        val patterns = StringBuilder()
        patterns.append("🔍 DESIGN PATTERNS & ARCHITECTURE\n")
        patterns.append("=".repeat(40)).append("\n\n")

        var hasContent = false
        sections["patterns"]?.forEach { pattern ->
            patterns.append("${pattern.trim()}\n")
            hasContent = true
        }

        // Look for architecture patterns in AI analysis
        sections["ai_analysis"]?.forEach { section ->
            if (section.contains("Architecture") || section.contains("Pattern")) {
                patterns.append(section).append("\n")
                hasContent = true
            }
        }

        if (!hasContent) {
            patterns.append("ℹ️ No specific patterns detected in this analysis.\n")
            patterns.append("This could mean:\n")
            patterns.append("• Simple project structure\n")
            patterns.append("• Domain-specific patterns not covered\n")
            patterns.append("• Analysis focused on other aspects\n")
        }

        return patterns.toString()
    }

    private fun buildRelationshipsContent(sections: Map<String, List<String>>): String {
        val relationships = StringBuilder()
        relationships.append("🔗 CLASS RELATIONSHIPS & DEPENDENCIES\n")
        relationships.append("=".repeat(45)).append("\n\n")

        var hasContent = false
        sections["relationships"]?.forEach { rel ->
            relationships.append("${rel.trim()}\n")
            hasContent = true
        }

        if (!hasContent) {
            relationships.append("ℹ️ No explicit relationships detected.\n")
            relationships.append("This could mean:\n")
            relationships.append("• Simple project with minimal dependencies\n")
            relationships.append("• Well-encapsulated design\n")
            relationships.append("• Relationships not analyzed in this view\n")
        }

        return relationships.toString()
    }

    private fun buildInsightsContent(sections: Map<String, List<String>>): String {
        val insights = StringBuilder()
        insights.append("💡 INSIGHTS & RECOMMENDATIONS\n")
        insights.append("=".repeat(35)).append("\n\n")

        var hasContent = false

        // Include AI insights
        sections["ai_analysis"]?.forEach { section ->
            if (section.contains("Insights") || section.contains("Recommendations")) {
                insights.append(section).append("\n")
                hasContent = true
            }
        }

        sections["insights"]?.forEach { insight ->
            insights.append("${insight.trim()}\n")
            hasContent = true
        }

        if (!hasContent) {
            insights.append("ℹ️ No specific insights detected.\n")
            insights.append("This suggests:\n")
            insights.append("• Well-structured codebase\n")
            insights.append("• Good coding practices followed\n")
            insights.append("• No immediate issues detected\n")
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