package com.prime.media.core.billing

object Private {
    /**
     * Base64-encoded RSA public key to include in your app binary
     */
    const val PLAY_CONSOLE_PUBLIC_KEY =
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAyO00F6uv5E62iLZf8mSH7NNn86Ppu6xzx3pnu+AIGd9rI4DFv7KACjIKkBTm4TrT3J8Wv5MB1PRK8znQFGZSLWxlYLMEfKMFqcsHgRgRRd4GRCFgOJLEsD9VcwgYkrlkMGYm1r6cWT0ZbNcBjMX98Ft0xRxxZbX7/W5jAZgdMcQon0turSWueGkqryzhfzzhbUh0tG4UqJdjuUJgKChZpXyEYSz5v+IczDwQbsfJ0aPJMO9B2fAGGLIqR8v/a4nwot4e817dmBi8HeKc408RuBLvFsi55DAVL8mQFnBXX7c611m45q7D4n2V7abya5uohTT1JFLkTitoi6JSF44V5QIDAQAB"

    const val UNITY_APP_ID = "4915933"
}

object Product {

    const val DISABLE_ADS = "disable_ads"

}

object Placement {

    const val INTERSTITIAL = "interstitial"

    const val BANNER_CONSOLE = "banner_console"

    const val BANNER_SETTINGS = "banner_settings"

    const val BANNER_EXPLORER = "banner_explorer"

    const val BANNER_VIEWER = "banner_viewer"
}