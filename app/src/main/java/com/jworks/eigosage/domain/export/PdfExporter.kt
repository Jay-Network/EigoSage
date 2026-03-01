package com.jworks.eigosage.domain.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PAGE_WIDTH = 595   // A4 width in points
        private const val PAGE_HEIGHT = 842  // A4 height in points
        private const val MARGIN = 50f
        private const val LINE_SPACING = 1.4f
    }

    private val titlePaint = Paint().apply {
        color = Color.BLACK
        textSize = 28f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    private val subtitlePaint = Paint().apply {
        color = Color.DKGRAY
        textSize = 16f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        isAntiAlias = true
    }

    private val headingPaint = Paint().apply {
        color = Color.BLACK
        textSize = 18f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    private val bodyPaint = Paint().apply {
        color = Color.DKGRAY
        textSize = 12f
        typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        isAntiAlias = true
    }

    private val metricLabelPaint = Paint().apply {
        color = Color.GRAY
        textSize = 11f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        isAntiAlias = true
    }

    private val metricValuePaint = Paint().apply {
        color = Color.BLACK
        textSize = 14f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    private val footerPaint = Paint().apply {
        color = Color.GRAY
        textSize = 9f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        isAntiAlias = true
    }

    fun export(result: AnalysisResult): Result<File> {
        return try {
            val outputDir = File(
                context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS),
                "EigoSage"
            )
            outputDir.mkdirs()
            val fileName = "analysis_${System.currentTimeMillis()}.pdf"
            val outputFile = File(outputDir, fileName)

            val pdf = PdfDocument()

            addCoverPage(pdf, result)
            addContentPages(pdf, result)

            FileOutputStream(outputFile).use { pdf.writeTo(it) }
            pdf.close()

            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun addCoverPage(pdf: PdfDocument, result: AnalysisResult) {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        val centerX = PAGE_WIDTH / 2f

        // Title
        var y = PAGE_HEIGHT * 0.3f
        drawCenteredText(canvas, "EigoSage", centerX, y, titlePaint)
        y += 40f
        drawCenteredText(canvas, "Text Analysis Report", centerX, y, subtitlePaint)

        // Grade level badge
        val readability = result.readability
        if (readability != null) {
            y += 80f
            val gradePaint = Paint(titlePaint).apply { textSize = 48f; color = gradeColor(readability.averageGrade) }
            drawCenteredText(canvas, "Grade ${String.format("%.1f", readability.averageGrade)}", centerX, y, gradePaint)
            y += 30f
            drawCenteredText(canvas, readability.difficulty.label, centerX, y, subtitlePaint)
            y += 24f
            drawCenteredText(canvas, readability.targetAudience, centerX, y, metricLabelPaint)
        }

        // Stats summary
        y = PAGE_HEIGHT * 0.6f
        drawCenteredText(canvas, "${result.wordCount} words analyzed", centerX, y, subtitlePaint)

        // Date
        y += 40f
        val dateStr = SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.US).format(Date(result.timestamp))
        drawCenteredText(canvas, dateStr, centerX, y, metricLabelPaint)

        drawFooter(canvas, 1)
        pdf.finishPage(page)
    }

    private fun addContentPages(pdf: PdfDocument, result: AnalysisResult) {
        var pageNum = 2
        var y = MARGIN

        var currentPage: PdfDocument.Page? = null
        var canvas: Canvas? = null

        fun startNewPage(): Canvas {
            currentPage?.let { pdf.finishPage(it) }
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum++).create()
            val p = pdf.startPage(pageInfo)
            currentPage = p
            y = MARGIN
            return p.canvas
        }

        fun ensureSpace(needed: Float): Canvas {
            if (canvas == null || y + needed > PAGE_HEIGHT - MARGIN - 20f) {
                canvas?.let { drawFooter(it, pageNum - 1) }
                canvas = startNewPage()
            }
            return canvas!!
        }

        // Readability metrics section
        val readability = result.readability
        if (readability != null) {
            canvas = ensureSpace(200f)
            canvas!!.drawText("Readability Scores", MARGIN, y + headingPaint.textSize, headingPaint)
            y += headingPaint.textSize + 20f

            val metrics = listOf(
                "Flesch-Kincaid Grade" to String.format("%.1f", readability.fleschKincaidGrade),
                "Flesch Reading Ease" to String.format("%.1f", readability.fleschReadingEase),
                "SMOG Index" to String.format("%.1f", readability.smogIndex),
                "Coleman-Liau Index" to String.format("%.1f", readability.colemanLiauIndex),
                "Average Grade" to String.format("%.1f", readability.averageGrade)
            )

            for ((label, value) in metrics) {
                canvas = ensureSpace(30f)
                canvas!!.drawText(label, MARGIN + 10f, y + metricLabelPaint.textSize, metricLabelPaint)
                canvas!!.drawText(value, PAGE_WIDTH - MARGIN - 60f, y + metricValuePaint.textSize, metricValuePaint)
                y += 24f
            }

            y += 10f

            // Text statistics
            canvas = ensureSpace(150f)
            canvas!!.drawText("Text Statistics", MARGIN, y + headingPaint.textSize, headingPaint)
            y += headingPaint.textSize + 20f

            val stats = listOf(
                "Total Words" to readability.statistics.totalWords.toString(),
                "Total Sentences" to readability.statistics.totalSentences.toString(),
                "Total Syllables" to readability.statistics.totalSyllables.toString(),
                "Avg Words/Sentence" to String.format("%.1f", readability.statistics.averageWordsPerSentence),
                "Avg Syllables/Word" to String.format("%.1f", readability.statistics.averageSyllablesPerWord),
                "Polysyllabic Words" to readability.statistics.polysyllableCount.toString()
            )

            for ((label, value) in stats) {
                canvas = ensureSpace(30f)
                canvas!!.drawText(label, MARGIN + 10f, y + metricLabelPaint.textSize, metricLabelPaint)
                canvas!!.drawText(value, PAGE_WIDTH - MARGIN - 60f, y + metricValuePaint.textSize, metricValuePaint)
                y += 24f
            }

            y += 20f
        }

        // Extracted text section
        canvas = ensureSpace(40f)
        canvas!!.drawText("Extracted Text", MARGIN, y + headingPaint.textSize, headingPaint)
        y += headingPaint.textSize + 16f

        // Wrap and draw body text
        val maxWidth = PAGE_WIDTH - 2 * MARGIN
        val lines = wrapText(result.fullText, bodyPaint, maxWidth)
        val lineHeight = bodyPaint.textSize * LINE_SPACING

        for (line in lines) {
            canvas = ensureSpace(lineHeight + 4f)
            canvas!!.drawText(line, MARGIN, y + bodyPaint.textSize, bodyPaint)
            y += lineHeight
        }

        // Finish last page
        canvas?.let {
            drawFooter(it, pageNum - 1)
        }
        currentPage?.let { pdf.finishPage(it) }
    }

    private fun drawCenteredText(canvas: Canvas, text: String, x: Float, y: Float, paint: Paint) {
        val textWidth = paint.measureText(text)
        canvas.drawText(text, x - textWidth / 2f, y, paint)
    }

    private fun drawFooter(canvas: Canvas, pageNum: Int) {
        val footerY = PAGE_HEIGHT - 20f
        canvas.drawText("Generated by EigoSage", MARGIN, footerY, footerPaint)
        val pageText = "Page $pageNum"
        val pageWidth = footerPaint.measureText(pageText)
        canvas.drawText(pageText, PAGE_WIDTH - MARGIN - pageWidth, footerY, footerPaint)
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val lines = mutableListOf<String>()
        val paragraphs = text.split("\n")

        for (paragraph in paragraphs) {
            if (paragraph.isBlank()) {
                lines.add("")
                continue
            }
            val words = paragraph.split(Regex("\\s+"))
            val currentLine = StringBuilder()

            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (paint.measureText(testLine) <= maxWidth) {
                    currentLine.clear().append(testLine)
                } else {
                    if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
                    currentLine.clear().append(word)
                }
            }
            if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
        }
        return lines
    }

    private fun gradeColor(grade: Double): Int = when {
        grade <= 5.0 -> Color.rgb(76, 175, 80)    // Green
        grade <= 7.0 -> Color.rgb(139, 195, 74)    // Light green
        grade <= 10.0 -> Color.rgb(255, 193, 7)    // Amber
        grade <= 13.0 -> Color.rgb(255, 152, 0)    // Orange
        else -> Color.rgb(244, 67, 54)              // Red
    }
}
