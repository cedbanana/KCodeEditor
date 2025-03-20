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

class CodeEditor : Application() {

    private val codeArea = CodeArea()
    private val outputList = FXCollections.observableArrayList<OutputLine>()
    private val listView = ListView(outputList)
    private var submitButton = Button("Submit") // Fixed variable name casing
    private var statusLabel = Label("Ready")
    private var currentProcess: Process? = null
    // NEW: Store error positions as (line, column), where column is nullable
    private var errorPositions: Set<Pair<Int, Int?>> = emptySet()

    override fun start(primaryStage: Stage) {
        val borderPane = BorderPane()
        val splitPane = SplitPane(codeArea, listView)
        splitPane.orientation = Orientation.HORIZONTAL
        borderPane.center = splitPane
        borderPane.top = ToolBar(submitButton)
        borderPane.bottom = statusLabel

        codeArea.paragraphGraphicFactory = LineNumberFactory.get(codeArea)
        // MODIFIED: Clear error positions and reapply styles on text change
        codeArea.textProperty().addListener { _, _, newValue ->
            errorPositions = emptySet() // Reset errors when text changes
            codeArea.setStyleSpans(0, keywordsHighlight(newValue)) // Renamed to avoid keyword clash
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
                        setOnMouseClicked { event ->
                            if (event.button == MouseButton.PRIMARY && event.isShortcutDown && item.isError && item.line != null) {
                                val position = if (item.column != null) {
                                    codeArea.getAbsolutePosition(item.line - 1, item.column - 1)
                                } else {
                                    codeArea.getAbsolutePosition(item.line - 1, 0)
                                }
                                codeArea.moveTo(position)
                                codeArea.requestFocus()
                                event.consume()
                            }
                        }
                    }
                }
            }
        }

        submitButton.setOnAction { runScript() }

        val scene = Scene(borderPane, 800.0, 600.0)
        scene.stylesheets.add(javaClass.getResource("/style.css").toExternalForm())
        primaryStage.scene = scene
        primaryStage.title = "Code Editor"
        primaryStage.show()
    }

    // NEW: Compute error ranges based on line and column positions
    private fun computeErrorRanges(text: String, errorPositions: Set<Pair<Int, Int?>>): List<Pair<Int, Int>> {
        val ranges = mutableListOf<Pair<Int, Int>>()
        val lines = text.split("\n")
        for ((line, column) in errorPositions) {
            var offsetStart: Int
            var offsetEnd: Int
            if (line > lines.size) continue
            val lineText = lines[line - 1]
            if (column != null) {
                var start =  if(column>lineText.length) lineText.length-1 else column - 1
                var end = column
                offsetStart = lines.subList(0, line - 1).sumOf { it.length + 1 } + start
                val charAtPos = lineText[start]
                // If the character is a symbol like ')', highlight just that character
                if (charAtPos.isLetterOrDigit() || charAtPos == '_') {
                    // Existing token logic for identifiers
                    while (start > 0 && (lineText[start - 1].isLetterOrDigit() || lineText[start - 1] == '_')) start--
                    while (end < lineText.length && (lineText[end].isLetterOrDigit() || lineText[end] == '_')) end++
                    if(end<lineText.length) {end++} // the (text ) case
                }
                    offsetEnd = lines.subList(0, line-1).sumOf { it.length + 1 } + end
            } else {
                // Highlight the entire line if no column is specified
                 offsetStart = lines.subList(0, line - 1).sumOf { it.length + 1 }
                 offsetEnd = offsetStart + lineText.length
            }
            ranges.add(Pair(offsetStart, offsetEnd))

        }
        return ranges
    }

    // NEW: Create StyleSpans for error ranges
    private fun computeErrorStyles(text: String, errorRanges: List<Pair<Int, Int>>): StyleSpans<Collection<String>> {
        val spansBuilder = StyleSpansBuilder<Collection<String>>()
        var lastPos = 1
        for ((start, end) in errorRanges.sortedBy { it.first }) {
            if (start > lastPos) {
                spansBuilder.add(emptyList(), start - lastPos)
            }
            spansBuilder.add(listOf("error"), end - start)
            lastPos = end
        }
        if (lastPos < text.length) {
            spansBuilder.add(emptyList(), text.length - lastPos)
        }
        return spansBuilder.create()
    }

    private fun keywordsHighlight(text: String): StyleSpans<Collection<String>>{
        // Compute keyword styles
        val keywords = setOf("fun", "val", "var", "if", "for", "while", "class", "interface", "import", "print")
        val keywordPattern = "\\b(" + keywords.joinToString("|") + ")\\b"
        val pattern = Pattern.compile("(?<KEYWORD>$keywordPattern)")
        val matcher = pattern.matcher(text)
        val spansBuilder = StyleSpansBuilder<Collection<String>>()
        var lastKw = 0
        while (matcher.find()) {
            spansBuilder.add(emptyList(), matcher.start() - lastKw)
            spansBuilder.add(listOf("keyword"), matcher.end() - matcher.start())
            lastKw = matcher.end()
        }
        spansBuilder.add(emptyList(), text.length - lastKw)
        return spansBuilder.create()
    }
    // MODIFIED: Renamed and enhanced to include error highlighting
    private fun doHighlight(text: String): StyleSpans<Collection<String>> {
        val keywordSpans = keywordsHighlight(text)
        // Compute error styles and overlay them
        val errorRanges = computeErrorRanges(text, errorPositions)
        val errorSpans = computeErrorStyles(text, errorRanges)
        return keywordSpans.overlay(errorSpans) { base, error -> base + error }
    }

    private fun runScript() {
        outputList.clear()
        statusLabel.text = "Running...."
        submitButton.isDisable = true

        val scriptFile = File.createTempFile("script", ".kts")
        scriptFile.writeText(codeArea.text)
        scriptFile.deleteOnExit()
        val processBuilder = ProcessBuilder("kotlinc", "-script", scriptFile.absolutePath)
        try {
            val process = processBuilder.start()
            currentProcess = process

            // Handle stdout
            thread {
                val outputReader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                while (process.isAlive || outputReader.ready()) {
                    line = outputReader.readLine()
                    if (line != null) {
                        Platform.runLater {
                            outputList.add(OutputLine(line, false, null, null))
                        }
                    }
                }
            }

            // MODIFIED: Collect error positions during stderr handling
            val errors = mutableSetOf<Pair<Int, Int?>>()
            thread {
                val errorReader = BufferedReader(InputStreamReader(process.errorStream))
                val normalErrorPattern = Pattern.compile(".*\\.kts:(\\d+):(\\d+): error: (.+)")
                val exceptionErrorPattern = Pattern.compile(".*\\.kts:(\\d+)")
                val skipPattern = Pattern.compile(".*\\^")
                var line: String?
                while (process.isAlive || errorReader.ready()) {
                    line = errorReader.readLine()
                    if (line != null) {
                        System.out.println(line)
                        val matcher = normalErrorPattern.matcher(line)
                        val exceptionMatcher = exceptionErrorPattern.matcher(line)
                        val skipMatcher = skipPattern.matcher(line)
                        if (matcher.matches()) {
                            val lineNum = matcher.group(1).toInt()
                            val colNum = matcher.group(2).toInt()
                            val errorMsg = matcher.group(3)
                            Platform.runLater {
                                outputList.add(OutputLine("error: $errorMsg at $lineNum:$colNum", true, lineNum, colNum))
                            }
                            errors.add(Pair(lineNum, colNum)) // Add error with column
                        } else if (exceptionMatcher.find()) {
                            val lineNum = exceptionMatcher.group(1).toInt()
                            Platform.runLater {
                                outputList.add(OutputLine(line, true, lineNum, null))
                            }
                            errors.add(Pair(lineNum, null)) // Add error without column
                        } else if(!skipMatcher.matches()){
                            Platform.runLater {
                                outputList.add(OutputLine(line, true, null, null))
                            }
                        }
                    }
                }
            }

            // MODIFIED: Update error positions and styles after script finishes
            thread {
                val exitCode = process.waitFor()
                Platform.runLater {
                    statusLabel.text = "Finished with code $exitCode"
                    statusLabel.style = if (exitCode != 0) "-fx-text-fill: red;" else "-fx-text-fill: black;"
                    submitButton.isDisable = false
                    errorPositions = errors // Set the collected error positions
                    codeArea.setStyleSpans(0, doHighlight(codeArea.text)) // Apply highlighting
                }
            }
        } catch (e: Exception) {
            Platform.runLater {
                outputList.add(OutputLine("Failed to run script: ${e.message}", false, null, null))
                statusLabel.text = "Error"
                statusLabel.style = "-fx-text-fill: red"
                submitButton.isDisable = false
            }
        }
    }

    override fun stop() {
        currentProcess?.destroy()
    }

    data class OutputLine(var text: String?, val isError: Boolean, val line: Int?, val column: Int?)
}

fun main() {
    Application.launch(CodeEditor::class.java)
}