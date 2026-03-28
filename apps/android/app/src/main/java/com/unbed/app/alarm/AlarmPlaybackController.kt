package com.unbed.app.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class AlarmPlaybackController(
    private val context: Context,
) {
    private var mediaPlayer: MediaPlayer? = null
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

    fun start() {
        if (mediaPlayer?.isPlaying == true) {
            return
        }

        val alarmUri =
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        mediaPlayer =
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                isLooping = true
                setDataSource(context, alarmUri)
                prepare()
                start()
            }

        vibrator?.vibrate(createWaveform())
    }

    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
    }

    private fun createWaveform(): VibrationEffect {
        return VibrationEffect.createWaveform(
            longArrayOf(VIBRATION_START_DELAY_MS, VIBRATION_ON_MS, VIBRATION_OFF_MS),
            0,
        )
    }

    companion object {
        private const val VIBRATION_START_DELAY_MS: Long = 0L
        private const val VIBRATION_ON_MS: Long = 500L
        private const val VIBRATION_OFF_MS: Long = 400L
    }
}
