package com.vidrierapp.domain.usecase

import com.vidrierapp.domain.model.PASS_DURATION_MILLIS
import com.vidrierapp.domain.repository.PassRepository
import kotlinx.coroutines.flow.first

class PurchasePassUseCase(
    private val repository: PassRepository,
    private val onPurchased: suspend () -> Unit = {},
    private val clock: () -> Long
) {
    suspend operator fun invoke() {
        val state = repository.passState.first()
        val start = maxOf(clock(), state.passExpiresAt ?: Long.MIN_VALUE)
        repository.savePassState(state.copy(passExpiresAt = start + PASS_DURATION_MILLIS))
        onPurchased()
    }
}
