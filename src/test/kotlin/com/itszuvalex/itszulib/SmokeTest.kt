package com.itszuvalex.itszulib

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SmokeTest {
    @Test
    fun modId_IsLowercase() {
        assertEquals(ItszuLib.ID.lowercase(), ItszuLib.ID)
    }
}
