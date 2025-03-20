package com.app

import org.fxmisc.richtext.CodeArea
import org.fxmisc.richtext.model.StyleSpans
import org.fxmisc.richtext.model.StyleSpansBuilder
import java.util.regex.Pattern

class Highlighter(private val codeArea: CodeArea) {
    private var errorPositions: Set<Pair<Int, Int?>> = emptySet()

    fun resetErrors() {
        errorPositions = emptySet()
    }

    fun setErrors(errors: Set<Pair<Int, Int?>>) {
        errorPositions = errors
    }

    fun updateHighlighting() {
        val text = codeArea.text
        val spans = computeHighlighting(text)
        codeArea.setStyleSpans(0, spans)
    }

    fun updateKeywordsHighlight() {
        val text = codeArea.text
        val spans = computeKeywordStyles(text)
        codeArea.setStyleSpans(0, spans)
    }

    private fun computeHighlighting(text: String): StyleSpans<Collection<String>> {
        val keywordSpans = computeKeywordStyles(text)
        val errorRanges = computeErrorRanges(text)
        val errorSpans = computeErrorStyles(text, errorRanges)
        return keywordSpans.overlay(errorSpans) { base, error -> base + error }
    }

    private fun computeKeywordStyles(text: String): StyleSpans<Collection<String>> {
        val keywords = setOf("fun", "val", "var", "if", "for", "while", "class", "interface", "import", "print", "println", "throw")
        val keywordPattern = "\\b(" + keywords.joinToString("|") + ")\\b"
        val pattern = Pattern.compile(keywordPattern)
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

    private fun computeErrorRanges(text: String): List<Pair<Int, Int>> {
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
}