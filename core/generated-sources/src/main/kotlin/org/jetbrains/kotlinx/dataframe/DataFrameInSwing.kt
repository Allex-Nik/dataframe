package org.jetbrains.kotlinx.dataframe

import org.jetbrains.kotlinx.dataframe.io.renderToString
import javax.swing.JFrame
import javax.swing.JScrollPane
import javax.swing.JTextArea

public fun showDFInSwing(df: AnyFrame) {
    val frame = JFrame("Output in Swing")
    frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
    val textArea = JTextArea(df.renderToString())
    textArea.isEditable = false
    val scrollPane = JScrollPane(textArea)
    frame.contentPane.add(scrollPane)
    frame.setSize(800, 600)
    frame.isVisible = true
}
