package com.vidrierapp.domain.usecase

import com.vidrierapp.domain.repository.PassRepository
import kotlinx.coroutines.flow.first

class StartTrialUseCase(
    private val repository: PassRepository,
    private val clock: () -> Long
) {
    suspend operator fun invoke(): Boolean {
        val state = repository.passState.first()
        if (state.trialUsed) return false
        repository.savePassState(state.copy(trialStartedAt = clock()))
        return true
    }
}
