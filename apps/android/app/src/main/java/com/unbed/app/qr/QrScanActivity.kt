package com.unbed.app.qr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.unbed.app.UnbedApplication
import com.unbed.app.alarm.AlarmNotifier
import com.unbed.app.alarm.QrSubmissionResult
import com.unbed.app.ringing.AlarmRingingActivity
import com.unbed.app.snooze.SnoozeWaitingActivity
import com.unbed.app.ui.theme.unbedTheme
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class QrScanActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sessionId = intent.getStringExtra(AlarmNotifier.EXTRA_SESSION_ID).orEmpty()
        val appContainer = (application as UnbedApplication).appContainer

        setContent {
            unbedTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    qrScanRoute(
                        sessionId = sessionId,
                        onScanMatched = {
                            appContainer.alarmPlaybackController.stop()
                            appContainer.alarmNotifier.dismissRingingNotification()
                            startActivity(
                                Intent(this, SnoozeWaitingActivity::class.java).apply {
                                    putExtra(AlarmNotifier.EXTRA_SESSION_ID, sessionId)
                                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                },
                            )
                            finish()
                        },
                        onBackToAlarm = {
                            startActivity(
                                Intent(this, AlarmRingingActivity::class.java).apply {
                                    putExtra(AlarmNotifier.EXTRA_SESSION_ID, sessionId)
                                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                },
                            )
                            finish()
                        },
                    )
                }
            }
        }
    }
}

@Composable
@Suppress("LongMethod")
private fun qrScanRoute(
    sessionId: String,
    onScanMatched: () -> Unit,
    onBackToAlarm: () -> Unit,
) {
    val context = LocalContext.current
    val application = context.applicationContext as UnbedApplication
    val appContainer = application.appContainer
    val hasPermission =
        remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED,
            )
        }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var scanLocked by remember { mutableStateOf(false) }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasPermission.value = granted
            if (!granted) {
                errorMessage = "Camera permission is required to scan the wake-up QR code."
            }
        }

    LaunchedEffect(sessionId) {
        appContainer.applicationScope.launch {
            appContainer.alarmCoordinator.beginQrScan(sessionId)
        }
    }

    val handleCancel = {
        appContainer.applicationScope.launch {
            appContainer.alarmCoordinator.cancelQrScan(sessionId)
        }
        onBackToAlarm()
    }
    val handleCodeDetected: (String) -> Unit = { rawValue ->
        if (!scanLocked) {
            scanLocked = true
            appContainer.applicationScope.launch {
                when (appContainer.alarmCoordinator.submitQrCode(sessionId, rawValue)) {
                    is QrSubmissionResult.Matched -> onScanMatched()
                    QrSubmissionResult.Mismatch -> {
                        errorMessage = "That QR code does not match the configured wake-up marker."
                        scanLocked = false
                    }
                    QrSubmissionResult.SessionUnavailable -> {
                        errorMessage = "The alarm session is no longer active."
                        scanLocked = false
                    }
                    is QrSubmissionResult.InvalidState -> {
                        errorMessage = "QR scanning is not available from the current alarm state."
                        scanLocked = false
                    }
                }
            }
        }
    }

    qrScanScreen(
        hasPermission = hasPermission.value,
        errorMessage = errorMessage,
        onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
        onCancel = handleCancel,
        cameraPreview = {
            if (hasPermission.value) {
                qrCameraPreview(onCodeDetected = handleCodeDetected)
            }
        },
    )
}

@Composable
private fun qrScanScreen(
    hasPermission: Boolean,
    errorMessage: String?,
    onRequestPermission: () -> Unit,
    onCancel: () -> Unit,
    cameraPreview: @Composable () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Scan wake-up QR",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Only the fixed MVP QR code will snooze the current alarm. Wrong codes keep the alarm active.",
            style = MaterialTheme.typography.bodyLarge,
        )
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (hasPermission) {
            cameraPreview()
        } else {
            Text(
                text =
                    "If you denied camera access earlier, return to initial setup after this " +
                        "alarm and re-enable it in system settings.",
                style = MaterialTheme.typography.bodySmall,
            )
            Button(onClick = onRequestPermission) {
                Text("Grant camera permission")
            }
        }
        Button(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Back to alarm")
        }
    }
}

@Composable
private fun qrCameraPreview(onCodeDetected: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val analyzer = remember { QrCameraAnalyzer(onCodeDetected) }

    AndroidView(
        factory = {
            previewView.apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        modifier =
            Modifier
                .fillMaxWidth()
                .height(360.dp),
    )

    DisposableEffect(lifecycleOwner, previewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val listener =
            Runnable {
                val cameraProvider = cameraProviderFuture.get()
                val preview =
                    Preview.Builder()
                        .build()
                        .also { it.surfaceProvider = previewView.surfaceProvider }
                val imageAnalysis =
                    ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { it.setAnalyzer(analysisExecutor, analyzer) }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis,
                )
            }
        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            ProcessCameraProvider.getInstance(context).get().unbindAll()
            analyzer.close()
            analysisExecutor.shutdown()
        }
    }
}
