package com.unbed.app.setup

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.unbed.app.UnbedApplication

@Composable
fun onboardingRoute(onCompleted: () -> Unit) {
    val application = LocalContext.current.applicationContext as UnbedApplication
    val onboardingViewModel: OnboardingViewModel =
        viewModel(
            factory =
                OnboardingViewModel.factory(
                    onboardingStore = application.appContainer.onboardingStore,
                    deviceSetupManager = application.appContainer.deviceSetupManager,
                ),
        )
    val uiState by onboardingViewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            onboardingViewModel.refreshStatuses()
        }
    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            onboardingViewModel.refreshStatuses()
        }

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    onboardingViewModel.refreshStatuses()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val onAction =
        remember(
            application,
            notificationPermissionLauncher,
            cameraPermissionLauncher,
        ) {
            {
                    action: SetupAction ->
                handleSetupAction(
                    action = action,
                    application = application,
                    notificationPermissionLauncher = notificationPermissionLauncher,
                    cameraPermissionLauncher = cameraPermissionLauncher,
                )
            }
        }

    onboardingScreen(
        uiState = uiState,
        onAction = onAction,
        onRefresh = onboardingViewModel::refreshStatuses,
        onConfirmQrPlacement = onboardingViewModel::setQrPlacementConfirmed,
        onComplete = {
            onboardingViewModel.markCompleted()
            onCompleted()
        },
    )
}

private fun handleSetupAction(
    action: SetupAction,
    application: UnbedApplication,
    notificationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    cameraPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
) {
    when (action) {
        SetupAction.Notifications -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        SetupAction.Camera -> {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        SetupAction.ExactAlarms -> {
            application.appContainer.deviceSetupManager.openExactAlarmSettings()
        }

        SetupAction.BatteryOptimization -> {
            application.appContainer.deviceSetupManager.openBatteryOptimizationSettings()
        }
    }
}

@Composable
internal fun onboardingScreen(
    uiState: OnboardingUiState,
    onAction: (SetupAction) -> Unit,
    onRefresh: () -> Unit,
    onConfirmQrPlacement: (Boolean) -> Unit,
    onComplete: () -> Unit,
) {
    val qrBitmap = remember(uiState.qrValue) { QrBitmapFactory.create(uiState.qrValue, QR_SIZE_PX) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = "Initial setup",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Finish permissions, system settings, and QR preparation before using alarms.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text =
                    "MVP limits: one shared QR, camera required, and no fallback dismissal " +
                        "if scanning is unavailable.",
                style = MaterialTheme.typography.bodyMedium,
            )

            setupRequirementsSection(
                uiState = uiState,
                onAction = onAction,
            )
            qrPreparationSection(
                qrBitmap = qrBitmap.asImageBitmap(),
                qrValue = uiState.qrValue,
                hasPlacedQrAwayFromBed = uiState.hasPlacedQrAwayFromBed,
                onConfirmQrPlacement = onConfirmQrPlacement,
            )

            OutlinedButton(onClick = onRefresh) {
                Text("Refresh setup status")
            }
            Button(
                onClick = onComplete,
                enabled = uiState.canComplete,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Finish setup")
            }
        }
    }
}

@Composable
private fun setupRequirementsSection(
    uiState: OnboardingUiState,
    onAction: (SetupAction) -> Unit,
) {
    setupCard(
        title = "Notifications",
        body = "Required for high-priority alarm alerts on top of the lock screen.",
        isReady = uiState.notificationsGranted,
        actionLabel = "Grant notifications",
        onAction = { onAction(SetupAction.Notifications) },
    )
    setupCard(
        title = "Camera",
        body = "Required to scan the wake-up QR code before the alarm can snooze.",
        isReady = uiState.cameraGranted,
        actionLabel = "Grant camera",
        onAction = { onAction(SetupAction.Camera) },
    )
    setupCard(
        title = "Exact alarms",
        body = "Allow Android to fire alarms at the configured minute instead of batching them.",
        isReady = uiState.exactAlarmGranted,
        actionLabel = "Open exact alarm settings",
        onAction = { onAction(SetupAction.ExactAlarms) },
    )
    setupCard(
        title = "Battery optimization",
        body = "Exclude Unbed from battery optimization so re-rings survive aggressive vendor policies.",
        isReady = uiState.batteryOptimizationIgnored,
        actionLabel = "Open battery settings",
        onAction = { onAction(SetupAction.BatteryOptimization) },
    )
}

@Composable
private fun qrPreparationSection(
    qrBitmap: androidx.compose.ui.graphics.ImageBitmap,
    qrValue: String,
    hasPlacedQrAwayFromBed: Boolean,
    onConfirmQrPlacement: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Fixed MVP QR",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "This QR is shared across all users in the MVP. Put it somewhere away from the bed.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "Print it or leave it on a second device. Do not keep it within arm's reach of the bed.",
            style = MaterialTheme.typography.bodySmall,
        )
        Image(
            bitmap = qrBitmap,
            contentDescription = "Fixed wake-up QR",
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(320.dp),
        )
        Text(
            text = qrValue,
            style = MaterialTheme.typography.bodySmall,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Checkbox(
                checked = hasPlacedQrAwayFromBed,
                onCheckedChange = onConfirmQrPlacement,
            )
            Text("I placed this QR away from the bed and can scan it when standing up.")
        }
    }
}

@Composable
private fun setupCard(
    title: String,
    body: String,
    isReady: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = if (isReady) "Ready" else "Action required",
            style = MaterialTheme.typography.bodySmall,
            color =
                if (isReady) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
        )
        if (!isReady) {
            OutlinedButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

private const val QR_SIZE_PX: Int = 768
