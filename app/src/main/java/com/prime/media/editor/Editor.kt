@file:OptIn(ExperimentalTextApi::class)
@file:Suppress("FunctionName", "NOTHING_TO_INLINE")

package com.prime.media.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.HideImage
import androidx.compose.material.icons.outlined.ReplyAll
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Shop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.prime.media.BuildConfig
import com.prime.media.Material
import com.prime.media.R
import com.prime.media.backgroundColorAtElevation
import com.prime.media.core.ContentPadding
import com.prime.media.core.billing.purchased
import com.prime.media.core.compose.LocalNavController
import com.prime.media.core.compose.LocalSystemFacade
import com.prime.media.core.compose.purchase
import com.prime.media.outline
import com.prime.media.overlay
import com.prime.media.small2
import com.primex.core.MetroGreen
import com.primex.core.MetroGreen2
import com.primex.core.composableOrNull
import com.primex.core.drawHorizontalDivider
import com.primex.core.getText2
import com.primex.core.gradient
import com.primex.core.rememberVectorPainter
import com.primex.core.textResource
import com.primex.material2.Label
import com.primex.material2.ListTile
import com.primex.material2.Text
import com.primex.material2.TextButton

private const val TAG = "TagEditor"

/**
 * A VisualTransformation that applies styling to timestamps in the [MM:SS:XX] format within text.
 *
 * @property text The input text containing timestamps to be styled.
 * @return A transformed AnnotatedString with timestamps styled in bold.
 */
private val LRCVisualTransformation =
    VisualTransformation { text ->
        // Define a regular expression to match the [MM:SS.XX] format
        val regex = """\[\d{2}:\d{2}.\d{2}\]""".toRegex()
        // Create a new AnnotatedString builder to store the transformed text
        val builder = AnnotatedString.Builder()
        // Loop through the text and append the matched and unmatched parts with different styles
        var cursor = 0
        regex.findAll(text.text).forEach { matchResult ->
            // Append the unmatched part with the original style
            builder.append(text.subSequence(cursor, matchResult.range.first))
            // Append the matched part with a bold style
            builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            builder.append(text.subSequence(matchResult.range))
            builder.pop()
            // Update the cursor position
            cursor = matchResult.range.last + 1
        }
        // Append the remaining part of the text with the original style
        builder.append(text.subSequence(cursor, text.length))
        // Return the transformed text with an identity offset mapping
        TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }

/**
 * Creates a VisualTransformation for adding an affix (prefix or suffix) to the input text.
 *
 * @param affix The affix (prefix or suffix) to be added to the text.
 * @param isPrefix A flag indicating whether the affix should be added as a prefix (true) or suffix (false).
 * @return A VisualTransformation that adds the specified affix to the input text.
 */
private fun AffixVisualTransformation(
    affix: CharSequence,
    isPrefix: Boolean = true
) = VisualTransformation { text ->
    TransformedText(
        // Create a buildAnnotatedString to store the transformed text
        text = buildAnnotatedString {
            // Add prefix if is
            if (isPrefix) append(affix)
            append(text)
            // else add as suffix
            if (!isPrefix) append(affix)
        },
        // Define an OffsetMapping to map between original and transformed text positions
        offsetMapping = object : OffsetMapping {
            // transformed
            override fun originalToTransformed(offset: Int): Int = offset + affix.length
            override fun transformedToOriginal(offset: Int): Int = offset - affix.length
        }
    )
}

private val DefaultKeyboardOptions =
    KeyboardOptions(
        capitalization = KeyboardCapitalization.Sentences,
        imeAction = ImeAction.Next
    )

context(LazyGridScope)
private inline fun Property(
    @StringRes title: Int,
    value: TextFieldValue,
    noinline onValueChange: (TextFieldValue) -> Unit,
    @StringRes placeholder: Int = ResourcesCompat.ID_NULL,
    maxLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = DefaultKeyboardOptions,
    enabled: Boolean = true,
    noinline span: (LazyGridItemSpanScope.() -> GridItemSpan)? = null,
) {
    item(key = title, contentType = "property", span = span) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Label(text = textResource(id = title)) },
            maxLines = maxLines,
            readOnly = !enabled,
            placeholder = composableOrNull(placeholder != ResourcesCompat.ID_NULL) {
                Label(text = if (placeholder != ResourcesCompat.ID_NULL) textResource(id = placeholder) else "")
            },
            singleLine = maxLines == 1,
            shape = MaterialTheme.shapes.small2,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            modifier = Modifier
                .defaultMinSize(minHeight = 64.dp)
                .onFocusChanged {
                    val selection =
                        if (it.hasFocus) TextRange(0, value.text.length) else TextRange.Zero
                    onValueChange(value.copy(selection = selection))
                }
        )
    }
}

context(LazyGridScope)
private inline fun Info(
    value: CharSequence
) {
    item(key = "info", contentType = "Info") {
        val color = LocalContentColor.current
        Text(
            style = MaterialTheme.typography.body2,
            color = color,
            modifier = Modifier.padding(ContentPadding.normal, ContentPadding.medium),
            text = value
        )
    }
}

