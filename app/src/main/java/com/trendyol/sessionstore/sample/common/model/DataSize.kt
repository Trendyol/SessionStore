package com.trendyol.sessionstore.sample.common.model

enum class DataSize(val bytes: Int, val displayName: String, val exceedsBundleLimit: Boolean) {
    SMALL(512 * 1024, "Small (512KB)", false),
    MEDIUM(1024 * 1024, "Medium (1MB)", false),
    LARGE((1.5 * 1024 * 1024).toInt(), "Large (1.5MB)", true),
    XLARGE(3 * 1024 * 1024, "X-Large (3MB)", true),
    XXLARGE(5 * 1024 * 1024, "XX-Large (5MB)", true)
}
