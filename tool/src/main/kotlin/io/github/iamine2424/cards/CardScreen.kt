package io.github.iamine2424.cards

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.google.zxing.BarcodeFormat
import com.google.zxing.common.BitMatrix
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.floor

class CardScreen(
    sealedActivity: SealedLightActivity,
    private val cardId: String,
    private val store: CardStore,
) : SimpleLightScreen<Unit>(sealedActivity) {

    private val scope = MainScope()

    override fun onScreenDestroy() = scope.cancel()

    private fun edit(transform: (Card) -> Card) = scope.launch {
        store.update { cards -> cards.map { if (it.id == cardId) transform(it) else it } }
    }

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val cards by store.cards.collectAsState(initial = null)
        val card = cards?.firstOrNull { it.id == cardId } ?: return
        val format = BarcodeFormat.valueOf(card.format)
        val matrix = remember(card.value, format) { runCatching { encode(card.value, format) }.getOrNull() }

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(icon = LightIcons.BACK, onClick = { goBack() }),
                    center = LightTopBarCenter.Text(card.name, onClick = {
                        navigateTo(screenFactory = { TextEditorScreen(it, "Rename", card.name) }) { name ->
                            edit { it.copy(name = name.trim()) }
                        }
                    }),
                    rightButton = LightBarButton.LightIcon(icon = LightIcons.TRASH, onClick = {
                        navigateTo(screenFactory = { ConfirmDeleteScreen(it, card.name) }) { confirmed ->
                            if (confirmed) scope.launch {
                                store.update { cards -> cards.filterNot { it.id == cardId } }
                                goBack()
                            }
                        }
                    }),
                    modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 1f.gridUnitsAsDp()),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    LightText(
                        text = card.number,
                        variant = LightTextVariant.Heading,
                        align = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                    )
                    if (matrix != null) {
                        Barcode(matrix)
                    } else {
                        LightText(
                            text = "can't draw this value as ${format.label()}",
                            variant = LightTextVariant.Copy,
                            align = TextAlign.Center,
                        )
                    }
                }

                // Tap to cycle the barcode type if the scanner doesn't read it.
                LightBottomBar(listOf(LightBarButton.Text(text = format.label(), onClick = {
                    edit { it.copy(format = format.next().name) }
                })))
            }
        }
    }
}

/** Black on white regardless of theme, snapped to whole pixels so scanners read it. */
@Composable
private fun Barcode(matrix: BitMatrix) {
    val is1D = matrix.height == 1
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(if (is1D) 2.5f else 1f)
            .background(Color.White),
    ) {
        val module = floor(size.width / matrix.width).coerceAtLeast(1f)
        val moduleHeight = if (is1D) size.height else module
        val left = (size.width - module * matrix.width) / 2
        val top = (size.height - moduleHeight * matrix.height) / 2
        for (y in 0 until matrix.height) for (x in 0 until matrix.width) {
            if (matrix[x, y]) {
                drawRect(Color.Black, Offset(left + x * module, top + y * moduleHeight), Size(module, moduleHeight))
            }
        }
    }
}

class ConfirmDeleteScreen(
    sealedActivity: SealedLightActivity,
    private val name: String,
) : SimpleLightScreen<Boolean>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(icon = LightIcons.BACK, onClick = { goBack(false) }),
                    center = LightTopBarCenter.Text(name),
                    modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                )
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 1f.gridUnitsAsDp()),
                    contentAlignment = Alignment.Center,
                ) {
                    LightText(
                        text = "Are you sure you'd like to remove this card?",
                        variant = LightTextVariant.Copy,
                        align = TextAlign.Center,
                    )
                }
                LightBottomBar(listOf(LightBarButton.Text(text = "CONFIRM", onClick = { goBack(true) })))
            }
        }
    }
}
