package com.example.autosleep

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var input: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pad = (24 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        val title = TextView(this).apply {
            text = "AutoSleep"
            textSize = 26f
        }

        status = TextView(this).apply {
            textSize = 16f
            setPadding(0, pad / 2, 0, pad / 2)
        }

        val label = TextView(this).apply {
            text = "Hareketsiz kalma süresi (saniye):"
            textSize = 16f
        }

        input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(prefs().getInt(SleepService.KEY_SECONDS, SleepService.DEFAULT_SECONDS).toString())
        }

        val save = Button(this).apply {
            text = "Kaydet"
            setOnClickListener {
                val value = input.text.toString().toIntOrNull()
                if (value == null || value < 5) {
                    Toast.makeText(this@MainActivity, "En az 5 saniye girin", Toast.LENGTH_SHORT).show()
                } else {
                    prefs().edit().putInt(SleepService.KEY_SECONDS, value).apply()
                    Toast.makeText(this@MainActivity, "Kaydedildi: $value sn", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val open = Button(this).apply {
            text = "Erişilebilirlik ayarlarını aç"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        val help = TextView(this).apply {
            text = "Kurulum: Erişilebilirlik ayarlarında \"AutoSleep\" hizmetini açın. " +
                "Açılamıyorsa: Ayarlar > Uygulamalar > AutoSleep > sağ üst ⋮ > " +
                "\"Kısıtlanmış ayarlara izin ver\", sonra tekrar deneyin."
            setPadding(0, pad, 0, 0)
        }

        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        listOf(title, status, label, input, save, open, help).forEach { root.addView(it, lp) }
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        status.text = if (isServiceEnabled()) "Durum: AÇIK ✅" else "Durum: KAPALI ❌ (hizmeti açın)"
    }

    private fun prefs() = getSharedPreferences(SleepService.PREFS, MODE_PRIVATE)

    private fun isServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.contains("$packageName/")
    }
}
