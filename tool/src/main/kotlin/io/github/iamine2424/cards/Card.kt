package io.github.iamine2424.cards

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * [number] is what's printed on the card, [value] is what the barcode encodes.
 * They differ on some cards (e.g. Museumkaart), so both are stored.
 */
@Serializable
data class Card(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val number: String,
    val value: String,
    val format: String = BarcodeFormat.CODE_128.name,
)

/** Formats the type button cycles through, most common first. */
val FORMATS = listOf(
    BarcodeFormat.CODE_128,
    BarcodeFormat.QR_CODE,
    BarcodeFormat.EAN_13,
    BarcodeFormat.CODE_39,
    BarcodeFormat.PDF_417,
    BarcodeFormat.AZTEC,
    BarcodeFormat.DATA_MATRIX,
)

fun BarcodeFormat.label() = name.replace('_', ' ')

fun BarcodeFormat.next() = FORMATS[(FORMATS.indexOf(this) + 1) % FORMATS.size]

/** Throws if [value] can't be drawn in [format] (e.g. letters in an EAN 13). */
fun encode(value: String, format: BarcodeFormat): BitMatrix =
    MultiFormatWriter().encode(value, format, 0, 0)

// ponytail: whole list as one JSON string, fine for dozens of cards; move to Room if it ever holds thousands
class CardStore(private val dataStore: DataStore<Preferences>) {
    private val key = stringPreferencesKey("cards")

    val cards: Flow<List<Card>> = dataStore.data.map { decode(it[key]) }

    suspend fun update(transform: (List<Card>) -> List<Card>) {
        dataStore.edit { it[key] = Json.encodeToString(transform(decode(it[key]))) }
    }

    private fun decode(json: String?): List<Card> =
        json?.let { Json.decodeFromString<List<Card>>(it) } ?: emptyList()
}
