package com.app

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ListView
import javafx.scene.layout.BorderPane
import javafx.scene.layout.VBox
import javafx.stage.Stage
import org.fxmisc.richtext.CodeArea
import java.io.File

class CodeEditor: Application(){
    override fun start(primaryStage: Stage) {
        val codeArea = CodeArea() // will write code in

        val outputList=ListView<String>() // will show the output

        val borderPane = BorderPane()
        var button = Button("Submit")

        button.setOnAction {
            val scriptFile = File.createTempFile("script", ".kts")
            scriptFile.writeText(codeArea.text)
            val process = ProcessBuilder("kotlinc","-script", scriptFile.absolutePath).start()
            val output = process.inputStream.bufferedReader().readText()
            outputList.items.add(output)
        }

        borderPane.center = codeArea
        borderPane.right = outputList
        borderPane.bottom = VBox(button)
        val scene = Scene(borderPane,800.0, 600.0)
        primaryStage.scene = scene
        primaryStage.title = "Code Editor"
        primaryStage.show()

    }
}

fun  main() {
    Application.launch(CodeEditor::class.java)
}