@file:Suppress("unused")

package com.prime.media.core.coil

import android.graphics.Bitmap
import android.graphics.HardwareRenderer
import android.graphics.PixelFormat
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.os.Build
import androidx.annotation.RequiresApi
import coil.size.Size
import coil.transform.Transformation

private const val TAG = "RsBlurTransformation"

private const val DEFAULT_RADIUS = 10f
private const val DEFAULT_SAMPLING = 1f

private val Bitmap.safeConfig: Bitmap.Config
    get() = config ?: Bitmap.Config.ARGB_8888

/**
 * A [Transformation] that applies a Gaussian blur to an image.
 *
 * @param radius The radius of the blur.
 * @param sampling The sampling multiplier used to scale the image. Values > 1
 *  will downscale the image. Values between 0 and 1 will upscale the image.
 */
@RequiresApi(Build.VERSION_CODES.S)
class ReBlurTransformation @JvmOverloads constructor(
    private val radius: Float = DEFAULT_RADIUS,
    private val sampling: Float = DEFAULT_SAMPLING
) : Transformation {

    init {
        //require(radius in 0.0..25.0) { "radius must be in [0, 25]." }
        require(sampling > 0) { "sampling must be > 0." }
    }

    @Suppress("NullableToStringCall")
    override val cacheKey = "${ReBlurTransformation::class.java.name}-$radius-$sampling"


    override suspend fun transform(bitmap: Bitmap, size: Size): Bitmap {
        val imageReader = ImageReader.newInstance(
            bitmap.width, bitmap.height,
            PixelFormat.RGBA_8888, 1,
            HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
        )
        val renderNode = RenderNode("BlurEffect")
        val hardwareRenderer = HardwareRenderer()

        hardwareRenderer.setSurface(imageReader.surface)
        hardwareRenderer.setContentRoot(renderNode)
        renderNode.setPosition(0, 0, imageReader.width, imageReader.height)
        val blurRenderEffect = RenderEffect.createBlurEffect(
            radius, radius,
            Shader.TileMode.MIRROR
        )
        renderNode.setRenderEffect(blurRenderEffect)
        var output: Bitmap? = null
        try {
            val renderCanvas = renderNode.beginRecording()
            renderCanvas.drawBitmap(bitmap, 0f, 0f, null)
            renderNode.endRecording()
            hardwareRenderer.createRenderRequest()
                .setWaitForPresent(true)
                .syncAndDraw()
            val image = imageReader.acquireNextImage() ?: throw RuntimeException("No Image")
            val hardwareBuffer = image.hardwareBuffer ?: throw RuntimeException("No HardwareBuffer")
            output = Bitmap.wrapHardwareBuffer(hardwareBuffer, null)
                ?: throw RuntimeException("Create Bitmap Failed")
        } finally {
            //  hardwareBuffer.close()
            //image.close()
            imageReader.close()
            renderNode.discardDisplayList()
            hardwareRenderer.destroy()
        }

        return output ?: bitmap
    }
}