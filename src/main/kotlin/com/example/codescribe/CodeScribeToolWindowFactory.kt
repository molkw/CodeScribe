package com.example.codescribe

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import javax.swing.JLabel
import javax.swing.JPanel

class CodeScribeToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()

        val panel = JPanel()
        panel.add(JLabel("Welcome to CodeScribe!"))

        val content = contentFactory.createContent(panel, "CodeScribe", false)
        toolWindow.contentManager.addContent(content)
    }
}