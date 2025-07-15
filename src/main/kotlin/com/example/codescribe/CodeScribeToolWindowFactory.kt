// src/main/kotlin/com/example/codescribe/CodeScribeToolWindowFactory.kt
// Updated version with proper content distribution

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

        // Add tabs with better descriptions
        tabbedPane.addTab("📊 Overview", JBScrollPane(overviewArea))
        tabbedPane.addTab("🎯 Components", JBScrollPane(classDetailsArea))
        tabbedPane.addTab("🏗️ Architecture", JBScrollPane(patternsArea))
        tabbedPane.addTab("🔗 Structure", JBScrollPane(relationshipsArea))
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
        showAnalysisProgress()

        // Run analysis in background with proper read action
        Thread {
            try {
                val documentationResult = ApplicationManager.getApplication().runReadAction<DocumentationResult> {
                    generator.generateDocumentation()
                }

                SwingUtilities.invokeLater {
                    displayResults(documentationResult)
                    analyzeButton.text = originalText
                    analyzeButton.isEnabled = true

                    val statusText = if (documentationResult.isAIEnhanced) {
                        "✅ AI-enhanced analysis completed!"
                    } else {
                        "✅ Static analysis completed!"
                    }
                    statusLabel.text = statusText
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    showError(e)
                    analyzeButton.text = originalText
                    analyzeButton.isEnabled = true
                    statusLabel.text = "❌ Analysis failed"
                }
                e.printStackTrace()
            }
        }.start()
    }

    private fun showAnalysisProgress() {
        overviewArea.text = """
🚀 COMPREHENSIVE PROJECT ANALYSIS IN PROGRESS...

📁 Scanning Java source files...
🔍 Extracting classes, methods, and annotations...
🤖 Sending to AI backend for intelligent analysis...
🧠 Processing with qwen2.5-coder model...
📊 Generating structured insights...

⏱️ This usually takes 30-90 seconds depending on project size.
⏱️ Larger projects may take longer for thorough analysis.

Please wait...
        """.trimIndent()
    }

    private fun displayResults(result: DocumentationResult) {
        // Distribute content properly to each tab
        overviewArea.text = result.overview
        classDetailsArea.text = result.classDetails
        patternsArea.text = result.patterns
        relationshipsArea.text = result.relationships
        insightsArea.text = result.insights

        // Auto-switch to overview tab to show results
        tabbedPane.selectedIndex = 0
    }

    private fun showError(e: Exception) {
        overviewArea.text = """
❌ ANALYSIS ERROR

Error Details: ${e.message}

🔍 Possible Causes:
• Project not fully loaded/indexed by IntelliJ
• AI backend connection issues  
• Invalid or empty project structure
• Insufficient permissions

🛠️ Solutions:
1. Wait for IntelliJ indexing to complete (check progress bar)
2. Check AI backend status: curl http://localhost:8000/api/v1/health
3. Restart AI backend: python main.py in codescribe-backend folder
4. Ensure project contains Java source files in src/main/java
5. Try with a smaller test project first

📞 AI Backend Setup:
1. cd codescribe-backend
2. python main.py
3. Verify: curl http://localhost:8000/api/v1/health

🔧 Debug Information:
${e.stackTraceToString()}
        """.trimIndent()

        // Clear other tabs
        classDetailsArea.text = "Analysis failed - check Overview tab for details"
        patternsArea.text = "Analysis failed - check Overview tab for details"
        relationshipsArea.text = "Analysis failed - check Overview tab for details"
        insightsArea.text = "Analysis failed - check Overview tab for details"
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