private val fullLineSpan: (LazyGridItemSpanScope.() -> GridItemSpan) = { GridItemSpan(maxLineSpan) }
context(LazyGridScope)
private inline fun Lyrics(
    value: TextFieldValue,
    noinline onValueChange: (TextFieldValue) -> Unit,
    enabled: Boolean = true,
) {
    item("lyrics", fullLineSpan, "Lyrics") {
        TextField(
            value = value,
            onValueChange = onValueChange,
            minLines = 10,
            maxLines = 12,
            shape = Material.shapes.small2,
            readOnly = !enabled,
            visualTransformation = LRCVisualTransformation,
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = Material.colors.overlay,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            enabled = enabled,
            placeholder = { Text(textResource(id = R.string.tag_editor_lyrics_placeholder)) },
        )
    }
}

private val PickMediaRequest =
    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
private val ArtworkShape = RoundedCornerShape(12)

context(LazyGridScope)
private inline fun Artwork(
    image: ImageBitmap?,
    crossinline onRequestChange: (new: Uri?) -> Unit
) {
    item(key = "artwork", contentType = "Artwork") {
        val purchase by purchase(id = BuildConfig.IAP_TAG_EDITOR_PRO)
        val facade = LocalSystemFacade.current
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
            when {
                it == null -> return@rememberLauncherForActivityResult // just return.
                purchase.purchased -> onRequestChange(it)
                else -> facade.show(R.string.msg_upgrade_to_pro)
            }
        }
        IconButton(onClick = { launcher.launch(PickMediaRequest) }) {
            // Backdrop image
            Image(
                painter = if (image != null)
                    BitmapPainter(image) else
                    rememberVectorPainter(
                        image = Icons.Outlined.HideImage,
                        tintColor = Material.colors.onBackground
                    ),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .clip(ArtworkShape)
                    .background(Material.colors.overlay, ArtworkShape)
                    .aspectRatio(1.0f) // different when width > height
                    .gradient(true, listOf(Color.Black.copy(0.3f), Color.Black.copy(0.3f)))
            )
            // Representational Icon.
            Icon(imageVector = Icons.Outlined.Edit, contentDescription = null, tint = Color.White)
        }
    }
}

context(LazyGridScope)
private inline fun BuyMe() {
    item("buy_me", contentType = "Buy Me", span = fullLineSpan) {
        val facade = LocalSystemFacade.current
        ListTile(
            overline = { Spacer(modifier = Modifier) },
            headline = { Spacer(modifier = Modifier) },
            footer = {
                TextButton(
                    label = textResource(id = R.string.tag_editor_screen_buy_now_action),
                    onClick = { facade.launchBillingFlow(BuildConfig.IAP_TAG_EDITOR_PRO) })
            },
            color = Material.colors.backgroundColorAtElevation(1.dp),
            onColor = Material.colors.onBackground,
            shape = Material.shapes.small2,
            leading = {
                Icon(
                    imageVector = Icons.Outlined.Shop,
                    contentDescription = null,
                    tint = Color.MetroGreen
                )
            },
            subtitle = { Text(text = textResource(id = R.string.msg_tag_editor_buy_me_banner)) },
        )
    }
}

context(LazyGridScope)
private inline fun Header(@StringRes id: Int) {
    item(key = id, contentType = "Header", span = fullLineSpan) {
        val primary = MaterialTheme.colors.secondary
        Label(
            text = textResource(id = id),
            fontWeight = FontWeight.SemiBold,
            color = primary,
            modifier = Modifier
                .padding(vertical = ContentPadding.normal)
                .fillMaxWidth()
                .drawHorizontalDivider(color = primary)
                .padding(bottom = ContentPadding.medium),
        )
    }
}

private val LayoutIndent = PaddingValues(
    start = ContentPadding.normal,
    end = ContentPadding.normal,
    top = ContentPadding.large,
    bottom = 120.dp
)

