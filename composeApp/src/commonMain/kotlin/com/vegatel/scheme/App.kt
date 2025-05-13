package com.vegatel.scheme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.vegatel.scheme.model.Cable
import com.vegatel.scheme.model.Element.Antenna
import com.vegatel.scheme.model.Element.Repeater
import com.vegatel.scheme.model.ElementMatrix
import com.vegatel.scheme.ui.MainMenu
import com.vegatel.scheme.ui.SchemeConstructor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jetbrains.compose.ui.tooling.preview.Preview

//
fun buildElementMatrix(
    rows: Int,
    cols: Int,
    builder: ElementMatrix.() -> Unit
): ElementMatrix {
    return ElementMatrix(initialRows = rows, initialCols = cols).apply(builder)
}

val initialElements = buildElementMatrix(rows = 2, cols = 1) {
    this[0, 0] = Antenna(
        id = 1,
        endElementId = 2,
        cable = Cable()
    )

    this[1, 0] = Repeater(
        id = 2,
        topElementId = 1
    )
}
//

private val _elements = MutableStateFlow(initialElements)
val elements: StateFlow<ElementMatrix> = _elements.asStateFlow()

@Composable
@Preview
fun App() {
    MaterialTheme {
//        var showContent by remember { mutableStateOf(false) }

//            Button(onClick = { showContent = !showContent }) {
//                Text("Click me!")
//            }
//
//            AnimatedVisibility(showContent) {
//                val greeting = remember { Greeting().greet() }
//
//                Column(
//                    Modifier.fillMaxWidth(),
//                    horizontalAlignment = Alignment.CenterHorizontally
//                ) {
//                    Image(painterResource(Res.drawable.compose_multiplatform), null)
//                    Text("Compose: $greeting")
//                }
//            }

        val elements by elements.collectAsState()

        Column(
            Modifier
                .fillMaxWidth()
                .padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                    bottom = WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding()
                )
        ) {
            MainMenu(
                onNew = { log("TEST", "onNew") }
            )

            Divider()


            SchemeConstructor(
                elements = elements,
                onElementsChange = { _elements.value = it }
            )
        }
    }
}