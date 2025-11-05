package com.trendyol.sessionstore.sample.common.model

enum class DataSize(val bytes: Int, val displayName: String, val exceedsBundleLimit: Boolean) {
    SMALL(128 * 1024, "Small (128KB)", false),
    MEDIUM(256 * 1024, "Medium (256KB)", false),
    LARGE(512 * 1024, "Large (512KB)", false),
    XLARGE(1024 * 1024, "X-Large (1MB)", false),
    XXLARGE(2 * 1024 * 1024, "XX-Large (2MB)", true)
}
