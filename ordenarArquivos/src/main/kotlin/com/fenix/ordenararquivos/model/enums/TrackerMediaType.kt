package com.fenix.ordenararquivos.model.enums

enum class TrackerMediaType(val label: String, val color: String) {
    MANGA("MANGA", "#1B8A5A"),
    NOVEL("NOVEL", "#D35400"),
    UNKNOWN("MANGA", "#1B8A5A");

    companion object {
        fun from(type: String?): TrackerMediaType {
            if (type.isNullOrBlank()) return MANGA
            val normalized = type.trim().lowercase().replace("-", "_").replace(" ", "_")
            return when {
                normalized.contains("novel") || normalized.contains("light_novel") -> NOVEL
                else -> MANGA
            }
        }
    }
}
