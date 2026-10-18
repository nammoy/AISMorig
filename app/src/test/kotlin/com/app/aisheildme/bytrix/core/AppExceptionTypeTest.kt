package com.app.aisheildme.bytrix.core

import com.app.aisheildme.bytrix.core.common.AppException
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class AppExceptionTypeTest {

    @Test
    fun `AppException is an IOException so OkHttp does not crash the dispatcher`() {
        assertTrue(AppException("boom") is IOException)
    }
}
