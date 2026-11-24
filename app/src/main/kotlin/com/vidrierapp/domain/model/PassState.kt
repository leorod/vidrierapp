package com.vidrierapp.domain.model

const val TRIAL_DURATION_MILLIS = 3L * 24 * 60 * 60 * 1000
const val PASS_DURATION_MILLIS = 30L * 24 * 60 * 60 * 1000

data class PassState(
    val trialStartedAt: Long? = null,
    val passExpiresAt: Long? = null
) {
    val trialUsed: Boolean get() = trialStartedAt != null

    val trialExpiresAt: Long? get() = trialStartedAt?.plus(TRIAL_DURATION_MILLIS)

    fun isTrialActive(now: Long): Boolean = trialExpiresAt?.let { now < it } == true

    fun isPassActive(now: Long): Boolean = passExpiresAt?.let { now < it } == true

    fun hasActivePass(now: Long): Boolean = isTrialActive(now) || isPassActive(now)

    fun activeUntil(now: Long): Long? = listOfNotNull(trialExpiresAt, passExpiresAt)
        .filter { now < it }
        .maxOrNull()
}

data class PassAccess(
    val hasActivePass: Boolean,
    val trialAvailable: Boolean
)
