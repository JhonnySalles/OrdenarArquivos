package com.fenix.ordenararquivos.model.entities

data class DriveFile(
    val id: String,
    val name: String,
    val size: Long = 0
) {
    val version: String
        get() {
            val regex = Regex("""ordenarArquivos-(.+?)(?:-jar-with-dependencies)?\.jar""")
            val match = regex.find(name)
            return match?.groupValues?.get(1) ?: ""
        }

    val isWithDependencies: Boolean
        get() = name.contains("jar-with-dependencies")
}
