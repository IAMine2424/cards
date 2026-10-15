package io.github.iamine2424.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.thelightphone.sdk.InitialScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.rememberKeyboardOptions
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextInputEditor
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@InitialScreen
class CardsHomeScreen(sealedActivity: SealedLightActivity) : SimpleLightScreen<Unit>(sealedActivity) {

    private val store = CardStore(lightContext.dataStore)

    // Not rememberCoroutineScope: results arrive while this screen is off-composition.
    private val scope = MainScope()

    override fun onScreenDestroy() = scope.cancel()

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val cards by store.cards.collectAsState(initial = null)

        fun openCard(id: String) = navigateTo(screenFactory = { CardScreen(it, id, store) })

        // Name -> number on the card -> barcode value (defaults to the number).
        fun addCard() {
            navigateTo(screenFactory = { TextEditorScreen(it, "Card Name", "", initialCaps = true) }) { name ->
                navigateTo(screenFactory = { TextEditorScreen(it, "Card Number", "") }) { number ->
                    navigateTo(screenFactory = { TextEditorScreen(it, "Barcode Value", number) }) { value ->
                        val card = Card(name = name.trim(), number = number.trim(), value = value.trim())
                        scope.launch {
                            store.update { it + card }
                            openCard(card.id)
                        }
                    }
                }
            }
        }

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    center = LightTopBarCenter.Text("Cards"),
                    modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                )

                val list = cards
                if (list != null && list.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        LightText(text = "no cards added", variant = LightTextVariant.Copy, align = TextAlign.Center)
                    }
                } else {
                    LightScrollView(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(start = 1f.gridUnitsAsDp()),
                    ) {
                        list.orEmpty().forEach { card ->
                            LightText(
                                text = card.name,
                                variant = LightTextVariant.Heading,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .lightClickable { openCard(card.id) }
                                    .padding(vertical = 0.75f.gridUnitsAsDp()),
                            )
                        }
                    }
                }

                LightBottomBar(listOf(LightBarButton.Text(text = "ADD NEW", onClick = ::addCard)))
            }
        }
    }
}

/** Full-screen keyboard; returns the text on submit, nothing on back or blank input. */
class TextEditorScreen(
    sealedActivity: SealedLightActivity,
    private val title: String,
    private val initialValue: String,
    private val initialCaps: Boolean = false,
) : SimpleLightScreen<String>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val textState = rememberTextFieldState(initialValue)
        LightTheme(colors = themeColors) {
            LightTextInputEditor(
                title = title,
                state = textState,
                keyboardOptionsFlow = rememberKeyboardOptions(),
                onSubmit = { text -> if (text.isNotBlank()) goBack(text.toString()) },
                onBack = { goBack(null) },
                modifier = Modifier.background(LightThemeTokens.colors.background),
                initialCaps = initialCaps,
            )
        }
    }
}
