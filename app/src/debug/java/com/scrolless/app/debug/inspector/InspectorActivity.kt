/*
 * Copyright (C) 2026 Scrolless
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.scrolless.app.debug.inspector

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Debug-only screen to control and export inspector dumps. Plain Android views on purpose:
 * it must keep working even if the Compose UI of the app changes.
 */
class InspectorActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var preview: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        InspectorStore.init(this)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(32))
        }
        content.addView(label("Inspetor de telas (só debug)", 22f, bold = true))
        content.addView(label(INTRO, 14f))
        status = label("", 14f, bold = true)
        content.addView(status)
        content.addView(
            toggle("Captura automática (a cada ~2 s com o TikTok aberto)", InspectorStore.autoCapture) {
                InspectorStore.autoCapture = it
            },
        )
        content.addView(
            toggle("Incluir textos da tela (podem conter dados pessoais)", InspectorStore.includeText) {
                InspectorStore.includeText = it
            },
        )
        content.addView(button("Abrir configurações de acessibilidade") { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) })
        content.addView(button("Ver o último dump") { showLast() })
        content.addView(button("Compartilhar dumps (.zip)") { share() })
        content.addView(button("Apagar todos os dumps") { confirmClear() })
        preview = TextView(this).apply {
            typeface = Typeface.MONOSPACE
            textSize = 10f
            setTextIsSelectable(true)
            setPadding(0, dp(16), 0, 0)
        }
        content.addView(preview)

        val scroll = ScrollView(this).apply { addView(content) }
        setContentView(scroll)
        // The app draws edge to edge, so keep the controls clear of the system bars.
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val files = InspectorStore.list()
        val kilobytes = files.sumOf { it.length() } / 1024
        val serviceState = if (isServiceEnabled()) "ATIVO" else "desligado"
        status.text = "Serviço de acessibilidade: $serviceState\nDumps salvos: ${files.size} ($kilobytes KB)"
    }

    private fun isServiceEnabled(): Boolean {
        val manager = getSystemService(AccessibilityManager::class.java)
        return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.resolveInfo.serviceInfo.packageName == packageName }
    }

    private fun showLast() {
        val last = InspectorStore.list().lastOrNull()
        preview.text = if (last == null) {
            "Nenhum dump ainda."
        } else {
            last.name + "\n\n" + last.readText().take(PREVIEW_CHARS)
        }
    }

    private fun share() {
        if (InspectorStore.list().isEmpty()) {
            Toast.makeText(this, "Nenhum dump para compartilhar.", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(this, "$packageName.inspector", InspectorStore.zipForSharing())
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("dumps", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(send, "Compartilhar dumps"))
    }

    private fun confirmClear() {
        AlertDialog.Builder(this)
            .setMessage("Apagar todos os dumps salvos neste aparelho?")
            .setPositiveButton("Apagar") { _, _ ->
                InspectorStore.clear()
                preview.text = ""
                refresh()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun label(text: String, size: Float, bold: Boolean = false) = TextView(this).apply {
        this.text = text
        textSize = size
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setPadding(0, 0, 0, dp(12))
    }

    private fun toggle(text: String, checked: Boolean, onChange: (Boolean) -> Unit) = Switch(this).apply {
        this.text = text
        isChecked = checked
        setPadding(0, dp(12), 0, dp(12))
        setOnCheckedChangeListener { _, value -> onChange(value) }
    }

    private fun button(text: String, onClick: () -> Unit) = Button(this).apply {
        this.text = text
        isAllCaps = false
        setOnClickListener { onClick() }
    }

    private companion object {
        const val PREVIEW_CHARS = 6_000

        const val INTRO = "Como usar:\n" +
            "1) Ative o serviço do app em Configurações > Acessibilidade.\n" +
            "2) No mesmo menu, escolha o botão de acessibilidade como atalho do serviço.\n" +
            "3) Abra o TikTok, vá até a tela que quer registrar e toque no botão: um dump é salvo.\n" +
            "4) Volte aqui e toque em Compartilhar.\n\n" +
            "Os dumps ficam só neste aparelho até você compartilhar. O app não tem permissão de internet. " +
            "Por padrão os textos da tela viram apenas o tamanho (#12)."
    }
}
