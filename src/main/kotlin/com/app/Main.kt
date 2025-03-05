package com.app

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.ListView
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import org.fxmisc.richtext.CodeArea

class CodeEditor: Application(){
    override fun start(primaryStage: Stage) {
        val codeArea = CodeArea() // will write code in

        val outputList=ListView<String>() // will show the output

        val borderPane = BorderPane()

        borderPane.center = codeArea
        borderPane.right = outputList
        val scene = Scene(borderPane,800.0, 600.0)
        primaryStage.scene = scene
        primaryStage.title = "Code Editor"
        primaryStage.show()

    }
}

fun  main() {
    Application.launch(CodeEditor::class.java)
}