package com.example.arm64opencvcamera

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.exp
import kotlin.math.roundToInt

/**
 * Wraps a trained TFLite skin-symptom classifier for fully offline, on-device
 * inference. Drop your exported model + labels file into app/src/main/assets/
 * with the filenames below.
 *
 * This model (MobileNetV2, INT8-quantized, with a Keras Rescaling layer BAKED
 * INTO the graph) expects RAW 0-255 pixel values as input — the model itself
 * handles normalization internally. We do NOT pre-normalize pixels ourselves;
 * doing so would double-normalize and silently degrade accuracy. Instead we
 * read the input tensor's own quantization params at runtime and quantize
 * raw pixel values against those — this is correct regardless of exactly how
 * the model was exported, rather than hardcoding scale/zero-point guesses.
 *
 * IMPORTANT: this returns raw model output only — ranked labels with confidence
 * scores. It does NOT decide anything is "diagnosed". Callers are responsible
 * for applying a confidence threshold and showing results as possible matches,
 * not conclusions — see MainActivity's classifySkinRegion() for how this is used.
 */
class SkinClassifier(context: Context) {

    companion object {
        private const val MODEL_FILE = "skin_model.tflite"
        private const val LABELS_FILE = "skin_labels.txt"
        private const val INPUT_SIZE = 224 // confirmed from this model's architecture (mobilenetv2_1.00_224)

        // If your Keras model's final Dense layer does NOT have activation='softmax'
        // (i.e. it outputs raw logits), keep this true so we apply softmax here.
        // If it DOES already end in softmax, set this to false.
        // Check your training script's model.summary() / last layer definition to confirm.
        private const val OUTPUT_IS_LOGITS = true
    }

    private val interpreter: Interpreter
    private val labels: List<String>
    private val inputDataType: DataType
    private val inputScale: Float
    private val inputZeroPoint: Int
    private val outputDataType: DataType
    private val outputScale: Float
    private val outputZeroPoint: Int
    private val numClasses: Int

    init {
        interpreter = Interpreter(loadModelFile(context))
        labels = context.assets.open(LABELS_FILE).bufferedReader().readLines().filter { it.isNotBlank() }

        val inputTensor = interpreter.getInputTensor(0)
        inputDataType = inputTensor.dataType()
        val inQ = inputTensor.quantizationParams()
        inputScale = if (inQ.getScale() != 0f) inQ.getScale() else 1f
        inputZeroPoint = inQ.getZeroPoint()

        val outputTensor = interpreter.getOutputTensor(0)
        outputDataType = outputTensor.dataType()
        val outQ = outputTensor.quantizationParams()
        outputScale = if (outQ.getScale() != 0f) outQ.getScale() else 1f
        outputZeroPoint = outQ.getZeroPoint()
        numClasses = outputTensor.shape().last()

        require(labels.size == numClasses) {
            "skin_labels.txt has ${labels.size} lines but the model outputs $numClasses classes — " +
                    "these must match exactly, in the same order your training generator used."
        }
    }

    private fun loadModelFile(context: Context): MappedByteBuffer {
        val fd = context.assets.openFd(MODEL_FILE)
        val inputStream = FileInputStream(fd.fileDescriptor)
        return inputStream.channel.map(
            FileChannel.MapMode.READ_ONLY,
            fd.startOffset,
            fd.declaredLength
        )
    }

    /** Returns (label, confidence 0..1) pairs sorted highest confidence first. */
    fun classify(bitmap: Bitmap): List<Pair<String, Float>> {
        val resized = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        resized.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        val inputBuffer: ByteBuffer = when (inputDataType) {
            DataType.UINT8, DataType.INT8 -> {
                val buf = ByteBuffer.allocateDirect(INPUT_SIZE * INPUT_SIZE * 3)
                buf.order(ByteOrder.nativeOrder())
                for (pixel in pixels) {
                    buf.put(quantizeByte(((pixel shr 16) and 0xFF).toFloat())) // R
                    buf.put(quantizeByte(((pixel shr 8) and 0xFF).toFloat()))  // G
                    buf.put(quantizeByte((pixel and 0xFF).toFloat()))          // B
                }
                buf
            }
            else -> { // FLOAT32 fallback — raw 0..255 values, model's own Rescaling layer normalizes
                val buf = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
                buf.order(ByteOrder.nativeOrder())
                for (pixel in pixels) {
                    buf.putFloat(((pixel shr 16) and 0xFF).toFloat())
                    buf.putFloat(((pixel shr 8) and 0xFF).toFloat())
                    buf.putFloat((pixel and 0xFF).toFloat())
                }
                buf
            }
        }

        val rawOutput: FloatArray = when (outputDataType) {
            DataType.UINT8, DataType.INT8 -> {
                val out = Array(1) { ByteArray(numClasses) }
                interpreter.run(inputBuffer, out)
                FloatArray(numClasses) { i ->
                    val raw = if (outputDataType == DataType.UINT8) out[0][i].toInt() and 0xFF else out[0][i].toInt()
                    (raw - outputZeroPoint) * outputScale
                }
            }
            else -> {
                val out = Array(1) { FloatArray(numClasses) }
                interpreter.run(inputBuffer, out)
                out[0]
            }
        }

        val probabilities = if (OUTPUT_IS_LOGITS) softmax(rawOutput) else rawOutput

        return labels.zip(probabilities.toList()).sortedByDescending { it.second }
    }

    /** Quantizes a raw 0..255 pixel value using this model's own input tensor quantization params. */
    private fun quantizeByte(rawPixelValue: Float): Byte {
        val quantized = (rawPixelValue / inputScale + inputZeroPoint).roundToInt()
        val clamped = if (inputDataType == DataType.UINT8) quantized.coerceIn(0, 255) else quantized.coerceIn(-128, 127)
        return clamped.toByte()
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val max = logits.maxOrNull() ?: 0f
        val exps = logits.map { exp((it - max).toDouble()).toFloat() }
        val sum = exps.sum()
        return exps.map { it / sum }.toFloatArray()
    }

    fun close() {
        interpreter.close()
    }
}