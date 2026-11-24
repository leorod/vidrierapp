package com.vidrierapp.domain.usecase

import com.vidrierapp.domain.model.PassAccess
import com.vidrierapp.domain.repository.PassRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.transformLatest

@OptIn(ExperimentalCoroutinesApi::class)
class ObservePassAccessUseCase(
    private val repository: PassRepository,
    private val clock: () -> Long
) {
    operator fun invoke(): Flow<PassAccess> = repository.passState
        .transformLatest { state ->
            while (true) {
                val now = clock()
                val until = state.activeUntil(now)
                emit(PassAccess(hasActivePass = until != null, trialAvailable = !state.trialUsed))
                if (until == null) break
                delay(until - now)
            }
        }
        .distinctUntilChanged()
}
