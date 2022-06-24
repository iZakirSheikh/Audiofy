package com.prime.player.extended.managers

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.opengl.GLES10
import android.os.Build
import android.os.Parcelable
import android.provider.MediaStore
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.prime.player.R
import com.prime.player.extended.*
import kotlinx.coroutines.delay
import java.io.*
import java.lang.Math.sqrt
import java.lang.reflect.Method
import java.util.*


private const val TMP_LOGS_FILE = "logs.txt"
private const val TMP_DEVICE_INFO_FILE = "device_info.txt"

private const val DEFAULT_BITMAP_WIDTH = 640
private const val DEFAULT_BITMAP_HEIGHT = 480

/**
 * Convert the specified view to a drawable, if possible
 *
 * @param view the view to convert
 * @return the bitmap or `null` if the `view` is null
 */
private fun View.capture(): Bitmap {
    val bitmapToExport = Bitmap
        .createBitmap(
            if (width > 0) width else DEFAULT_BITMAP_WIDTH,
            if (height > 0) height else DEFAULT_BITMAP_HEIGHT,
            Bitmap.Config.ARGB_8888
        )
    val canvas = Canvas(bitmapToExport)
    draw(canvas)
    return bitmapToExport
}

/**
 * Returns total available memory in Megabytes.
 */
private val Context.totalMemory: Long?
    get() {
        return try {
            val mi = ActivityManager.MemoryInfo()
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.getMemoryInfo(mi)
            mi.totalMem / 1048576L
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


private val Context.isTablet: Boolean
    get() = resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_LARGE

/**
 * Returns available memory in Megabytes
 */
private val Context.freeMemory: Long?
    get() {
        return try {
            val mi = ActivityManager.MemoryInfo()
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.getMemoryInfo(mi)
            mi.availMem / 1048576L
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

private val deviceName: String
    get() {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.startsWith(manufacturer)) {
            model
        } else {
            "$manufacturer $model"
        }
    }


private val Context.isDeviceMoreThan5Inch: Boolean
    get() {
        return try {
            val displayMetrics: DisplayMetrics = resources.displayMetrics
            // int width = displayMetrics.widthPixels;
            // int height = displayMetrics.heightPixels;
            val yInches = displayMetrics.heightPixels / displayMetrics.ydpi
            val xInches = displayMetrics.widthPixels / displayMetrics.xdpi
            val diagonalInches = sqrt((xInches * xInches + yInches * yInches).toDouble())
            diagonalInches >= 7
        } catch (e: java.lang.Exception) {
            false
        }
    }

private val Context.appVersionName: String?
    get() {
        return try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            pInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            null
        }
    }

private val Context.networkType: String
    @RequiresPermission(android.Manifest.permission.READ_PHONE_STATE)
    get() {
        val type = (getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager).run {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) dataNetworkType else networkType
        }
        return when (type) {
            TelephonyManager.NETWORK_TYPE_HSDPA -> "Mobile Data 3G"
            TelephonyManager.NETWORK_TYPE_HSPAP -> "Mobile Data 4G"
            TelephonyManager.NETWORK_TYPE_GPRS -> "Mobile Data GPRS"
            TelephonyManager.NETWORK_TYPE_EDGE -> "Mobile Data EDGE 2G"
            else -> "Unknown"
        }
    }


private fun extractDeviceInfo(context: Activity): String {
    val builder = StringBuilder("\n\n ==== SYSTEM-INFO ===\n\n")
    with(builder) {
        // version
        val windowManager: WindowManager = context.windowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)

        append("\n\n ==== DEVICE ===\n\n")

        append("\n Device: $deviceName")
        append("Board: ${android.os.Build.BOARD}")
        append("\nBrand: ${android.os.Build.BRAND}")
        append("\nDevice: ${android.os.Build.DEVICE}")
        append("\nModel: ${android.os.Build.MODEL}")
        append("\nProduct: ${android.os.Build.PRODUCT}")
        append("\nTags: ${android.os.Build.TAGS}")
        append("\nLinux Version: ${System.getProperty("os.version")}")
        append("\nManufacturer: ${android.os.Build.MANUFACTURER}")
        append("\nHardware: ${android.os.Build.HARDWARE}")
        // append("\nCPU ABI: ${android.os.Build.CPU_ABI}")
        // append("\nCPU ABI2: ${android.os.Build.CPU_ABI2}")
        append("\n Total Memory: ${context.totalMemory} MBs")
        append("\n Free Memory: ${context.freeMemory} MBs")
        append(
            "\nDevice Type: ${
                kotlin.run {
                    if (context.isTablet) {
                        if (context.isDeviceMoreThan5Inch) {
                            "Tablet"
                        } else
                            "Mobile"
                    } else {
                        "Mobile"
                    }
                }
            }"
        )

        append("\n\n ==== DENSITY ===\n\n")

        append("Density ${metrics.density}")
        append("\nDensityDPI: ${metrics.densityDpi}")
        append("\nScaled Density: ${metrics.scaledDensity}")
        append("\nXDPI: ${metrics.xdpi}")
        append("\nYDPI: ${metrics.ydpi}")
        append("\nHeight Pixels: ${metrics.heightPixels}")
        append("\nWidth Pixels: ${metrics.widthPixels}")
        append(
            "\nResolution: ${
                String.format(
                    "%d x %d",
                    metrics.widthPixels,
                    metrics.heightPixels
                )
            }"
        )

        append("\n\n ==== OS ===\n\n")

        append("Android Release Version: ${android.os.Build.VERSION.RELEASE}")
        append("Build Version: ${android.os.Build.VERSION.INCREMENTAL}")
        append("Build Display: ${android.os.Build.DISPLAY}")
        append("Build Fingerprint: ${android.os.Build.FINGERPRINT}")
        append("Build ID: ${android.os.Build.ID}")
        append("Build Time: ${android.os.Build.TIME}")
        append("Build Type: ${android.os.Build.TYPE}")
        append("Build User: ${android.os.Build.USER}")
        append("\n SDK Version: SDK ${android.os.Build.VERSION.SDK_INT}")
        append("\n App Version Name: ${context.appVersionName}")
        append("\n Language: ${Locale.getDefault().displayLanguage}")
        append("\n TimeZone: ${TimeZone.getDefault().id}")

        append("\n\n ==== Misc ===\n\n")

        if (context.checkHasPermission(Manifest.permission.READ_PHONE_STATE))
            append("\n Active Data Network: ${kotlin.run { context.networkType }}")

        try {
            val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @SuppressWarnings("MissingPermission") val wifiInfo = wifiManager.connectionInfo
            val supplicantState = wifiInfo.supplicantState
            val openGlVersion = GLES10.glGetString(GLES10.GL_VERSION)

            append("\n Supplicant State: $supplicantState")
            append("\n Open GL Version = $openGlVersion")
        } catch (t: Throwable) {
            //no worries
        }

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        try {
            val cmClass = Class.forName(cm.javaClass.name)
            val method: Method = cmClass.getDeclaredMethod("getMobileDataEnabled")
            method.isAccessible = true
            val mobileDataEnabled = method.invoke(cm)
            append("\n Mobile Data Enabled: $mobileDataEnabled")
        } catch (t: Throwable) {
            // Private API access - no worries
        }

        try {
            val gpsEnabled =
                (context.getSystemService(Context.LOCATION_SERVICE) as LocationManager).isProviderEnabled(
                    LocationManager.GPS_PROVIDER
                )
            append("\n Mobile GPS Enabled: $gpsEnabled")
        } catch (t: Throwable) {
            //No worries
        }
    }
    return builder.toString()
}

