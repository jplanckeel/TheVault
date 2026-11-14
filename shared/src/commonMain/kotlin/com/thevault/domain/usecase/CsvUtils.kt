package com.thevault.domain.usecase

object CsvUtils {
    fun parseCsv(csvString: String): List<List<String>> {
        val result = mutableListOf<List<String>>()
        var currentRecord = mutableListOf<String>()
        var currentField = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < csvString.length) {
            val char = csvString[i]
            val nextChar = if (i < csvString.length - 1) csvString[i + 1] else null

            if (inQuotes) {
                if (char == '"' && nextChar == '"') {
                    // Escaped quote
                    currentField.append('"')
                    i++ // Skip next char
                } else if (char == '"') {
                    // End of quoted field
                    inQuotes = false
                } else {
                    currentField.append(char)
                }
            } else {
                if (char == '"') {
                    inQuotes = true
                } else if (char == ',') {
                    currentRecord.add(currentField.toString().trim())
                    currentField.clear()
                } else if (char == '\n' || char == '\r') {
                    if (char == '\r' && nextChar == '\n') {
                        // Skip \r if it's \r\n
                        i++
                    }
                    currentRecord.add(currentField.toString().trim())
                    result.add(currentRecord)
                    currentRecord = mutableListOf()
                    currentField.clear()
                } else {
                    currentField.append(char)
                }
            }
            i++
        }

        // Add the last field/record if not empty
        if (currentField.isNotEmpty() || csvString.endsWith(",")) {
            currentRecord.add(currentField.toString())
        }
        if (currentRecord.isNotEmpty()) {
            result.add(currentRecord)
        }

        return result
    }

    fun generateCsv(
        headers: List<String>,
        rows: List<List<String>>,
    ): String {
        val sb = StringBuilder()
        sb.append(headers.joinToString(",") { escapeCsvField(it) }).append("\n")
        for (row in rows) {
            sb.append(row.joinToString(",") { escapeCsvField(it) }).append("\n")
        }
        return sb.toString()
    }

    private fun escapeCsvField(field: String): String {
        if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            return "\"" + field.replace("\"", "\"\"") + "\""
        }
        return field
    }
}
