package com.prime.player.extended.managers

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.annotation.IntRange
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.prime.player.R
import com.prime.player.extended.*
import com.prime.player.preferences.Preferences
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

private data class Review(
    @IntRange(from = 1, to = 5)
    val rating: Int,
    val version: Long
)

/**
 * **Automatically manages user reviews.**
 *
 *  @param delayMills: The initial delay when no review has been asked.
 *  @param delayTimes: The initial delay times when no review has been recorded.
 *  @param delayMillsAfter: The delay in mills after the user posted bad review with rating <= recall
 *  @param delayTimesAfter: The dayay in launches after the user posted bad review with rating <= recall
 *  @param delayAskLater: if the user asked to review latter, it represents the max time when to re-ask for review
 *  @param recall: The rating (and its lesser) which is considered bad.
 *  @param priority: A list of [Market] in descending order o priority.
 *
 */
class ReviewManager(
    private val delayMills: Long = DEFAULT_INITIAL_DELAY_MILLS,
    private val delayTimes: Int = DEFAULT_INITIAL_MAX_LAUNCHES,
    private val delayMillsAfter: Long = DEFAULT_AFTER_DELAY_MILLS,
    private val delayTimesAfter: Int = DEFAULT_AFTER_MAX_LAUNCHES,
    private val delayAskLater: Long = DEFAULT_DELAY_ASK_LATER,
    @IntRange(from = 1, to = 5)
    private val recall: Int = DEFAULT_RECALL_RATING,
    private val context: Context,
    private val priority: List<Market>,
) {

    private val prefs = Preferences.get(context)
    private val analytics = Firebase.analytics

    private val gson = Gson()

    /**
     * The old saved review
     */
    private var review: Review?
        get() = with(prefs) {
            getString(KEY_REVIEW, "").map { value ->
                if (value.isEmpty())
                    null
                else {
                    gson.fromJson(value, Review::class.java)
                }
            }.collectBlocking()
        }
        set(value) {
            val coded = value?.let { gson.toJson(value) } ?: ""
            runBlocking {
                prefs.setString(KEY_REVIEW, coded)
            }
        }

    /**
     * The number of launches that happened when user reviewed. this depends on old [review], if it is null
     * the user haven't reviewed yet.
     */
    private var launches: Long
        get() = with(prefs) { getLong(KEY_LAUNCH_COUNTER, 0).collectBlocking() }
        set(value) {
            runBlocking {
                prefs.setLong(KEY_LAUNCH_COUNTER, value)
            }
        }

    /**
     * The time in mills when we showed the review dialog and user pressed Ask Me Later. default value -1
     */
    private var whenAskedLater: Long
        get() = with(prefs) { getLong(KEY_WHEN_ASKED, -1).collectBlocking() }
        set(value) {
            runBlocking {
                prefs.setLong(KEY_WHEN_ASKED, value)
            }
        }

    /**
     * The anchor in timeline against we compare the time.
     */
    private var epoch: Long
        get() = with(prefs) { getLong(KEY_EPOCH, -1L).collectBlocking() }
        set(value) {
            runBlocking {
                prefs.setLong(KEY_EPOCH, value)
            }
        }


    private var savedVersion: Long
        get() = with(prefs) { getLong(KEY_SAVED_VERSION, -1L).collectBlocking() }
        set(value) {
            runBlocking {
                prefs.setLong(KEY_SAVED_VERSION, value)
            }
        }

    /**
     * user requested dismiss for now
     *
     * Resets automatic conditions like [epoch] to [System.currentTimeMillis], [launches] to 0,
     * [whenAskedLater] to [-1L] i.e., default conditions, leaving [review] as it is, next time the
     * condition apply based on [review] if it is saved post types like [delayTimesAfter] etc.
     * otherwise [delayTimes]
     */
    fun reset() {
        // clear conditions for auto show
        // reset epoch to now
        epoch = System.currentTimeMillis()
        launches = 0
        whenAskedLater = -1L
    }

    /**
     * Returns the old rating given by the user. or -1
     */
    fun rating() = review?.rating ?: -1

    /**
     * Resets all automatic conditions to default and sets [whenAskedLater] to [System.currentTimeMillis]
     * when can be used to automatically start dialog on next launch when [delayAskLater] has passed
     */
    fun requestAskLater() {
        //
        reset()
        // re-init when asked
        whenAskedLater = System.currentTimeMillis()
    }

    // single this is a Global, it will run once when app is launched.
    init {
        // init or rest if epoch == -1L or saved version is less than installedVersion
        val sv = savedVersion
        val iv = context.installedVersion
        // check if it a new install
        if (sv == -1L)
            analytics.logEvent("prime_new_install", null)
        // new update
        if (sv < iv)
            analytics.logEvent("prime_update_$iv", null)
        // if epoch is un-init or saved version is un-init or user updated the app, reset the init
        // conditions
        if (epoch == -1L || sv == -1L || sv < iv) reset()

        // update saved version pref in case update happened or it is un-init
        if (sv == -1L || sv < iv) savedVersion = iv
        // increment launch counter by 1
        launches += 1
    }

    /** check whether, we should show rate dialog based on automatic conditions.
     * @return: true if conditions are met otherwise false.
     */
    fun showRatingDialog(): Boolean {
        return kotlin.run {
            // check maybe user ask for later
            val whn = whenAskedLater
            // if time of delay has passed return true
            whn != -1L && (System.currentTimeMillis() - whn > delayAskLater)
        }
                || kotlin.run {
            val old = review
            when (old == null) {
                // if no review is available
                // check if conditions are met
                true -> launches > delayTimes
                        && (System.currentTimeMillis() - epoch) > delayMills
                // only ask if app has been updated
                // rating is <= recall
                // and contains of after are met.
                else -> context.installedVersion > old.version
                        && old.rating <= recall && launches > delayTimesAfter
                        && (System.currentTimeMillis() - epoch) > delayMillsAfter
            }
        }
    }

    /**
     * Saves the review locally and returns [Intent] of available market.
     *
     * @return [Intent] of available market based on [priority] or null if [rating] is less or equal
     * to [recall] {which might be used to ask for feedback}
     * @throws [IllegalStateException] in case no market app suggested in [priority] is available
     */
    @Throws(IllegalStateException::class)
    private fun submit(@IntRange(from = 1, to = 5) rating: Int): Intent? {
        //save review locally
        review = Review(rating, context.installedVersion)
        // reset any how.
        reset()
        // return if rating is less than recall
        if (rating <= recall) return null

        // select market from [priority]
        var selected: Market? = null
        // select first available market
        for (market in priority) {
            //check is installed and enabled
            val available = context.isAppInstalled(MarketPackage.map(market = market))
            if (available) {
                selected = market
                //break
                break
            }
        }
        if (selected == null)
            throw IllegalStateException(
                "No app market installed!!. Please install and app market" +
                        " like Google Play Store, Samsung App Store etc."
            )

        return selected.intent
    }


    /**
     * This should not be here but adjust for now
     */
    fun Context.rate(@IntRange(from = 1, to = 5) rating: Int, feedbackCollector: Window) {
        val msg = try {
            when (rating == -1) {
                true -> "Please tap on star to select a rating."
                else -> {
                    submit(rating)?.let {
                        context.startActivity(it)
                    } ?: kotlin.run {
                        feedbackCollector.show()
                    }
                    null
                }
            }
        } catch (e: IllegalStateException) {
            e.message
        }
        msg?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }


    companion object {
        private const val TAG = "ReviewManager"
        private const val KEY_LAUNCH_COUNTER = TAG + "_counter"
        private const val KEY_WHEN_ASKED = TAG + "_when_asked"
        private const val KEY_EPOCH = TAG + "_from_when"
        private const val KEY_REVIEW = TAG + "_review"
        private const val KEY_SAVED_VERSION = TAG + "_saved_version"


        private val DEFAULT_INITIAL_DELAY_MILLS = TimeUnit.DAYS.toMillis(3)
        private val DEFAULT_AFTER_DELAY_MILLS = TimeUnit.DAYS.toMillis(6)
        private val DEFAULT_DELAY_ASK_LATER = TimeUnit.HOURS.toMillis(3)

        private const val DEFAULT_RECALL_RATING = 3

        private const val DEFAULT_INITIAL_MAX_LAUNCHES = 10
        private const val DEFAULT_AFTER_MAX_LAUNCHES = 20
    }
}

