package com.example.codescribe

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.SwingUtilities

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
    private val generateButton = JButton("Generate Documentation")
    private val outputArea = JTextArea()

    init {
        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        outputArea.isEditable = false

        buttonPanel.add(generateButton)

        contentPanel.add(buttonPanel, BorderLayout.NORTH)
        contentPanel.add(JBScrollPane(outputArea), BorderLayout.CENTER)
    }

    private fun setupListeners() {
        generateButton.addActionListener {
            generateDocumentation()
        }
    }

    private fun generateDocumentation() {
        outputArea.text = "Generating documentation for project: ${project.name}...\n"

        try {
            val generator = DocumentationGenerator(project)
            val documentation = generator.generateProjectDocumentation()
            outputArea.text = documentation
        } catch (e: Exception) {
            outputArea.text = "Error generating documentation: ${e.message}"
            e.printStackTrace()
        }
    }

    fun getContentPanel(): JPanel {
        return contentPanel
    }

    companion object {
        private fun setupListeners(codeScribeToolWindowContent: CodeScribeToolWindowContent) {
                codeScribeToolWindowContent.generateButton.addActionListener {
                    codeScribeToolWindowContent.outputArea.text = "Generating docs...\n"
                    Thread {
                        try {
                            val docs = DocumentationGenerator(codeScribeToolWindowContent.project).generateProjectDocumentation()
                            SwingUtilities.invokeLater { codeScribeToolWindowContent.outputArea.text = docs }
                        } catch (e: Exception) {
                            SwingUtilities.invokeLater { codeScribeToolWindowContent.outputArea.text = "Error: ${e.message}" }
                        }
                    }.start()
                }
            }
    } }