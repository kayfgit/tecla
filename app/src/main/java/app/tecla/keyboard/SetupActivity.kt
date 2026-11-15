package app.tecla.keyboard

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import app.tecla.keyboard.model.LetterLayout

/** Setup screen: enable the keyboard, switch to it, pick settings and try it out. */
class SetupActivity : Activity() {
    private lateinit var settings: TeclaSettings
    private lateinit var layoutButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)
        settings = TeclaSettings(this)
        findViewById<Button>(R.id.enable).setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
        findViewById<Button>(R.id.choose).setOnClickListener {
            getSystemService(InputMethodManager::class.java)?.showInputMethodPicker()
        }
        layoutButton = findViewById(R.id.letter_layout)
        layoutButton.setOnClickListener { pickLetterLayout() }
        showLetterLayout()
    }

    override fun onResume() {
        super.onResume()
        val imm = getSystemService(InputMethodManager::class.java)
        val enabled = imm?.enabledInputMethodList?.any { it.packageName == packageName } == true
        findViewById<TextView>(R.id.status).setText(if (enabled) R.string.status_enabled else R.string.status_disabled)
    }

    private fun showLetterLayout() {
        layoutButton.text = getString(R.string.letter_layout_button, settings.letterLayout.label)
    }

    private fun pickLetterLayout() {
        val layouts = LetterLayout.entries
        AlertDialog.Builder(this)
            .setTitle(R.string.letter_layout_title)
            .setSingleChoiceItems(layouts.map { it.label }.toTypedArray(), layouts.indexOf(settings.letterLayout)) { dialog, which ->
                settings.letterLayout = layouts[which]
                showLetterLayout()
                dialog.dismiss()
            }
            .show()
    }
}
