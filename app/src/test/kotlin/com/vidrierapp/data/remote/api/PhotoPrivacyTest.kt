package com.vidrierapp.data.remote.api

import com.vidrierapp.data.remote.dto.CheckoutRequestDto
import com.vidrierapp.data.remote.dto.LoginRequestDto
import com.vidrierapp.data.remote.dto.ShopDto
import com.vidrierapp.data.remote.dto.TrialRequestDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoPrivacyTest {

    private val forbiddenTypes = listOf(
        com.vidrierapp.domain.model.Visit::class.java,
        com.vidrierapp.data.local.entity.VisitEntity::class.java,
        MultipartBody::class.java,
        MultipartBody.Part::class.java,
        RequestBody::class.java,
        java.io.File::class.java
    )

    @Test
    fun `api service has no upload endpoint or visit and file parameters`() {
        val methods = VidrierAppApiService::class.java.methods

        assertTrue(methods.isNotEmpty())
        methods.forEach { method ->
            method.parameterTypes.forEach { type ->
                assertFalse("${method.name} takes $type", forbiddenTypes.any { it.isAssignableFrom(type) })
            }
            method.annotations.forEach { annotation ->
                val name = annotation.annotationClass.simpleName
                val forbidden = setOf("POST", "PUT", "PATCH", "Multipart")
                val allowedParameters = setOf(
                    String::class.java,
                    TrialRequestDto::class.java,
                    CheckoutRequestDto::class.java,
                    LoginRequestDto::class.java
                )
                val allowed = method.name in setOf("login", "reportRented", "startTrial", "checkout") &&
                    method.parameterTypes.all { it in allowedParameters || it.name == "kotlin.coroutines.Continuation" }
                assertFalse("${method.name} is $name", name in forbidden && !allowed)
            }
        }
    }

    @Test
    fun `shop dto carries no photo fields`() {
        val fields = ShopDto::class.java.declaredFields.map { it.name.lowercase() }

        assertFalse(fields.any { "photo" in it || "image" in it })
    }
}