@Composable
private fun Layout(
    state: TagEditor,
    modifier: Modifier = Modifier,
) {
    val isProVersion by purchase(id = BuildConfig.IAP_TAG_EDITOR_PRO)
    val facade = LocalSystemFacade.current
    val ctx = LocalContext.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(ContentPadding.large),
        verticalArrangement = Arrangement.spacedBy(ContentPadding.normal),
        modifier = modifier,
        contentPadding = LayoutIndent
    ) {
        // Artwork
        Artwork(state.artwork, state::setArtwork)
        //Info
        Info(state.extraInfo ?: "")
        // BuyMe Banner
        if (!isProVersion.purchased)
            BuyMe()
        // Title
        Property(
            title = R.string.title,
            value = state.title,
            onValueChange = { state.title = it },
            span = fullLineSpan,
            placeholder = R.string.tag_editor_property_placeholder
        )
        // Album
        Property(
            title = R.string.album,
            value = state.album,
            onValueChange = { state.album = it },
            placeholder = R.string.tag_editor_property_placeholder
        )
        // Artist
        Property(
            title = R.string.artist,
            value = state.artist,
            onValueChange = { state.artist = it },
            placeholder = R.string.tag_editor_property_placeholder
        )
        // Album Artist
        Property(
            title = R.string.album_artist,
            value = state.albumArtist,
            onValueChange = { state.albumArtist = it },
            placeholder = R.string.tag_editor_property_placeholder
        )
        // Composer
        Property(
            title = R.string.composer,
            value = state.composer,
            onValueChange = { state.composer = it },
            placeholder = R.string.tag_editor_property_placeholder
        )
        // Genre
        Property(
            title = R.string.genre,
            value = state.genre,
            onValueChange = { state.genre = it },
            placeholder = R.string.tag_editor_property_placeholder,
        )
        // comment
        Property(
            title = R.string.comment,
            value = state.comment,
            onValueChange = { state.comment = it },
            placeholder = R.string.tag_editor_property_placeholder,
            span = fullLineSpan,
        )

        // Original Artist
        Property(
            title = R.string.original_artist,
            value = state.originalArtist,
            onValueChange = { state.originalArtist = it },
            placeholder = R.string.tag_editor_property_placeholder
        )

        // Publisher
        Property(
            title = R.string.publisher,
            value = state.publisher,
            onValueChange = { state.publisher = it },
            placeholder = R.string.tag_editor_property_placeholder
        )

        // Copywriter
        Property(
            title = R.string.copywriter,
            value = state.copyright,
            onValueChange = { state.copyright = it },
            placeholder = R.string.tag_editor_property_placeholder,
        )
        // number
        val KeyboardTypeNumber =
            KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
        // Year
        Property(
            title = R.string.year,
            value = state.year,
            onValueChange = { state.year = it },
            placeholder = R.string.tag_editor_property_placeholder,
            keyboardOptions = KeyboardTypeNumber
        )
        // Track Number
        Property(
            title = R.string.track_number,
            value = state.trackNumber,
            onValueChange = { state.trackNumber = it },
            placeholder = R.string.tag_editor_property_placeholder,
            keyboardOptions = KeyboardTypeNumber
        )

        // FixMe - Removing Affix Visual Transformer from this as this is casing crash due to
        //  offset mismatch
        // TODO - Suggestion: Instead attach http only if there isn't any and apply green color to it.
        // Url
        Property(
            title = R.string.url,
            value = state.url,
            onValueChange = { state.url = it },
            placeholder = R.string.tag_editor_property_placeholder,
            span = fullLineSpan,
         //   visualTransformation = AffixVisualTransformation(ctx.resources.getText2(R.string.tag_editor_web_address_prefix))
        )
        // Disk Number
        Property(
            title = R.string.disk_number,
            value = state.diskNumber,
            onValueChange = { state.diskNumber = it },
            placeholder = R.string.tag_editor_property_placeholder,
            keyboardOptions = KeyboardTypeNumber
        )
        // Total Disks
        Property(
            title = R.string.total_disks,
            value = state.totalDisks,
            onValueChange = { state.totalDisks = it },
            placeholder = R.string.tag_editor_property_placeholder,
            keyboardOptions = KeyboardTypeNumber
        )
        //Header
        Header(id = R.string.lyrics)
        // Lyrics
        Lyrics(
            state.lyrics,
            onValueChange = { state.lyrics = it },
            enabled = isProVersion.purchased
        )
    }
}

private val TopBarDividerIndent = PaddingValues(
    start = ContentPadding.large,
    end = ContentPadding.xLarge,
    top = ContentPadding.medium
)

@Composable
@NonRestartableComposable
private fun Toolbar(
    state: TagEditor,
    modifier: Modifier = Modifier
) {
    val purchase by purchase(id = BuildConfig.IAP_TAG_EDITOR_PRO)
    androidx.compose.material.TopAppBar(
        title = { Label(text =  textResource(id = if (purchase.purchased) R.string.tag_editor_scr_pro_title else R.string.tag_editor_scr_title)  ) },
        navigationIcon = {
            val navigator = LocalNavController.current
            com.primex.material2.IconButton(
                imageVector = Icons.Outlined.ReplyAll,
                contentDescription = null,
                onClick = { navigator.navigateUp() }
            )
        },
        backgroundColor = MaterialTheme.colors.background,
        modifier = modifier.drawHorizontalDivider(
            Material.colors.outline,
            indent = TopBarDividerIndent
        ),
        elevation = 0.dp,
        actions = {
            com.primex.material2.IconButton(
                imageVector = Icons.Outlined.Restore,
                contentDescription = null,
                onClick = { state.reset() }
            )
        }
    )
}

private val FloatingActionButtonShape = RoundedCornerShape(30)
@Composable
fun TagEditor(state: TagEditor) {
    Scaffold(
        topBar = { Toolbar(state, Modifier.statusBarsPadding()) },
        floatingActionButton = {
            val ctx = LocalContext.current
            val facade = LocalSystemFacade.current
            FloatingActionButton(
                onClick = { state.save(ctx); facade.showAd() },
                content = {
                    Icon(imageVector = Icons.Outlined.Save, contentDescription = null)
                },
                shape = FloatingActionButtonShape,
                backgroundColor = Color.MetroGreen2,
                contentColor = Material.colors.onPrimary
            )
        },
        content = {
            Layout(state = state, modifier = Modifier.padding(it))
        }
    )
}

