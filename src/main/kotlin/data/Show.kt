package data

import java.time.LocalDateTime
import kotlin.time.Duration


data class Show(
    val name: String,
    val originalAirDate: LocalDateTime,
    val airDate: LocalDateTime?,
    val duration: Duration,
    val description: String,
    val series: Series,
    val videos: Set<Video>,
    val subtitles: Set<Subtitles>
)