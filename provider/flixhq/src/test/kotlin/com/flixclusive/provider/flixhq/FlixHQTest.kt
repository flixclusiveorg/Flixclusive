package com.flixclusive.provider.flixhq

import com.flixclusive.provider.base.testing.BaseProviderTest
import com.flixclusive.provider.base.util.CryptographyHelper.decryptAes
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@Suppress("SpellCheckingInspection")
class FlixHQTest : BaseProviderTest() {

    @Before
    override fun setUp() {
        super.setUp()

        sourceProvider = FlixHQ(OkHttpClient())
    }

    @Test
    fun `verify decryption tool`() {
        val encString = "U2FsdGVkX19CIuaIRgRRf1pJVxKY/7n1obeNUcQrizcCUdaPwDa7OG3pNT7KDRD7DfFIlUha8IPbVvmtYsY+1ehAGVp3mV5KtcVo+8AfsoqWkSkP7KuCTggiymPOQg094fWDy4pHAUGHe+RmO9ZQi0SE+MWrDbKTeAhtC4TlLVpHJMFYqw1rlNdAD7rRp6H5HCsnG8QIZ1QqQN8G3nnYd5BYbYkvvsYT+ahTlY2r1C5CkDzyaeb647VVjRsYBSTAIaCVk3xC/seXA3dQgICoVWPHXPgVSMj0USc8Dirdacc0U3HL0ySkZPtxC8mhhr1bXnnHCN10SACWEJpZwDXhyPE6pwer0FnySnUW/eqVGiCpZxdvNjWIuvGBqtyNUIm1cpzQsxRfVreq7YwP1BDFU2OwYPtFq7DY4KPqbZtLGSAHRL0OcmnucVuxRRRUIn5PPxdL2my26YjNZHUuzgySOsMuvpp1LwpXk2HdyZWOJmft0JwdKgj4RDz5yKDbGXIF"
        val key = "ZTwVpfLANX3t6Xb7cBpbkmFrGT0bCpEvBiN"

        val result = decryptAes(encString, key)
        assertEquals(true, result.contains("http"))
    }
}