private fun toEmotion(rating: Int): Pair<Int, String> {
    return when (rating) {
        -1 -> R.raw.emoji_excited to "Rate us"
        1 -> R.raw.emoji_sad to "Hate it"
        2 -> R.raw.sad_less_emoji to "Dislike it"
        3 -> R.raw.emoji_poker_face to "It's Ok"
        4 -> R.raw.emoji_like_it to "Like it"
        else -> R.raw.emoji_love_it to "Love it"
    }
}


@Composable
fun ReviewManager.Dialog(onDismissRequest: () -> Unit) {

    var rating by remember {
        mutableStateOf(rating())
    }

    val context = LocalContext.current
    val provider = LocalFeedbackCollector.current

    PrimeDialog(
        title = "Rate us",
        subtitle = "Let us know what you think.",
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = false),
        vectorIcon = Icons.Default.RateReview,
        imageButton = Icons.Default.Close to {
            requestAskLater()
            onDismissRequest()
        },
        button1 = "DISMISS" to {
            // dismisses dialog
            reset()
            onDismissRequest()
        },
        button2 = "SUBMIT" to {
            context.rate(rating, provider)
            onDismissRequest()
        }
    ) {
        Column(
            modifier = Modifier.padding(Padding.LARGE),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            val emotion = toEmotion(rating)
            val primary = MaterialTheme.colors.primary

            Crossfade(
                targetState = emotion,
                modifier = Modifier
                    .padding(vertical = Padding.MEDIUM)
                    .align(Alignment.CenterHorizontally)
                    .animate()
            ) {

                Lottie(
                    res = it.first,
                    autoPlay = true,
                    modifier = Modifier.size(100.dp)
                )
            }

            Crossfade(
                targetState = emotion,
                modifier = Modifier
                    .padding(vertical = Padding.MEDIUM)
                    .align(Alignment.CenterHorizontally)
                    .animate()
            ) {
                Header5(
                    text = it.second,
                    color = primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(horizontal = Padding.MEDIUM)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(5) { index ->
                    val vector = if (rating >= index + 1)
                        Icons.Filled.Star
                    else
                        Icons.Default.StarBorder

                    IconButton(
                        modifier = Modifier.requiredSize(46.dp),
                        onClick = { rating = index + 1 },
                    ) {
                        Icon(
                            imageVector = vector,
                            contentDescription = null,
                            tint = primary,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.review_msg),
                color = LocalContentColor.current.copy(ContentAlpha.medium),
                modifier = Modifier.padding(vertical = Padding.LARGE)
            )
            Divider()
        }
    }
}


val LocalReviewCollector = staticCompositionLocalOf<Window> {
    error("No Review Manager defined!!")
}