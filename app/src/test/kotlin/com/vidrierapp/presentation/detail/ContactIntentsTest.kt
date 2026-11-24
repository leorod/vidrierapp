package com.vidrierapp.presentation.detail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContactIntentsTest {

    @Test
    fun normalizePhoneKeepsPlusAndDigits() {
        assertEquals("+541146871234", ContactIntents.normalizePhone(" +54 11 4687-1234 "))
    }

    @Test
    fun normalizePhoneDropsFormattingWithoutPlus() {
        assertEquals("1146871234", ContactIntents.normalizePhone("(11) 4687-1234"))
    }

    @Test
    fun dialBuildsTelUri() {
        assertEquals(
            ContactIntentData(ContactIntents.ACTION_DIAL, "tel:+541146871234"),
            ContactIntents.dial("+54 11 4687-1234")
        )
    }

    @Test
    fun dialRejectsBlankOrNonNumeric() {
        assertNull(ContactIntents.dial(""))
        assertNull(ContactIntents.dial("abc"))
    }

    @Test
    fun whatsAppUsesDigitsOnlyAndEncodesMessage() {
        val data = ContactIntents.whatsApp("+54 11 4687-1234", "Hola, consulto por el local en Av. Alberdi 6420 & más")
        assertEquals(ContactIntents.ACTION_VIEW, data?.action)
        assertEquals(
            "https://api.whatsapp.com/send?phone=541146871234&text=Hola%2C%20consulto%20por%20el%20local%20en%20Av.%20Alberdi%206420%20%26%20m%C3%A1s",
            data?.uri
        )
    }

    @Test
    fun whatsAppRejectsInvalidPhone() {
        assertNull(ContactIntents.whatsApp("--", "x"))
    }

    @Test
    fun emailBuildsMailtoWithEncodedSubject() {
        val data = ContactIntents.email(" info@x.example ", "Consulta por el local en Haedo")
        assertEquals(ContactIntents.ACTION_SENDTO, data?.action)
        assertEquals("mailto:info@x.example?subject=Consulta%20por%20el%20local%20en%20Haedo", data?.uri)
    }

    @Test
    fun emailRejectsBlank() {
        assertNull(ContactIntents.email("  ", "s"))
    }
}
