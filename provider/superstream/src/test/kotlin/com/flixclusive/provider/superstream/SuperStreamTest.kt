package com.flixclusive.provider.superstream

import com.flixclusive.core.util.log.debugLog
import com.flixclusive.core.util.network.asString
import com.flixclusive.provider.base.testing.BaseProviderTest
import com.flixclusive.provider.superstream.SuperStreamCommon.apiUrl
import com.flixclusive.provider.superstream.util.CipherUtils.decrypt
import junit.framework.TestCase.assertNotNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Before
import org.junit.Test

class SuperStreamTest : BaseProviderTest() {
    private val data = "eyJhcHBfa2V5IjoiNjQwNWMyZTYwNDVmNjU5YjdmZGNkOTU3ZmU1MmZlOWEiLCJ2ZXJpZnkiOiJmYzhjYzg0ODc5NDVjNjBmY2Y5YWJkMTNhZWU1ODNmNCIsImVuY3J5cHRfZGF0YSI6IkZER2kwcGV3R2MyUklWdTJKM2ltWVRVRU9abHZaM0xhdVo0c25YNXVaZUZWbi9ZYi9ROVA5WWFSMDViekhUWVpJYmIrQ0VpQzBwSVZuS0ZNR3AwYnVSTEFLS1pqbENOQm9oakNlaEpYSEF5NmRYSUxmN3RSY01jTXR3TElYWFlqd2xtdjJEQlQxaFRmNS9xU29QZlJKZlRXT1FJU3Q2eWhWQ3I4OG9zNEl6QldSZnV4ZmhuMDFScS9keVRRNCszSFlodUwvdTlxZXZmZkxyTnVOWmhnd1kvdDArNnZVSzNwR2dXU09ablZGRWtnY3ZvV1k3RFNTelNuTXBXMTJpKzFFQVpNTXFRNHNDVGYydURyVW9ZeklQQlZTOU9CbDY1emF0WnJVMHdPYkp3PSJ9"

    @Before
    override fun setUp() {
        super.setUp()

        sourceProvider = SuperStream(OkHttpClient())
    }

    @Test
    fun decryption() {
        val result = decrypt(data, SuperStreamCommon.key, SuperStreamCommon.iv)

        assertNotNull(result)
        debugLog(result!!)
    }


    @Test
    fun cloudfareTest() {
        val client = OkHttpClient()
        val mediaType = "application/x-www-form-urlencoded".toMediaType()
        val body = "data=$data&appid=27&platform=android&version=160&medium=Website%26token=f8ed3c62d0d57a5823ccbe14b802ed57".toRequestBody(mediaType)
        val request = Request.Builder()
            .url(apiUrl)
            .post(body)
            .addHeader("Platform", "android")
            .addHeader("Accept", "charset=utf-8")
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .build()

        val responseBody = client.newCall(request).execute().body?.charStream()?.asString()

        assertNotNull(responseBody)
        assert(responseBody!!.contains("cloudfare", true).not())
        println(responseBody)
    }
}