fun extractLogToString(): String {
    val result = StringBuilder("\n\n ==== SYSTEM-LOG ===\n\n")
    val pid = android.os.Process.myPid()
    try {
        val command = String.format("logcat -d -v threadtime *:*")
        val process = Runtime.getRuntime().exec(command)
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        var currentLine: String?
        while (reader.readLine().also { currentLine = it } != null) {
            if (currentLine != null && currentLine!!.contains(pid.toString())) {
                result.append(currentLine)
                result.append("\n")
            }
        }
        //Runtime.getRuntime().exec("logcat -d -v time -f "+file.getAbsolutePath());
    } catch (e: IOException) {
        //Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
    }

    //clear the log
    try {
        Runtime.getRuntime().exec("logcat -c")
    } catch (e: IOException) {
        // Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
    }
    return result.toString()
}

private fun Context.getFileUri(name: String): Uri {
    val root: String = filesDir.path
    val file = File(root, name)
    return FileProvider.getUriForFile(this, "${packageName}.provider", file)
}

@Composable
fun Feedback(onDismissRequest: () -> Unit) {
    // extract info ahead
    val context = LocalContext.current as Activity
    val focusRequester = remember {
        FocusRequester()
    }
    // Accumulate all logs device info on first launch
    // generate their uris
    LaunchedEffect(key1 = Unit) {
        extractLogToString().also {
            val writer =
                OutputStreamWriter(context.openFileOutput(TMP_LOGS_FILE, Context.MODE_PRIVATE))
            writer.write(it)
            writer.close()
        }
        extractDeviceInfo(context).also {
            val writer = OutputStreamWriter(
                context.openFileOutput(
                    TMP_DEVICE_INFO_FILE,
                    Context.MODE_PRIVATE
                )
            )
            writer.write(it)
            writer.close()
        }
        delay(500)
        focusRequester.requestFocus()
    }

    val maxHeight = with(LocalDensity.current) { displayHeight * 0.4f }

    var text by remember {
        mutableStateOf("")
    }
    var uri by remember {
        mutableStateOf<Uri?>(null)
    }
    // image picker
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { imageUri ->
        imageUri?.let {
            uri = it
        }
    }

    PrimeDialog(
        title = "Send Feedback",
        onDismissRequest = { onDismissRequest() },
        vectorIcon = Icons.Outlined.Feedback,
        button1 = "DISMISS" to onDismissRequest,
        properties = DialogProperties(dismissOnClickOutside = false),
        button2 = "SUBMIT" to {
            onDismissRequest()
            // add logic
            //calculate URIs
            val email = "feedbacktoprime@gmail.com"
            val subject = context.getString(
                R.string.feedback_mail_subject,
                context.getString(R.string.app_name)
            )
            val uris = ArrayList<Uri>().also { list ->
                list.add(context.getFileUri(TMP_LOGS_FILE))
                list.add(context.getFileUri(TMP_DEVICE_INFO_FILE))
                uri?.let {
                    list.add(it)
                }
            }
            context.submit(email, text, uris = uris, subject)
            onDismissRequest()
        }) {

        Column(
            modifier = Modifier
                .heightIn(max = maxHeight)
                .padding(Padding.LARGE)
        ) {
            // text field
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                },
                label = {
                    Caption(text = stringResource(id = R.string.feedback_dialog_label))
                },
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.feedback_dialog_placeholder)
                    )
                },
                modifier = Modifier
                    .padding(vertical = Padding.LARGE)
                    .focusRequester(focusRequester)
                    .weight(0.7f)
            )


            Frame(
                modifier = Modifier
                    .weight(0.3f),
                color = MaterialTheme.colors.background,
                onClick = {
                    picker.launch("image/*")
                },
            ) {
                ScreenShot(uri = uri)
            }

            Text(
                text = buildAnnotatedString {
                    append("Your")
                    withStyle(style = SpanStyle(color = Color.SkyBlue)) {
                        append(" System info ")
                    }
                    append("and")
                    withStyle(style = SpanStyle(color = Color.SkyBlue)) {
                        append(" Logs ")
                    }
                    append("will be sent to Rhythm.")
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = LocalContentColor.current.copy(ContentAlpha.medium),
                modifier = Modifier.padding(vertical = Padding.MEDIUM)
            )

            Divider()
        }
    }
}

