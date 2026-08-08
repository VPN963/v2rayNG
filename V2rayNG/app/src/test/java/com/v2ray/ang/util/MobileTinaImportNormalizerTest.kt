package com.v2ray.ang.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MobileTinaImportNormalizerTest {

    @Test
    fun removesSingleLeadingHashFromBase64Payload() {
        val input = "#dmxlc3M6Ly9leGFtcGxl"
        assertEquals("dmxlc3M6Ly9leGFtcGxl", MobileTinaImportNormalizer.normalize(input))
    }

    @Test
    fun preservesNormalHashPrefixedText() {
        val input = "# import sub"
        assertEquals(input, MobileTinaImportNormalizer.normalize(input))
    }

    @Test
    fun preservesRegularConfigUri() {
        val input = "vless://example"
        assertEquals(input, MobileTinaImportNormalizer.normalize(input))
    }
}
