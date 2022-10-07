package data

import java.time.LocalDate
import kotlin.time.Duration


data class Show(
    val name: String,
    val airDate: LocalDate,
    val duration: Duration,
    val description: String,
    val series: Series,
    val videos: Set<Video>,
    val subtitles: Set<Subtitles>
)