package com.app

import javafx.application.Application
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.geometry.Orientation
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.input.MouseButton
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.stage.Stage
import javafx.util.Callback
import org.fxmisc.richtext.CodeArea
import org.fxmisc.richtext.LineNumberFactory
import org.fxmisc.richtext.model.StyleSpans
import org.fxmisc.richtext.model.StyleSpansBuilder
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.regex.Pattern
import kotlin.concurrent.thread

class CodeEditor: Application() {

    private val codeArea = CodeArea()
    private val outputList = FXCollections.observableArrayList<OutputLine>()
    private val listView = ListView(outputList);
    private var Submitbutton = Button("Submit")
    private var statusLabel = Label("Ready");
    private var currentProcess: Process? = null


    override fun start(primaryStage: Stage) {
        val borderPane = BorderPane()
        val splitPane = SplitPane(codeArea,listView)
        splitPane.orientation = Orientation.HORIZONTAL
        borderPane.center = splitPane
        borderPane.top = ToolBar(Submitbutton)
        borderPane.bottom = statusLabel

        codeArea.paragraphGraphicFactory = LineNumberFactory.get(codeArea)
        codeArea.textProperty().addListener { _, _, newValue ->
            codeArea.setStyleSpans(0,DoHighlight(newValue))
        }

        listView.cellFactory = Callback {
            object : ListCell<OutputLine>() {
                override fun updateItem(item: OutputLine?, empty: Boolean) {
                    super.updateItem(item, empty)
                    if (empty) {
                        graphic = null
                    } else {
                        val hbox = HBox().apply { spacing = 5.0 }
                        val label = Label(item!!.text)
                        hbox.children.add(label)
                        graphic = hbox
                        setOnMouseClicked { event->

                            if(event.button==MouseButton.PRIMARY && event.isShortcutDown && item.isError && item.line != null){
                                if(item.column!=null){
                                    val position = codeArea.getAbsolutePosition(item.line - 1, item.column - 1);
                                    codeArea.moveTo(position)

                                } else {
                                    val position = codeArea.getAbsolutePosition(item.line - 1, 0)
                                    codeArea.moveTo(position)

                                }
                                codeArea.requestFocus()
                                event.consume()
                            }
                        }
                    }
                }
            }
        }

        Submitbutton.setOnAction { runScript() }

        val scene = Scene(borderPane, 800.0, 600.0)
        scene.stylesheets.add(javaClass.getResource("/style.css").toExternalForm())
        primaryStage.scene = scene
        primaryStage.title = "Code Editor"
        primaryStage.show()

    }

    private fun DoHighlight(text: String): StyleSpans<Collection<String>> {
        val keywords = setOf("fun", "val", "var", "if", "for", "while", "class", "interface", "import", "print")
        val keywordPattern = "\\b(" + keywords.joinToString("|") + ")\\b"
        val pattern = Pattern.compile("(?<KEYWORD>" + keywordPattern + ")")
        val matcher = pattern.matcher(text)
        val spansBuilder = StyleSpansBuilder<Collection<String>>()
        var lastKw = 0
        while (matcher.find()) {
            spansBuilder.add(emptyList(), matcher.start() - lastKw)
            spansBuilder.add(listOf("keyword"), matcher.end() - matcher.start())
            lastKw = matcher.end()
        }
        spansBuilder.add(emptyList(), text.length - lastKw)
        return spansBuilder.create();
    }

    private fun runScript() {
        outputList.clear()
        statusLabel.text = "Running...."
        Submitbutton.isDisable = true

        val scriptFile =File.createTempFile("script", ".kts")
        scriptFile.writeText(codeArea.text)
        scriptFile.deleteOnExit()
        val processBuilder = ProcessBuilder("kotlinc", "-script", scriptFile.absolutePath)
        try{
            val process = processBuilder.start()
            currentProcess = process

            //Handle stdout
            thread{
                val outputReader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                while(process.isAlive || outputReader.ready()) {
                    line = outputReader.readLine()
                    if (line != null) {
                        Platform.runLater {
                            outputList.add(OutputLine(line,false , null,null))
                        }
                    }
                }
            }

            thread{
                val errorReader = BufferedReader(InputStreamReader(process.errorStream))
                val normalErrorPattern = Pattern.compile(".*\\.kts:(\\d+):(\\d+): error: (.+)")
                val exceptionErrorPattern = Pattern.compile(".*\\.kts:(\\d+)")
                var line: String?
                while(process.isAlive || errorReader.ready()) {
                    line=errorReader.readLine()
                    System.out.println("[ERROR]: $line")
                    if (line != null) {
                        val matcher = normalErrorPattern.matcher(line)
                        val exceptionmatcher = exceptionErrorPattern.matcher(line)
                        if (matcher.matches()) {
                            val lineNum=matcher.group(1).toInt()
                            val colNum=matcher.group(2).toInt()
                            val errorMsg = matcher.group(3)

                            Platform.runLater {
                                outputList.add(OutputLine("error: $errorMsg at $lineNum:$colNum", true, lineNum,colNum))
                            }
                        } else if (exceptionmatcher.find()) { // look for the substring
                            val lineNum=exceptionmatcher.group(1).toInt()
                            Platform.runLater {
                                outputList.add(OutputLine(line, true, lineNum, null))
                            }
                        } else {
                            Platform.runLater {
                                outputList.add(OutputLine(line, true, null, null))
                            }
                        }
                    }
                }
            }
            thread {
                val exitCode= process.waitFor()
                Platform.runLater {
                    statusLabel.text = "Finished with code $exitCode"
                    statusLabel.style = if(exitCode !=0) "-fx-text-fill: red;" else "fx-text-fill: black;"
                    Submitbutton.isDisable = false

                }
            }
//TODO add red line under the text where the column is present.
        } catch (e :Exception) {
            Platform.runLater {
                outputList.add(OutputLine("Failed to run script: ${e.message}", false, null, null))
                statusLabel.text = "Error"
                statusLabel.style = "-fx-text-fill: red"
                Submitbutton.isDisable = false
            }
        }
    }

    override fun stop() {
        currentProcess?.destroy()
    }

    data class OutputLine(var text: String?, val isError: Boolean, val line: Int?, val column: Int?)
}
fun  main() {
    Application.launch(CodeEditor::class.java)
}