package com.example.feature.powwow

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.di.MarcoRepositories
import com.example.data.model.PowWowSessionEntity
import com.example.data.model.PowWowSessionState
import com.example.data.model.TravelerAgeBand
import com.example.data.model.TripBriefEntity
import com.example.data.repository.ConsentStatus
import com.example.feature.powwow.audio.AudioCaptureManager
import com.example.feature.powwow.synthesis.BriefSynthesizer
import com.example.feature.powwow.transcription.PowWowTranscriber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

/**
 * Feature ViewModel for Pow Wow Audio Capture, Consent Gate, Transcription, and Synthesis.
 * Obtains its repositories cleanly from [MarcoRepositories] without touching [TravelViewModel].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PowWowViewModel(application: Application) : AndroidViewModel(application) {

    private val powWowRepository = MarcoRepositories.powWow(application)
    private val partyRepository = MarcoRepositories.party(application)

    private val captureManager = AudioCaptureManager(application)
    private val transcriber = PowWowTranscriber(powWowRepository)

    private val _currentTripId = MutableStateFlow(0L)
    val currentTripId: StateFlow<Long> = _currentTripId.asStateFlow()

    private val _currentIdeaId = MutableStateFlow(0L)
    val currentIdeaId: StateFlow<Long> = _currentIdeaId.asStateFlow()

    private var activeRecordingFile: File? = null

    val isRecording: StateFlow<Boolean> = captureManager.isRecording
    val recordingDuration: StateFlow<Int> = captureManager.durationSeconds
    val recordingAmplitude: StateFlow<Int> = captureManager.amplitude

    private val _currentPromptIndex = MutableStateFlow(0)
    val currentPromptIndex: StateFlow<Int> = _currentPromptIndex.asStateFlow()

    fun selectTrip(tripId: Long) {
        _currentTripId.value = tripId
    }

    fun selectIdea(ideaId: Long) {
        _currentIdeaId.value = ideaId
    }

    val sessions: StateFlow<List<PowWowSessionEntity>> = _currentTripId.flatMapLatest { tripId ->
        if (tripId == 0L) {
            _currentIdeaId.flatMapLatest { ideaId ->
                if (ideaId == 0L) flowOf(emptyList()) else powWowRepository.observeSessionsForIdea(ideaId)
            }
        } else {
            powWowRepository.observeSessionsForTrip(tripId)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestBrief: StateFlow<TripBriefEntity?> = _currentTripId.flatMapLatest { tripId ->
        if (tripId == 0L) {
            flowOf(null)
        } else {
            powWowRepository.observeLatestBrief(tripId)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun createSession(travelerId: Long, ageBand: TravelerAgeBand): Long {
        val tripId = _currentTripId.value
        val ideaId = _currentIdeaId.value
        var createdId = 0L

        viewModelScope.launch(Dispatchers.IO) {
            createdId = powWowRepository.createSession(
                PowWowSessionEntity(
                    tripId = tripId,
                    ideaId = ideaId,
                    travelerId = travelerId,
                    state = PowWowSessionState.PENDING_CONSENT.value
                )
            )
        }
        return createdId
    }

    fun grantConsent(sessionId: Long, guardianTravelerId: Long = 0L) {
        viewModelScope.launch(Dispatchers.IO) {
            powWowRepository.recordConsent(sessionId, 0L)
            if (guardianTravelerId != 0L) {
                powWowRepository.recordGuardianConsent(sessionId, guardianTravelerId)
            }
        }
    }

    fun startRecording(sessionId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val consent = powWowRepository.consentStatus(sessionId)
            if (consent !is ConsentStatus.Granted) {
                return@launch
            }
            powWowRepository.markRecordingStarted(sessionId)
            activeRecordingFile = captureManager.startRecording(sessionId, viewModelScope)
            _currentPromptIndex.value = 0
        }
    }

    fun advancePrompt() {
        if (_currentPromptIndex.value < 5) {
            _currentPromptIndex.value += 1
        }
    }

    fun stopAndTranscribe(sessionId: Long, promptNotes: String = "") {
        val file = captureManager.stopRecording() ?: activeRecordingFile
        if (file == null || !file.exists()) {
            viewModelScope.launch(Dispatchers.IO) {
                powWowRepository.markTranscriptionFailed(sessionId, "Audio file missing")
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val durationSec = recordingDuration.value / 10
            powWowRepository.markRecordingComplete(sessionId, durationSec)
            transcriber.transcribeAndPurgeAudio(sessionId, file, promptNotes)
            activeRecordingFile = null
        }
    }

    fun discardSession(sessionId: Long) {
        captureManager.stopAndCleanUp()
        activeRecordingFile = null
        viewModelScope.launch(Dispatchers.IO) {
            powWowRepository.discardSession(sessionId)
        }
    }

    fun synthesizeBrief() {
        val tripId = _currentTripId.value
        val ideaId = _currentIdeaId.value

        viewModelScope.launch(Dispatchers.IO) {
            val sessionsList = sessions.value

            val transcripts = sessionsList.mapNotNull {
                powWowRepository.getLatestTranscript(it.id)
            }

            val travelers = if (tripId != 0L) {
                val memberships = partyRepository.getMemberships(tripId)
                memberships.mapNotNull { partyRepository.getTraveler(it.travelerId) }
            } else {
                emptyList()
            }

            val brief = BriefSynthesizer.synthesizeBrief(
                tripId = tripId,
                ideaId = ideaId,
                version = (latestBrief.value?.version ?: 0) + 1,
                transcripts = transcripts,
                travelers = travelers
            )

            powWowRepository.insertBrief(brief)
        }
    }

    fun acceptBrief(briefId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            powWowRepository.markBriefAccepted(briefId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        captureManager.stopAndCleanUp()
    }
}
