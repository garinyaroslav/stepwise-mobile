import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

fun formatIsoToDdMmYyyy(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return try {
        val outFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        when {
            iso.length <= 10 -> LocalDate.parse(iso).format(outFmt)
            iso.contains('Z') || iso.contains('+') -> OffsetDateTime.parse(iso).toLocalDate().format(outFmt)
            else -> LocalDateTime.parse(iso).toLocalDate().format(outFmt)
        }
    } catch (t: Throwable) {
        iso
    }
}