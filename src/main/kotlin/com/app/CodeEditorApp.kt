package com.app

import javafx.application.Application
import javafx.geometry.Orientation
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import org.fxmisc.richtext.CodeArea

class CodeEditorApp : Application() {
    override fun start(primaryStage: Stage) {
        // Create UI components
        val codeArea = CodeArea()
        val listView = ListView<OutputLine>()
        val submitButton = Button("Submit")
        val statusLabel = Label("Ready")

        // Set up layout
        val borderPane = BorderPane()
        val splitPane = SplitPane(codeArea, listView)
        splitPane.orientation = Orientation.HORIZONTAL
        borderPane.center = splitPane
        borderPane.top = ToolBar(submitButton)
        borderPane.bottom = statusLabel

        // Initialize controller with UI components
        val controller = CodeEditorController(codeArea, listView, submitButton, statusLabel)
        controller.initialize()
        // Set up scene
        val scene = Scene(borderPane, 800.0, 600.0)
        scene.stylesheets.add(javaClass.getResource("/style.css")!!.toExternalForm())
        primaryStage.scene = scene
        primaryStage.title = "Code Editor"
        primaryStage.show()
    }

    override fun stop() {
        // Delegate cleanup to controller if needed
    }
}

fun main() {
    Application.launch(CodeEditorApp::class.java)
}