package com.vidrierapp.domain.usecase

import com.vidrierapp.domain.model.PassState
import com.vidrierapp.domain.repository.PassRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakePassRepository(initial: PassState = PassState()) : PassRepository {
    val state = MutableStateFlow(initial)

    override val passState: Flow<PassState> = state

    override suspend fun savePassState(state: PassState) {
        this.state.value = state
    }

    override suspend fun installationId(): String = "fake-id"
}
