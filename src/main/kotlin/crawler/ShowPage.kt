package crawler

import data.Series
import data.Show
import data.Subtitles
import data.Video
import java.time.LocalDate
import kotlin.time.Duration

abstract class ShowPage<K: ApiClient> (val url:String, client: K) {
    abstract fun fetchName():String
    abstract fun fetchAirDate():LocalDate
    abstract fun fetchDuration():Duration
    abstract fun fetchDescription():String
    abstract fun fetchSeriesPage(): SeriesPage<K>
    abstract fun fetchVideos(): Set<Video>
    abstract fun fetchSubtitle(): Set<Subtitles>

    fun convert(series: Series): Show =
        Show(
            name = fetchName(),
            airDate = fetchAirDate(),
            duration = fetchDuration(),
            description = fetchDescription(),
            series= series,
            videos = fetchVideos(),
            subtitles = fetchSubtitle()
        )
}