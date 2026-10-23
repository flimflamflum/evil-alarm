package com.evilalarm.app.blocker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evilalarm.app.data.AppSettings
import com.evilalarm.app.repository
import com.evilalarm.app.ui.theme.EvilAlarmTheme
import com.evilalarm.app.ui.theme.GlassBackground
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Covers a blocked app. Leaving goes to the home screen, never back into the app. */
class BlockActivity : ComponentActivity() {
    private var blockedLabel by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        readIntent(intent)

        setContent {
            EvilAlarmTheme {
                BackHandler { goHome() }
                val blockUntil by repository.blockUntil.collectAsStateWithLifecycle<Long?>(initialValue = null)
                val settings by repository.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
                val dayStats by repository.dayStats.collectAsStateWithLifecycle(initialValue = emptyList())
                val attemptsToday = dayStats.find { it.date == LocalDate.now().toString() }?.attempts ?: 0
                val scope = rememberCoroutineScope()

                val until = blockUntil
                if (until == null) {
                    GlassBackground { }
                } else {
                    BlockScreen(
                        appLabel = blockedLabel,
                        blockUntil = until,
                        attemptsToday = attemptsToday,
                        settings = settings,
                        onGoHome = ::goHome,
                        onEndBlock = {
                            scope.launch {
                                repository.endBlock()
                                goHome()
                            }
                        },
                        // Block's over: drop back into whatever was underneath.
                        onContinue = ::finish,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readIntent(intent)
    }

    private fun readIntent(intent: Intent) {
        val pkg = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()
        blockedLabel = if (pkg.isEmpty()) "This app" else Blocker.appLabel(this, pkg)
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        finish()
    }

    companion object {
        private const val EXTRA_PACKAGE = "package"

        fun intent(context: Context, blockedPackage: String): Intent =
            Intent(context, BlockActivity::class.java)
                .putExtra(EXTRA_PACKAGE, blockedPackage)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
    }
}
