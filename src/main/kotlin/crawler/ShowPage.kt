package crawler

import data.Series
import data.Show
import data.Subtitles
import data.Video
import java.net.URL
import java.time.LocalDateTime
import kotlin.time.Duration

abstract class ShowPage (val url:URL){
    abstract fun fetchName():String
    abstract fun fetchOriginalAirDate():LocalDateTime
    abstract fun fetchAirDate():LocalDateTime?
    abstract fun fetchDuration():Duration
    abstract fun fetchDescription():String
    abstract fun fetchSeriesPage(): SeriesPage
    abstract fun fetchVideos(): Set<Video>
    abstract fun fetchSubtitle(): Set<Subtitles>

    fun convert(series: Series): Show =
        Show(
            name = fetchName(),
            originalAirDate = fetchOriginalAirDate(),
            airDate = fetchAirDate(),
            duration = fetchDuration(),
            description = fetchDescription(),
            series= series,
            videos = fetchVideos(),
            subtitles = fetchSubtitle()
        )
}