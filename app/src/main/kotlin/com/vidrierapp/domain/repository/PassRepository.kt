package com.vidrierapp.domain.repository

import com.vidrierapp.domain.model.PassState
import kotlinx.coroutines.flow.Flow

interface PassRepository {
    val passState: Flow<PassState>

    suspend fun savePassState(state: PassState)

    suspend fun installationId(): String
}
