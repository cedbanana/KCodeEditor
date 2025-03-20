package com.app

import javafx.application.Platform
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.regex.Pattern
import kotlin.concurrent.thread

class ScriptRunner {
    private var currentProcess: Process? = null

    fun runScript(
        scriptContent: String,
        onOutput: (OutputLine) -> Unit,
        onErrorPositions: (Set<Pair<Int, Int?>>) -> Unit,
        onFinish: (Int) -> Unit
    ) {
        val scriptFile = File.createTempFile("script", ".kts")
        scriptFile.writeText(scriptContent)
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
                        onOutput(OutputLine(line, false, null, null))
                    }
                }
            }

            val errors = mutableSetOf<Pair<Int, Int?>>()
            thread {
                val errorReader = BufferedReader(InputStreamReader(process.errorStream))
                val normalErrorPattern = Pattern.compile(".*\\.kts:(\\d+):(\\d+): error: (.+)")
                val exceptionErrorPattern = Pattern.compile(".*\\.kts:(\\d+)")
                var line: String?
                while (process.isAlive || errorReader.ready()) {
                    line = errorReader.readLine()
                    if (line != null) {
                        val matcher = normalErrorPattern.matcher(line)
                        val exceptionMatcher = exceptionErrorPattern.matcher(line)
                        if (matcher.matches()) {
                            val lineNum = matcher.group(1).toInt()
                            val colNum = matcher.group(2).toInt()
                            val errorMsg = matcher.group(3)
                                onOutput(OutputLine("error: $errorMsg at $lineNum:$colNum", true, lineNum, colNum))
                                System.out.flush()
                            errors.add(Pair(lineNum, colNum)) // Add error with column
                        } else{
                                if (exceptionMatcher.find()) {
                                val lineNum = exceptionMatcher.group(1).toInt()
                                    onOutput(OutputLine("error: $line", true, lineNum, null))
                                    System.out.flush()
                                    errors.add(Pair(lineNum, null)) // Add error without column
                            }
                        }


                   }
                }
            }

            // Handle process completion
            thread {
                val exitCode = process.waitFor()
                onErrorPositions(errors)
                onFinish(exitCode)
            }
        } catch (e: Exception) {
            onOutput(OutputLine("Failed to run script: ${e.message}", false, null, null))
            onFinish(-1)
        }
    }

    fun cancel() {
        currentProcess?.destroy()
    }
}