@Composable
private fun ScreenShot(uri: Uri?) {
    val context = LocalContext.current
    Crossfade(targetState = uri, modifier = Modifier.fillMaxSize()) { image ->
        when (image == null) {
            true -> Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Label(
                    text = "Add \nScreenshot",
                    modifier = Modifier.padding(Padding.MEDIUM),
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
                Icon(imageVector = Icons.Default.Image, contentDescription = null)
            }
            else -> {
                val image = remember(image) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ImageDecoder.createSource(context.contentResolver, image).run {
                            ImageDecoder.decodeBitmap(this)
                        }
                    } else {
                        MediaStore.Images.Media.getBitmap(
                            context.contentResolver,
                            image
                        )
                    }
                }
                Image(
                    bitmap = image.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillWidth
                )
            }
        }
    }
}


private fun Context.submit(
    email: String,
    feedback: String,
    uris: ArrayList<Uri>,
    subject: String
) {
    val source = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        // data = Uri.parse("mailto:") // only email apps should handle this
        putExtra(Intent.EXTRA_SUBJECT, subject)
        type = "text/plain"
        putExtra(Intent.EXTRA_EMAIL, Array(1) { email })
        putExtra(Intent.EXTRA_TEXT, feedback)
        if (uris.isNotEmpty()) {
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    val title = "Send Feedback to Rhythm"

    val intents: java.util.Stack<Intent?> = java.util.Stack()
    val i = Intent(
        Intent.ACTION_SENDTO, Uri.fromParts(
            "mailto",
            "info@domain.com", null
        )
    )
    val activities: List<ResolveInfo> = packageManager
        .queryIntentActivities(i, 0)

    for (ri in activities) {
        val target = Intent(source)
        target.setPackage(ri.activityInfo.packageName)
        intents.add(target)
    }

    val result = if (!intents.isEmpty()) {
        val chooserIntent = Intent.createChooser(
            intents.removeAt(0),
            title
        )
        chooserIntent.putExtra(
            Intent.EXTRA_INITIAL_INTENTS,
            intents.toArray(arrayOfNulls<Parcelable>(intents.size))
        )
        chooserIntent
    } else {
        Intent.createChooser(source, title)
    }

    try {
        startActivity(result)
    } catch (e: Exception) {
        Toast.makeText(this, "No Email App found!", Toast.LENGTH_SHORT).show()
    }
}

val LocalFeedbackCollector = staticCompositionLocalOf<Window> {
    error("No local feedback collector defined!!")
}