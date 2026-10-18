package com.app.aisheildme.bytrix.data.remote

import com.app.aisheildme.bytrix.core.common.AppException
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

class SafeConverterFactory(private val delegate: Converter.Factory) : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): Converter<ResponseBody, *>? {
        val inner = delegate.responseBodyConverter(type, annotations, retrofit) ?: return null
        return Converter<ResponseBody, Any?> { body ->
            try {
                inner.convert(body)
            } catch (e: AppException) {
                throw e
            } catch (e: Exception) {
                throw AppException(AppException.GENERIC)
            }
        }
    }

    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<out Annotation>,
        methodAnnotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): Converter<*, RequestBody>? =
        delegate.requestBodyConverter(type, parameterAnnotations, methodAnnotations, retrofit)
}
