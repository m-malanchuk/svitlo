package com.malanchuk.svitlo.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.malanchuk.svitlo.R
import com.malanchuk.svitlo.data.ScheduleStore
import com.malanchuk.svitlo.model.QueueIds
import com.malanchuk.svitlo.util.StartupTrace
import com.malanchuk.svitlo.util.withLanguage
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    private lateinit var todayButton: MaterialButton
    private lateinit var tomorrowButton: MaterialButton
    private lateinit var queueButton: MaterialButton
    private lateinit var refreshButton: ImageButton
    private lateinit var settingsButton: ImageButton
    private lateinit var errorCard: View
    private lateinit var errorText: TextView
    private lateinit var errorRetryButton: MaterialButton
    private lateinit var scheduleCard: ScheduleCardView

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    override fun attachBaseContext(newBase: Context) {
        val languageTag = ScheduleStore(newBase).languageTag()
        super.attachBaseContext(newBase.withLanguage(languageTag))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StartupTrace.mark("MainActivity.onCreate start")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContentView(R.layout.main_screen)
        bindViews()
        bindActions()
        render(viewModel.uiState.value)
        StartupTrace.mark("native main screen installed")

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.reloadCachedSchedule() }
            }
        }
    }

    private fun bindViews() {
        todayButton = findViewById(R.id.date_today_button)
        tomorrowButton = findViewById(R.id.date_tomorrow_button)
        queueButton = findViewById(R.id.queue_button)
        refreshButton = findViewById(R.id.refresh_button)
        settingsButton = findViewById(R.id.settings_button)
        errorCard = findViewById(R.id.error_card)
        errorText = findViewById(R.id.error_text)
        errorRetryButton = findViewById(R.id.error_retry_button)
        scheduleCard = findViewById(R.id.schedule_card)
        styleQueueButton()
    }

    private fun bindActions() {
        todayButton.setOnClickListener { viewModel.selectDay(0) }
        tomorrowButton.setOnClickListener { viewModel.selectDay(1) }
        queueButton.setOnClickListener {
            val options = viewModel.uiState.value.snapshot?.queueNames() ?: QueueIds.default
            showChoicePopup(queueButton, options, viewModel.uiState.value.selectedQueue) { queue ->
                viewModel.selectQueue(queue)
            }
        }
        refreshButton.setOnClickListener { viewModel.refresh() }
        settingsButton.setOnClickListener { showSettingsDialog() }
        errorRetryButton.setOnClickListener { viewModel.refresh() }
    }

    private fun render(state: MainUiState) {
        queueButton.text = state.selectedQueue
        setDateButtonState(todayButton, selected = state.dayOffset == 0)
        setDateButtonState(tomorrowButton, selected = state.dayOffset == 1)
        refreshButton.isEnabled = !state.isRefreshing
        refreshButton.alpha = 1f
        errorCard.visibility = if (state.errorMessage == null) View.GONE else View.VISIBLE
        errorText.text = state.errorMessage ?: getString(R.string.refresh_error)
        scheduleCard.bind(
            snapshot = state.snapshot,
            date = state.selectedDate,
            queue = state.selectedQueue,
            correctionEnabled = state.correction30MinutesEnabled,
            languageTag = ScheduleStore(this).languageTag(),
        )
    }

    private fun setDateButtonState(button: MaterialButton, selected: Boolean) {
        val primary = ContextCompat.getColor(this, R.color.startup_primary)
        val surface = ContextCompat.getColor(this, R.color.startup_surface)
        val outline = ContextCompat.getColor(this, R.color.startup_outline)
        val onPrimary = ContextCompat.getColor(this, R.color.startup_on_primary)
        button.backgroundTintList = null
        button.background = roundedBackground(if (selected) primary else surface, if (selected) 0 else outline, 16)
        button.stateListAnimator = null
        button.elevation = 0f
        button.setTextColor(if (selected) onPrimary else ContextCompat.getColor(this, R.color.startup_text_primary))
        button.strokeWidth = if (selected) 0 else dp(1)
        button.strokeColor = ColorStateList.valueOf(outline)
    }

    private fun styleQueueButton() {
        val primary = ContextCompat.getColor(this, R.color.startup_primary)
        queueButton.backgroundTintList = null
        queueButton.background = roundedBackground(primary, 0, 14)
        queueButton.stateListAnimator = null
        queueButton.elevation = 0f
        queueButton.setTextColor(ContextCompat.getColor(this, R.color.startup_on_primary))
    }

    private fun roundedBackground(fillColor: Int, strokeColor: Int, cornerRadiusDp: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(cornerRadiusDp).toFloat()
            setColor(fillColor)
            if (strokeColor != 0) setStroke(dp(1), strokeColor)
        }

    private fun showSettingsDialog() {
        val store = ScheduleStore(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(4), dp(16), dp(8))
        }

        content.addView(dialogLabel(getString(R.string.settings_language)))
        val languageButton = MaterialButton(this).apply {
            isAllCaps = false
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.startup_dropdown)
            iconGravity = MaterialButton.ICON_GRAVITY_END
            iconTint = ColorStateList.valueOf(ContextCompat.getColor(this@MainActivity, R.color.startup_text_primary))
            backgroundTintList = null
            background = roundedBackground(
                ContextCompat.getColor(this@MainActivity, R.color.startup_surface),
                ContextCompat.getColor(this@MainActivity, R.color.startup_outline),
                14,
            )
            stateListAnimator = null
            elevation = 0f
            cornerRadius = dp(14)
        }
        fun languageName(tag: String): String = if (tag == "uk") {
            getString(R.string.language_ukrainian)
        } else {
            getString(R.string.language_russian)
        }
        var selectedLanguage = store.languageTag()
        languageButton.text = languageName(selectedLanguage)
        languageButton.setOnClickListener {
            showChoicePopup(languageButton, listOf("ru", "uk"), selectedLanguage) { language ->
                selectedLanguage = language
                languageButton.text = languageName(language)
                viewModel.setLanguage(language)
                recreate()
            }
        }
        content.addView(languageButton, LinearLayout.LayoutParams(-1, dp(52)).apply {
            topMargin = dp(6)
        })

        val correctionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val correctionLabel = TextView(this).apply {
            text = getString(R.string.settings_correction_30_minutes)
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.startup_text_primary))
            textSize = 16f
        }
        correctionRow.addView(correctionLabel, LinearLayout.LayoutParams(0, -2, 1f))
        val correctionSwitch = SwitchMaterial(this).apply {
            isChecked = viewModel.uiState.value.correction30MinutesEnabled
            setOnCheckedChangeListener { _, checked -> viewModel.setCorrection30MinutesEnabled(checked) }
        }
        correctionRow.addView(correctionSwitch, LinearLayout.LayoutParams(-2, -2))
        content.addView(correctionRow, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(10)
        })

        val intervalLayout = TextInputLayout(this).apply {
            hint = getString(R.string.refresh_interval)
        }
        val intervalEdit = TextInputEditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setSingleLine(true)
            setText(viewModel.uiState.value.refreshIntervalMinutes.toString())
        }
        intervalLayout.addView(intervalEdit, LinearLayout.LayoutParams(-1, -2))
        intervalLayout.helperText = getString(R.string.refresh_interval_hint)
        content.addView(intervalLayout, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(12)
        })

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings_title)
            .setView(content)
            .setNegativeButton(R.string.settings_cancel, null)
            .setPositiveButton(R.string.settings_save, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val minutes = intervalEdit.text?.toString()?.toIntOrNull()
                if (minutes == null || minutes !in 15..1440) {
                    intervalLayout.error = getString(R.string.refresh_interval_error)
                } else if (viewModel.setRefreshInterval(minutes)) {
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun dialogLabel(text: String): TextView = TextView(this).apply {
        this.text = text
        setTextColor(ContextCompat.getColor(this@MainActivity, R.color.startup_text_secondary))
        textSize = 14f
    }

    private fun showChoicePopup(
        anchor: View,
        options: List<String>,
        selected: String,
        onSelected: (String) -> Unit,
    ) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(8), dp(4), dp(8))
        }
        val popupBackground = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(16).toFloat()
            setColor(ContextCompat.getColor(this@MainActivity, R.color.startup_surface))
            setStroke(dp(1), ContextCompat.getColor(this@MainActivity, R.color.startup_outline))
        }
        container.background = popupBackground
        val popup = PopupWindow(
            container,
            anchor.width.coerceAtLeast(dp(180)),
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true,
        ).apply {
            isOutsideTouchable = true
            elevation = dp(8).toFloat()
            setBackgroundDrawable(popupBackground)
        }
        options.forEach { option ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), 0, dp(10), 0)
                setBackgroundResource(android.R.drawable.list_selector_background)
            }
            val label = TextView(this).apply {
                text = when (option) {
                    "ru" -> getString(R.string.language_russian)
                    "uk" -> getString(R.string.language_ukrainian)
                    else -> option
                }
                textSize = 16f
                gravity = Gravity.CENTER_VERTICAL
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.startup_text_primary))
            }
            row.addView(label, LinearLayout.LayoutParams(0, dp(44), 1f))
            val check = TextView(this).apply {
                text = if (option == selected) "✓" else ""
                textSize = 20f
                gravity = Gravity.CENTER
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.startup_primary))
            }
            row.addView(check, LinearLayout.LayoutParams(dp(36), dp(44)))
            row.setOnClickListener {
                onSelected(option)
                popup.dismiss()
            }
            container.addView(row, LinearLayout.LayoutParams(-1, dp(44)))
        }
        popup.showAsDropDown(anchor, 0, -dp(2))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()
}
