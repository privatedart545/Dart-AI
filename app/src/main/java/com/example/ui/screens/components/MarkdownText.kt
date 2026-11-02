package com.example.ui.screens.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val blocks = parseMarkdownBlocks(markdown)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (block in blocks) {
            when (block) {
                is MarkdownBlock.Header -> {
                    val fontSize = when (block.level) {
                        1 -> 20.sp
                        2 -> 17.sp
                        else -> 15.sp
                    }
                    Text(
                        text = buildInlineAnnotatedString(block.text),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = fontSize,
                            color = CyanPrimary,
                            lineHeight = (fontSize.value + 6).sp
                        )
                    )
                }
                is MarkdownBlock.CodeBlock -> {
                    CodeBlockCard(language = block.language, code = block.code)
                }
                is MarkdownBlock.LatexBlock -> {
                    LatexBlockCard(formula = block.formula)
                }
                is MarkdownBlock.BulletItem -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = CyanPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = buildInlineAnnotatedString(block.text),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextPrimary,
                                lineHeight = 21.sp,
                                fontSize = 14.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                is MarkdownBlock.NumberedItem -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${block.number}.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = PurpleAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = buildInlineAnnotatedString(block.text),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextPrimary,
                                lineHeight = 21.sp,
                                fontSize = 14.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                is MarkdownBlock.Quote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(CyberSurfaceElevated.copy(alpha = 0.5f))
                            .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
                            .padding(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(20.dp)
                                .background(CyanPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = buildInlineAnnotatedString(block.text),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary,
                                fontStyle = FontStyle.Italic,
                                lineHeight = 20.sp,
                                fontSize = 13.sp
                            )
                        )
                    }
                }
                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = buildInlineAnnotatedString(block.text),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextPrimary,
                            lineHeight = 22.sp,
                            fontSize = 14.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun CodeBlockCard(language: String, code: String) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CyberBackground)
            .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberSurfaceElevated)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = language.ifBlank { "code" }.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = CyanPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            )
            IconButton(
                onClick = {
                    clipboard.setText(AnnotatedString(code))
                    Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy Code",
                    tint = TextMuted,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            Text(
                text = code,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = TextPrimary
                )
            )
        }
    }
}

@Composable
private fun LatexBlockCard(formula: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CyberBackground.copy(alpha = 0.8f))
            .border(1.dp, PurpleAccent.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Functions,
                contentDescription = "Formula",
                tint = PurpleAccent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "FORMULA",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = PurpleAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = formatLatexFormula(formula),
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = CyanPrimary
            ),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        )
    }
}

// Simple cleaner for LaTeX expressions
private fun formatLatexFormula(latex: String): String {
    return latex
        .replace("\\frac", "")
        .replace("\\sqrt", "√")
        .replace("\\sum", "∑")
        .replace("\\int", "∫")
        .replace("\\prod", "∏")
        .replace("\\alpha", "α")
        .replace("\\beta", "β")
        .replace("\\gamma", "γ")
        .replace("\\theta", "θ")
        .replace("\\pi", "π")
        .replace("\\infty", "∞")
        .replace("\\times", "×")
        .replace("\\cdot", "·")
        .replace("\\pm", "±")
        .replace("\\leq", "≤")
        .replace("\\geq", "≥")
        .replace("\\neq", "≠")
        .replace("\\approx", "≈")
        .replace("{", "")
        .replace("}", "")
        .trim()
}

