package com.vegatel.scheme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val _schemeState = MutableStateFlow<SchemeState>(initialSchemeState)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars =
            true

        registerOpenElementMatrixFromDialog(_schemeState)
        registerSaveElementMatrixFromDialog(_schemeState)
        registerOpenBackgroundFromDialog(_schemeState)
        registerExportSchemeToPdfFromDialog()

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}