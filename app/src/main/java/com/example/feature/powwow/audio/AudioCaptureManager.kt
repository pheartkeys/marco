package com.example.feature.powwow.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.example.data.model.PowWowLimits
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * Manages raw audio recording for Pow Wow sessions into a short-lived temporary cache file.
 *
 * Enforces the core privacy contract:
 * - Temporary audio file path is stored strictly in memory and never written to Room.
 * - Files are deleted immediately after transcription or upon discard/cancellation.
 */
class AudioCaptureManager(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var currentTempFile: File? = null
    private var tickerJob: Job? = null

    private val _durationSeconds = MutableStateFlow(0)
    val durationSeconds: StateFlow<Int> = _durationSeconds.asStateFlow()

    private val _amplitude = MutableStateFlow(0)
    val amplitude: StateFlow<Int> = _amplitude.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    fun startRecording(sessionId: Long, scope: CoroutineScope): File? {
        stopAndCleanUp()

        return try {
            val tempFile = File.createTempFile("powwow_session_${sessionId}_", ".m4a", context.cacheDir)
            currentTempFile = tempFile

            val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            newRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(tempFile.absolutePath)
                prepare()
                start()
            }

            recorder = newRecorder
            _isRecording.value = true
            _durationSeconds.value = 0

            // Start live ticker & amplitude sampler
            tickerJob = scope.launch(Dispatchers.Default) {
                while (isActive && _isRecording.value) {
                    delay(100)
                    _durationSeconds.value += 1
                    try {
                        val maxAmp = recorder?.maxAmplitude ?: 0
                        _amplitude.value = maxAmp
                    } catch (_: Exception) {}

                    // Auto-stop at max limit
                    if (_durationSeconds.value >= PowWowLimits.MAX_DURATION_SECONDS * 10) {
                        break
                    }
                }
            }

            tempFile
        } catch (e: Exception) {
            stopAndCleanUp()
            null
        }
    }

    fun stopRecording(): File? {
        tickerJob?.cancel()
        tickerJob = null
        _isRecording.value = false

        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {}
        recorder = null

        return currentTempFile
    }

    fun deleteAudioFile(file: File?) {
        try {
            if (file != null && file.exists()) {
                file.delete()
            }
            if (currentTempFile != null && currentTempFile == file) {
                currentTempFile = null
            }
        } catch (_: Exception) {}
    }

    fun stopAndCleanUp() {
        tickerJob?.cancel()
        tickerJob = null
        _isRecording.value = false
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {}
        recorder = null

        currentTempFile?.let { deleteAudioFile(it) }
        currentTempFile = null
        _durationSeconds.value = 0
        _amplitude.value = 0
    }
}
