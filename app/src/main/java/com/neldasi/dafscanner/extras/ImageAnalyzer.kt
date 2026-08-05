package com.neldasi.dafscanner.extras

import android.graphics.ImageFormat
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BinaryBitmap
import com.google.zxing.LuminanceSource
import com.google.zxing.NotFoundException
import com.google.zxing.Reader
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.datamatrix.DataMatrixReader
import java.util.concurrent.Executor
import kotlin.math.min

private const val ZXING_CROP_SIZE_PX = 480

/**
 * A simple LuminanceSource for ZXing that wraps a grayscale byte array.
 * `luma` must be width * height bytes, row-major, 8-bit Y/gray.
 */
private class ByteArrayLuminanceSource(
    private val luma: ByteArray,
    dataWidth: Int,
    left: Int,
    top: Int,
    width: Int,
    height: Int,
) : LuminanceSource(width, height) {

    private val rowStride = dataWidth
    private val topOffset = top
    private val leftOffset = left

    override fun getRow(y: Int, row: ByteArray?): ByteArray {
        val outRow = row ?: ByteArray(width)
        val srcIndex = ((y + topOffset) * rowStride) + leftOffset
        System.arraycopy(luma, srcIndex, outRow, 0, width)
        return outRow
    }

    override fun getMatrix(): ByteArray {
        val area = ByteArray(width * height)
        var inputIndex = (topOffset * rowStride) + leftOffset
        var outIndex = 0
        repeat(height) {
            System.arraycopy(luma, inputIndex, area, outIndex, width)
            inputIndex += rowStride
            outIndex += width
        }
        return area
    }

    override fun isCropSupported(): Boolean = true
}

class DafImageAnalyzer(
    private val barcodeScanner: BarcodeScanner,
    // Listeners below are pinned to this executor so ML Kit's default (main-thread)
    // dispatch doesn't pull the ZXing fallback decode onto the UI thread.
    private val executor: Executor,
    private val onScannedValue: (String) -> Unit,
    private val onError: ((Throwable) -> Unit)? = null,
) {
    private var frameSkipCounter = 0
    private var lumaBuffer: ByteArray? = null
    private var rowDataBuffer: ByteArray? = null

    @OptIn(ExperimentalGetImage::class)
    fun process(imageProxy: ImageProxy, shouldProcess: Boolean) {
        // Throttle analysis: only run on every other frame to help stability / focus
        frameSkipCounter++
        if ((frameSkipCounter % 2 != 0) || !shouldProcess) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            Log.w("Scanner", "ImageProxy contained no image.")
            imageProxy.close()
            onError?.invoke(IllegalStateException("ImageProxy contained no image"))
            return
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        // 1. Primary path: ML Kit
        barcodeScanner.process(inputImage)
            .addOnSuccessListener(executor) { barcodes ->
                Log.d("Scanner", "Detected ${barcodes.size} barcodes (ML Kit)")
                val firstValue = barcodes.firstOrNull { it.rawValue != null }?.rawValue

                if (firstValue != null) {
                    onScannedValue(firstValue)
                    return@addOnSuccessListener
                }

                // 2. Fallback: ZXing on center crop (Data Matrix only)
                try {
                    val triple = extractLumaPlane(imageProxy)
                    if (triple != null) {
                        val (lumaBytes, w, h) = triple
                        val zxingResult = tryDecodeWithZXingCenterCrop(lumaBytes, w, h)
                        if (zxingResult != null) {
                            Log.d("Scanner", "ZXing decoded Data Matrix: $zxingResult")
                            onScannedValue(zxingResult)
                            return@addOnSuccessListener
                        }
                    }
                } catch (zxEx: Exception) {
                    Log.e("Scanner", "ZXing fallback failed", zxEx)
                }
            }
            .addOnFailureListener(executor) { exception ->
                Log.e("Scanner", "Barcode scanning failed (ML Kit)", exception)
                onError?.invoke(exception)
            }
            .addOnCompleteListener(executor) {
                imageProxy.close()
            }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun extractLumaPlane(imageProxy: ImageProxy): Triple<ByteArray, Int, Int>? {
        val image = imageProxy.image ?: return null
        if ((image.format != ImageFormat.YUV_420_888) && (imageProxy.format != ImageFormat.YUV_420_888)) {
            return null
        }

        val yPlane = imageProxy.planes[0]
        val yBuffer = yPlane.buffer
        val yRowStride = yPlane.rowStride
        val yPixelStride = yPlane.pixelStride

        val width = imageProxy.width
        val height = imageProxy.height

        // Reuse buffers to reduce GC pressure
        val size = width * height
        if (lumaBuffer == null || lumaBuffer!!.size != size) {
            lumaBuffer = ByteArray(size)
        }
        val out = lumaBuffer!!

        if (rowDataBuffer == null || rowDataBuffer!!.size != yRowStride) {
            rowDataBuffer = ByteArray(yRowStride)
        }
        val rowData = rowDataBuffer!!

        var outIndex = 0
        for (row in 0 until height) {
            yBuffer.position(row * yRowStride)
            yBuffer.get(rowData, 0, yRowStride.coerceAtMost(yBuffer.remaining()))
            var col = 0
            while (col < width) {
                out[outIndex++] = rowData[col * yPixelStride]
                col++
            }
        }

        return Triple(out, width, height)
    }

    private fun tryDecodeWithZXingCenterCrop(luma: ByteArray, width: Int, height: Int): String? {
        val cropSize = ZXING_CROP_SIZE_PX
        val cropWidth = min(cropSize, width)
        val cropHeight = min(cropSize, height)
        val left = ((width - cropWidth) / 2).coerceAtLeast(0)
        val top = ((height - cropHeight) / 2).coerceAtLeast(0)

        val source: LuminanceSource = ByteArrayLuminanceSource(
            luma = luma,
            dataWidth = width,
            left = left,
            top = top,
            width = cropWidth,
            height = cropHeight,
        )

        val bitmap = BinaryBitmap(HybridBinarizer(source))
        val reader: Reader = DataMatrixReader()
        return try {
            val result: Result = reader.decode(bitmap)
            result.text
        } catch (_: NotFoundException) {
            null
        } catch (_: Exception) {
            null
        }
    }
}
