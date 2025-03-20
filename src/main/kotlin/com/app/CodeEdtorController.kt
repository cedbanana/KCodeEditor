package com.app

import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import org.fxmisc.richtext.CodeArea
import org.fxmisc.richtext.LineNumberFactory

class CodeEditorController(
    private val codeArea: CodeArea,
    private val listView: ListView<OutputLine>,
    private val submitButton: Button,
    private val statusLabel: Label
) {
    private val outputList = FXCollections.observableArrayList<OutputLine>()
    private val scriptRunner = ScriptRunner()
    private val highlighter = Highlighter(codeArea)

    fun initialize() {
        // Set up CodeArea
        codeArea.paragraphGraphicFactory = LineNumberFactory.get(codeArea)
        codeArea.textProperty().addListener { _, _, _ ->
            highlighter.resetErrors()
            highlighter.updateKeywordsHighlight()
        }

        // Set up ListView
        listView.items = outputList
        listView.cellFactory = javafx.util.Callback {
            object : javafx.scene.control.ListCell<OutputLine>() {
                override fun updateItem(item: OutputLine?, empty: Boolean) {
                    super.updateItem(item, empty)
                    if (empty || item == null) {
                        graphic = null
                    } else {

                        val hbox = javafx.scene.layout.HBox().apply { spacing = 5.0 }
                        val label =Label(item.text)

                        hbox.children.add(label)
                        graphic = hbox
                        setOnMouseClicked { event ->
                            onEvent(event,item)}
                    }
                }
            }
        }

        // Set up submit button
        submitButton.setOnAction { runScript() }
    }
    private fun onEvent(event: MouseEvent,item: OutputLine) {
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
        return
    }

    private fun runScript() {
        outputList.clear()
        statusLabel.text = "Running...."
        submitButton.isDisable = true

        scriptRunner.runScript(
            codeArea.text,
            onOutput = { outputLine -> Platform.runLater { outputList.add(outputLine) } },
            onErrorPositions = { errors ->
                Platform.runLater {
                    highlighter.setErrors(errors)
                    highlighter.updateHighlighting()
                }
            },
            onFinish = { exitCode ->
                Platform.runLater {
                    statusLabel.text = "Finished with code $exitCode"
                    statusLabel.style = if (exitCode != 0) "-fx-text-fill: red;" else "-fx-text-fill: black;"
                    submitButton.isDisable = false
                }
            }
        )
    }

}