// Inline parser for bold, italics, inline code, and inline math
private fun buildInlineAnnotatedString(text: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val len = text.length

        while (i < len) {
            // Check for bold **text**
            if (i + 1 < len && text[i] == '*' && text[i + 1] == '*') {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                    continue
                }
            }

            // Check for inline code `code`
            if (text[i] == '`') {
                val end = text.indexOf('`', i + 1)
                if (end != -1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            color = CyanPrimary,
                            background = CyberSurfaceElevated
                        )
                    ) {
                        append(" ${text.substring(i + 1, end)} ")
                    }
                    i = end + 1
                    continue
                }
            }

            // Check for inline LaTeX $math$
            if (text[i] == '$' && (i + 1 < len && text[i + 1] != '$')) {
                val end = text.indexOf('$', i + 1)
                if (end != -1) {
                    val formula = text.substring(i + 1, end)
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = PurpleAccent
                        )
                    ) {
                        append(formatLatexFormula(formula))
                    }
                    i = end + 1
                    continue
                }
            }

            // Check for italic *text*
            if (text[i] == '*' && (i + 1 < len && text[i + 1] != '*')) {
                val end = text.indexOf('*', i + 1)
                if (end != -1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = TextSecondary)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                    continue
                }
            }

            append(text[i])
            i++
        }
    }
}

sealed class MarkdownBlock {
    data class Header(val level: Int, val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    data class LatexBlock(val formula: String) : MarkdownBlock()
    data class BulletItem(val text: String) : MarkdownBlock()
    data class NumberedItem(val number: String, val text: String) : MarkdownBlock()
    data class Quote(val text: String) : MarkdownBlock()
}

private fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = markdown.lines()
    var idx = 0

    while (idx < lines.size) {
        val line = lines[idx]
        val trimmed = line.trim()

        // 1. Code Block ```
        if (trimmed.startsWith("```")) {
            val language = trimmed.removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            idx++
            while (idx < lines.size && !lines[idx].trim().startsWith("```")) {
                codeLines.add(lines[idx])
                idx++
            }
            if (idx < lines.size && lines[idx].trim().startsWith("```")) {
                idx++
            }
            blocks.add(MarkdownBlock.CodeBlock(language, codeLines.joinToString("\n")))
            continue
        }

        // 2. LaTeX Math Block $$ ... $$ or \[ ... \]
        if (trimmed.startsWith("$$") || trimmed.startsWith("\\[")) {
            val mathLines = mutableListOf<String>()
            if (trimmed.length > 2 && trimmed.endsWith("$$") && trimmed != "$$") {
                mathLines.add(trimmed.removePrefix("$$").removeSuffix("$$"))
                idx++
            } else {
                idx++
                while (idx < lines.size && !lines[idx].trim().endsWith("$$") && !lines[idx].trim().endsWith("\\]")) {
                    mathLines.add(lines[idx])
                    idx++
                }
                if (idx < lines.size) {
                    mathLines.add(lines[idx].trim().removeSuffix("$$").removeSuffix("\\]"))
                    idx++
                }
            }
            blocks.add(MarkdownBlock.LatexBlock(mathLines.joinToString(" ")))
            continue
        }

        // 3. Headers #, ##, ###
        if (trimmed.startsWith("#")) {
            val level = trimmed.takeWhile { it == '#' }.length.coerceIn(1, 3)
            val text = trimmed.removePrefix("#".repeat(level)).trim()
            blocks.add(MarkdownBlock.Header(level, text))
            idx++
            continue
        }

        // 4. Bullet lists (*, -, •)
        if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ")) {
            val text = trimmed.drop(2).trim()
            blocks.add(MarkdownBlock.BulletItem(text))
            idx++
            continue
        }

        // 5. Numbered lists (1. , 2. )
        val numberedMatch = Regex("^(\\d+)\\.\\s+(.*)").find(trimmed)
        if (numberedMatch != null) {
            val num = numberedMatch.groupValues[1]
            val text = numberedMatch.groupValues[2]
            blocks.add(MarkdownBlock.NumberedItem(num, text))
            idx++
            continue
        }

        // 6. Blockquotes (> )
        if (trimmed.startsWith("> ")) {
            blocks.add(MarkdownBlock.Quote(trimmed.removePrefix("> ").trim()))
            idx++
            continue
        }

        // 7. Regular paragraph / empty line
        if (trimmed.isNotEmpty()) {
            blocks.add(MarkdownBlock.Paragraph(line))
        }
        idx++
    }

    return blocks
}
