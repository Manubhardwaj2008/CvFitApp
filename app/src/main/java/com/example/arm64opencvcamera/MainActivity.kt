package com.example.arm64opencvcamera

import android.Manifest
import com.example.arm64opencvcamera.R
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfInt
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    // View References
    private lateinit var openCvCameraView: CameraBridgeViewBase
    private lateinit var imageView: ImageView
    private lateinit var buttonBack: ImageView
    private lateinit var btnToggleDetect: LinearLayout
    private lateinit var ivDetectIcon: ImageView
    private lateinit var buttonPlayPause: ImageView
    private lateinit var badgeScanning: LinearLayout
    private lateinit var scanningLabel: TextView
    private lateinit var dotScanning: View
    private lateinit var viewfinderContainer: FrameLayout
    private lateinit var scanLaserLine: View
    private lateinit var cardFocusArea: LinearLayout
    private lateinit var layoutDisclaimer: LinearLayout
    private lateinit var buttonGallery: LinearLayout
    private lateinit var buttonShutter: FrameLayout
    private lateinit var buttonTips: LinearLayout
    private lateinit var textViewStatus: TextView

    // State Variables
    private var isDetectActive = true
    private var isPreviewActive = false
    private var isOpenCvInitialized = false
    private val cameraPermissionRequestCode = 100

    // Animators
    private var laserSweepAnimator: ValueAnimator? = null
    private var badgePulseAnimator: ObjectAnimator? = null
    private val statusHandler = Handler(Looper.getMainLooper())
    private var statusDismissRunnable: Runnable? = null

    // OpenCV & Classifier State
    private lateinit var inputMat: Mat
    private var displayBitmap: Bitmap? = null
    private var segmentedMat: Mat? = null
    private var skinClassifier: SkinClassifier? = null
    private var lastSkinRegion: org.opencv.core.Rect? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
        initOpenCvAndClassifier()
        checkCameraPermission()
    }

    private fun initViews() {
        openCvCameraView = findViewById(R.id.cameraView)
        imageView = findViewById(R.id.imageView)
        buttonBack = findViewById(R.id.buttonBack)
        btnToggleDetect = findViewById(R.id.btnToggleDetect)
        ivDetectIcon = findViewById(R.id.ivDetectIcon)
        buttonPlayPause = findViewById(R.id.buttonPlayPause)
        badgeScanning = findViewById(R.id.badgeScanning)
        scanningLabel = findViewById(R.id.scanningLabel)
        dotScanning = findViewById(R.id.dotScanning)
        viewfinderContainer = findViewById(R.id.viewfinderContainer)
        scanLaserLine = findViewById(R.id.scanLaserLine)
        cardFocusArea = findViewById(R.id.cardFocusArea)
        layoutDisclaimer = findViewById(R.id.layoutDisclaimer)
        buttonGallery = findViewById(R.id.buttonGallery)
        buttonShutter = findViewById(R.id.buttonShutter)
        buttonTips = findViewById(R.id.buttonTips)
        textViewStatus = findViewById(R.id.textViewStatus)
    }

    private fun setupListeners() {
        buttonBack.setOnClickListener {
            finish()
        }

        btnToggleDetect.setOnClickListener {
            isDetectActive = !isDetectActive
            updateDetectModeUI()
        }

        buttonPlayPause.setOnClickListener {
            toggleCameraPreview()
        }

        buttonShutter.setOnClickListener {
            animateShutterClick()
            captureCurrentFrame()
        }

        buttonGallery.setOnClickListener {
            openGallery()
        }

        buttonTips.setOnClickListener {
            showScanningTipsDialog()
        }
    }

    private fun initOpenCvAndClassifier() {
        isOpenCvInitialized = OpenCVLoader.initLocal()

        skinClassifier = try {
            SkinClassifier(this)
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Skin classifier model not loaded: ${e.message}")
            null
        }

        openCvCameraView.setCameraIndex(0)
        openCvCameraView.setCvCameraViewListener(this)
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                cameraPermissionRequestCode
            )
        } else {
            startCameraPreview()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == cameraPermissionRequestCode &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startCameraPreview()
        } else {
            showStatusMessage("Camera permission is required for scanning", 4000)
        }
    }

    private fun startCameraPreview() {
        if (isOpenCvInitialized) {
            openCvCameraView.setCameraPermissionGranted()
            openCvCameraView.enableView()
            isPreviewActive = true
            buttonPlayPause.setImageResource(R.drawable.ic_pause)
            updateDetectModeUI()
        } else {
            showStatusMessage("OpenCV initialization failed", 3000)
        }
    }

    private fun toggleCameraPreview() {
        if (!isOpenCvInitialized) {
            showStatusMessage("OpenCV not initialized", 2000)
            return
        }

        if (isPreviewActive) {
            openCvCameraView.disableView()
            isPreviewActive = false
            buttonPlayPause.setImageResource(R.drawable.ic_play)
            stopLaserAndBadgeAnimations()
            showStatusMessage("Camera preview paused", 1800)
        } else {
            openCvCameraView.setCameraPermissionGranted()
            openCvCameraView.enableView()
            isPreviewActive = true
            buttonPlayPause.setImageResource(R.drawable.ic_pause)
            updateDetectModeUI()
            showStatusMessage("Camera preview resumed", 1800)
        }
    }

    private fun updateDetectModeUI() {
        if (isDetectActive) {
            btnToggleDetect.setBackgroundResource(R.drawable.bg_pill_detect_active)
            badgeScanning.visibility = View.VISIBLE
            scanLaserLine.visibility = View.VISIBLE
            cardFocusArea.visibility = View.VISIBLE
            if (isPreviewActive) {
                startLaserAndBadgeAnimations()
            }
        } else {
            btnToggleDetect.setBackgroundResource(R.drawable.bg_pill_dark)
            badgeScanning.visibility = View.GONE
            scanLaserLine.visibility = View.GONE
            stopLaserAndBadgeAnimations()
        }
    }

    private fun startLaserAndBadgeAnimations() {
        viewfinderContainer.post {
            val travel = (viewfinderContainer.height - scanLaserLine.height).coerceAtLeast(0).toFloat()

            laserSweepAnimator?.cancel()
            laserSweepAnimator = ValueAnimator.ofFloat(0f, travel).apply {
                duration = 2000
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                addUpdateListener { anim ->
                    scanLaserLine.translationY = anim.animatedValue as Float
                }
                start()
            }
        }

        badgePulseAnimator?.cancel()
        badgePulseAnimator = ObjectAnimator.ofFloat(badgeScanning, "alpha", 1f, 0.45f).apply {
            duration = 850
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            start()
        }
    }

    private fun stopLaserAndBadgeAnimations() {
        laserSweepAnimator?.cancel()
        badgePulseAnimator?.cancel()
        badgeScanning.alpha = 1f
    }

    private fun animateShutterClick() {
        buttonShutter.animate()
            .scaleX(0.88f)
            .scaleY(0.88f)
            .setDuration(80)
            .withEndAction {
                buttonShutter.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(120)
                    .start()
            }
            .start()
    }

    private fun captureCurrentFrame() {
        val source = displayBitmap
        if (source == null) {
            showStatusMessage("Camera frame not ready yet", 2000)
            return
        }

        val bitmapCopy = source.copy(source.config ?: Bitmap.Config.ARGB_8888, false)
        flashShutterEffect()

        val filename = "CV_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ARM64OpenCVCamera")
            }
        }

        try {
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                contentResolver.openOutputStream(uri)?.use { out ->
                    bitmapCopy.compress(Bitmap.CompressFormat.JPEG, 92, out)
                }
                classifyCapturedRegion(filename)
            } else {
                showStatusMessage("Capture failed to save", 2500)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to save capture", e)
            showStatusMessage("Capture error: ${e.localizedMessage}", 2500)
        }
    }

    private fun classifyCapturedRegion(savedFilename: String) {
        val classifier = skinClassifier
        if (classifier == null) {
            showStatusMessage("Saved $savedFilename to Pictures", 3500)
            return
        }

        val region = lastSkinRegion
        val cropBitmap: Bitmap = try {
            val cropMat = if (region != null) Mat(inputMat, region) else inputMat
            val bmp = Bitmap.createBitmap(cropMat.cols(), cropMat.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(cropMat, bmp)
            if (region != null) cropMat.release()
            bmp
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Crop for classification failed", e)
            showStatusMessage("Saved $savedFilename (classification error)", 3500)
            return
        }

        val results = try {
            classifier.classify(cropBitmap)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Classification failed", e)
            showStatusMessage("Saved $savedFilename (classification error)", 3500)
            return
        }

        val top = results.firstOrNull()
        val confidenceThreshold = 0.5f

        val message = if (top == null || top.second < confidenceThreshold) {
            "Saved $savedFilename — Inconclusive screening. Consult a specialist."
        } else {
            val pct = (top.second * 100).toInt()
            "Saved $savedFilename — Possible match: ${top.first} ($pct%). Consult a dermatologist."
        }

        showStatusMessage(message, 4500)
    }

    private fun flashShutterEffect() {
        imageView.animate().cancel()
        imageView.alpha = 1f
        imageView.animate()
            .alpha(0.15f)
            .setDuration(70)
            .withEndAction {
                imageView.animate().alpha(1f).setDuration(150).start()
            }
            .start()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            type = "image/*"
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "No gallery app found", e)
            showStatusMessage("No gallery app found", 2000)
        }
    }

    private fun showStatusMessage(message: String, durationMs: Long = 3000) {
        statusDismissRunnable?.let { statusHandler.removeCallbacks(it) }

        textViewStatus.text = message
        textViewStatus.alpha = 0f
        textViewStatus.visibility = View.VISIBLE
        textViewStatus.animate()
            .alpha(1f)
            .setDuration(200)
            .start()

        statusDismissRunnable = Runnable {
            textViewStatus.animate()
                .alpha(0f)
                .setDuration(250)
                .withEndAction {
                    textViewStatus.visibility = View.GONE
                }
                .start()
        }
        statusHandler.postDelayed(statusDismissRunnable!!, durationMs)
    }

    private fun showScanningTipsDialog() {
        val message = """
            1. Optimal Lighting:
            Ensure bright, uniform ambient lighting. Avoid harsh shadows and strong reflections.
            
            2. Steady Hand:
            Hold the device steady for 1-2 seconds within the reticle frame.
            
            3. Clean Lens:
            Wipe your camera lens gently for maximum clarity.
            
            4. Frame Alignment:
            Keep the area of interest centered inside the cyan corner brackets.
            
            Visual analysis is for screening reference only and does not replace medical diagnosis.
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("💡 Skin Scan Tips")
            .setMessage(message)
            .setPositiveButton("Got it", null)
            .show()
    }

    // CameraBridgeViewBase Callbacks
    override fun onCameraViewStarted(width: Int, height: Int) {
        isPreviewActive = true
        inputMat = Mat(height, width, CvType.CV_8UC4)
        runOnUiThread {
            buttonPlayPause.setImageResource(R.drawable.ic_pause)
            updateDetectModeUI()
        }
    }

    override fun onCameraViewStopped() {
        isPreviewActive = false
        if (::inputMat.isInitialized) {
            inputMat.release()
        }
        segmentedMat?.release()
        segmentedMat = null
        runOnUiThread {
            buttonPlayPause.setImageResource(R.drawable.ic_play)
            stopLaserAndBadgeAnimations()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        skinClassifier?.close()
        stopLaserAndBadgeAnimations()
        statusDismissRunnable?.let { statusHandler.removeCallbacks(it) }
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        if (inputFrame == null) return inputMat

        inputFrame.rgba().copyTo(inputMat)

        val outputMat: Mat = if (isDetectActive) {
            try {
                val result = segmentFrame(inputMat)
                segmentedMat?.release()
                segmentedMat = result
                result
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "segmentFrame failed, showing raw frame", e)
                inputMat
            }
        } else {
            inputMat
        }

        if (displayBitmap == null ||
            displayBitmap!!.width != outputMat.cols() ||
            displayBitmap!!.height != outputMat.rows()
        ) {
            displayBitmap = Bitmap.createBitmap(outputMat.cols(), outputMat.rows(), Bitmap.Config.ARGB_8888)
        }
        Utils.matToBitmap(outputMat, displayBitmap)

        val bitmapToShow = displayBitmap
        runOnUiThread {
            imageView.setImageBitmap(bitmapToShow)
        }

        return outputMat
    }

    /**
     * Skin segmentation, three layers deep:
     *
     * 1) COLOR — majority vote (2-of-3) across three independent color-space
     *    rules: Peer et al. RGB rule, classic YCrCb range, and an HSV hue
     *    band. Majority vote (not strict AND-of-all-three) avoids the
     *    over-tightening failure mode where requiring every rule to agree
     *    on every pixel kills recall under normal lighting.
     *
     * 2) TEXTURE — real skin has natural micro-texture (pores, subtle tonal
     *    variation) under any real camera sensor. Flat printed/plastic/
     *    painted surfaces that happen to be skin-colored are usually far
     *    more visually uniform. Blobs with very low internal intensity
     *    variance are rejected as likely non-skin.
     *
     * 3) SHAPE — man-made skin-colored objects (boxes, cards, panels,
     *    phone cases) tend to have simple, highly convex, low-vertex-count
     *    silhouettes. Real skin regions (arms, hands, faces) are visually
     *    organic/irregular. Blobs that approximate a simple convex polygon
     *    are rejected.
     *
     * IMPORTANT — honest limitation: no combination of color/texture/shape
     * heuristics can PERFECTLY separate real skin from every skin-colored
     * surface in the world. This tightens false positives substantially,
     * but it is still a classical CV heuristic, not a learned model — that
     * ceiling is exactly why the trained TFLite classifier exists as the
     * actual symptom-screening stage, run on what this function finds.
     */
    private fun segmentFrame(rgba: Mat): Mat {
        val scale = 0.5
        val small = Mat()
        Imgproc.resize(rgba, small, Size(), scale, scale, Imgproc.INTER_LINEAR)

        val bgr = Mat()
        Imgproc.cvtColor(small, bgr, Imgproc.COLOR_RGBA2BGR)

        val blurred = Mat()
        Imgproc.GaussianBlur(bgr, blurred, Size(5.0, 5.0), 0.0)

        // --- Rule 1: Peer et al. RGB rule ---
        val channels = ArrayList<Mat>()
        Core.split(blurred, channels)
        val bC = channels[0]; val gC = channels[1]; val rC = channels[2]

        val maxRG = Mat(); Core.max(rC, gC, maxRG)
        val maxRGB = Mat(); Core.max(maxRG, bC, maxRGB)
        val minRG = Mat(); Core.min(rC, gC, minRG)
        val minRGB = Mat(); Core.min(minRG, bC, minRGB)
        val spread = Mat(); Core.subtract(maxRGB, minRGB, spread)
        val diffRG = Mat(); Core.absdiff(rC, gC, diffRG)

        val condR = Mat(); Core.compare(rC, Scalar(95.0), condR, Core.CMP_GT)
        val condG = Mat(); Core.compare(gC, Scalar(40.0), condG, Core.CMP_GT)
        val condB = Mat(); Core.compare(bC, Scalar(20.0), condB, Core.CMP_GT)
        val condSpread = Mat(); Core.compare(spread, Scalar(15.0), condSpread, Core.CMP_GT)
        val condDiff = Mat(); Core.compare(diffRG, Scalar(15.0), condDiff, Core.CMP_GT)
        val condRG = Mat(); Core.compare(rC, gC, condRG, Core.CMP_GT)
        val condRB = Mat(); Core.compare(rC, bC, condRB, Core.CMP_GT)

        val rgbSkinMask = Mat()
        Core.bitwise_and(condR, condG, rgbSkinMask)
        Core.bitwise_and(rgbSkinMask, condB, rgbSkinMask)
        Core.bitwise_and(rgbSkinMask, condSpread, rgbSkinMask)
        Core.bitwise_and(rgbSkinMask, condDiff, rgbSkinMask)
        Core.bitwise_and(rgbSkinMask, condRG, rgbSkinMask)
        Core.bitwise_and(rgbSkinMask, condRB, rgbSkinMask)

        for (c in channels) c.release()
        maxRG.release(); maxRGB.release(); minRG.release(); minRGB.release()
        spread.release(); diffRG.release()
        condR.release(); condG.release(); condB.release(); condSpread.release()
        condDiff.release(); condRG.release(); condRB.release()

        // --- Rule 2: classic YCrCb range ---
        val ycrcb = Mat()
        Imgproc.cvtColor(blurred, ycrcb, Imgproc.COLOR_BGR2YCrCb)
        val ycrcbMask = Mat()
        Core.inRange(ycrcb, Scalar(0.0, 133.0, 77.0), Scalar(255.0, 173.0, 127.0), ycrcbMask)
        ycrcb.release()

        // --- Rule 3: HSV hue band ---
        val hsv = Mat()
        Imgproc.cvtColor(blurred, hsv, Imgproc.COLOR_BGR2HSV)
        val hsvLow = Mat(); val hsvHigh = Mat(); val hsvMask = Mat()
        Core.inRange(hsv, Scalar(0.0, 25.0, 50.0), Scalar(30.0, 190.0, 255.0), hsvLow)
        Core.inRange(hsv, Scalar(160.0, 25.0, 50.0), Scalar(180.0, 190.0, 255.0), hsvHigh)
        Core.bitwise_or(hsvLow, hsvHigh, hsvMask)
        hsvLow.release(); hsvHigh.release(); hsv.release()
        blurred.release()

        // --- Majority vote: pixel counts as skin only if >= 2 of 3 rules agree ---
        val v1 = Mat(); Core.divide(rgbSkinMask, Scalar(255.0), v1)
        val v2 = Mat(); Core.divide(ycrcbMask, Scalar(255.0), v2)
        val v3 = Mat(); Core.divide(hsvMask, Scalar(255.0), v3)
        rgbSkinMask.release(); ycrcbMask.release(); hsvMask.release()

        val voteSum = Mat()
        Core.add(v1, v2, voteSum)
        Core.add(voteSum, v3, voteSum)
        v1.release(); v2.release(); v3.release()

        val skinMask = Mat()
        Core.compare(voteSum, Scalar(2.0), skinMask, Core.CMP_GE)
        voteSum.release()

        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(5.0, 5.0))
        Imgproc.morphologyEx(skinMask, skinMask, Imgproc.MORPH_OPEN, kernel, Point(-1.0, -1.0), 2)
        Imgproc.morphologyEx(skinMask, skinMask, Imgproc.MORPH_CLOSE, kernel, Point(-1.0, -1.0), 2)
        kernel.release()

        val labels = Mat()
        val numLabels = Imgproc.connectedComponents(skinMask, labels)
        skinMask.release()

        val gray = Mat()
        Imgproc.cvtColor(bgr, gray, Imgproc.COLOR_BGR2GRAY)

        val output = bgr.clone()
        val minBlobArea = 500
        val maxRegions = 8
        val minTextureVariance = 3.0 // below this, treat as "too flat to be real skin"
        val maxVertexCountForRejection = 6 // simple polygons with this many corners or fewer...
        val minSolidityForRejection = 0.92 // ...and this convex, look man-made, not organic
        val accent = Scalar(40.0, 170.0, 255.0)

        var regionsDrawn = 0
        var bestArea = 0
        var bestRectSmall: org.opencv.core.Rect? = null

        for (label in 1 until numLabels) {
            if (regionsDrawn >= maxRegions) break

            val mask = Mat()
            Core.compare(labels, Scalar(label.toDouble()), mask, Core.CMP_EQ)
            val area = Core.countNonZero(mask)
            if (area < minBlobArea) {
                mask.release()
                continue
            }

            // --- Texture check: reject overly flat/uniform regions ---
            val meanS = org.opencv.core.MatOfDouble()
            val stdS = org.opencv.core.MatOfDouble()
            Core.meanStdDev(gray, meanS, stdS, mask)
            val roughness = stdS.toArray().getOrElse(0) { 0.0 }
            meanS.release(); stdS.release()
            if (roughness < minTextureVariance) {
                mask.release()
                continue
            }

            val contours = ArrayList<MatOfPoint>()
            val hierarchy = Mat()
            Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
            hierarchy.release()

            val largest = contours.maxByOrNull { Imgproc.contourArea(it) }
            if (largest == null) {
                for (c in contours) c.release()
                mask.release()
                continue
            }

            // --- Shape check: reject clean, low-vertex, highly-convex polygons ---
            val contour2f = MatOfPoint2f(*largest.toArray())
            val perimeter = Imgproc.arcLength(contour2f, true)
            val approx2f = MatOfPoint2f()
            Imgproc.approxPolyDP(contour2f, approx2f, 0.02 * perimeter, true)
            val vertexCount = approx2f.toArray().size
            contour2f.release(); approx2f.release()

            val hullIndices = MatOfInt()
            Imgproc.convexHull(largest, hullIndices)
            val contourPoints = largest.toArray()
            val hullPoints = hullIndices.toArray().map { idx -> contourPoints[idx] }
            hullIndices.release()
            var isGeometric = false
            if (hullPoints.size >= 3) {
                val hullMat = MatOfPoint(*hullPoints.toTypedArray())
                val hullArea = Imgproc.contourArea(hullMat)
                hullMat.release()
                val contourAreaVal = Imgproc.contourArea(largest)
                val solidity = if (hullArea > 0) contourAreaVal / hullArea else 0.0
                if (vertexCount in 3..maxVertexCountForRejection && solidity > minSolidityForRejection) {
                    isGeometric = true
                }
            }
            if (isGeometric) {
                for (c in contours) c.release()
                mask.release()
                continue
            }

            Imgproc.drawContours(output, contours, -1, accent, 2)

            val rect = Imgproc.boundingRect(largest)
            if (area > bestArea) {
                bestArea = area
                bestRectSmall = rect
            }
            Imgproc.rectangle(
                output,
                Point(rect.x.toDouble(), rect.y.toDouble()),
                Point((rect.x + rect.width).toDouble(), (rect.y + rect.height).toDouble()),
                accent,
                2
            )
            val text = "Skin  area:${area}px  var:${"%.1f".format(roughness)}"
            val labelOrigin = Point(rect.x.toDouble(), (rect.y - 6).coerceAtLeast(12).toDouble())
            Imgproc.putText(output, text, labelOrigin, Imgproc.FONT_HERSHEY_SIMPLEX, 0.42, accent, 1)

            for (c in contours) c.release()
            mask.release()
            regionsDrawn++
        }
        gray.release()
        labels.release()

        lastSkinRegion = bestRectSmall?.let { r ->
            val inv = 1.0 / scale
            org.opencv.core.Rect(
                (r.x * inv).toInt().coerceAtLeast(0),
                (r.y * inv).toInt().coerceAtLeast(0),
                (r.width * inv).toInt().coerceAtMost(rgba.cols() - (r.x * inv).toInt()),
                (r.height * inv).toInt().coerceAtMost(rgba.rows() - (r.y * inv).toInt())
            )
        }

        val outputRgba = Mat()
        Imgproc.cvtColor(output, outputRgba, Imgproc.COLOR_BGR2RGBA)
        val finalOutput = Mat()
        Imgproc.resize(outputRgba, finalOutput, rgba.size(), 0.0, 0.0, Imgproc.INTER_NEAREST)

        small.release(); bgr.release(); output.release(); outputRgba.release()
        return finalOutput
    }
}