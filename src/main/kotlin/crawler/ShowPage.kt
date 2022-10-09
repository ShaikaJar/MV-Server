package crawler

import data.Show
import data.Subtitles
import data.Video
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate
import kotlin.time.Duration

abstract class ShowPage<K : ApiClient>(val url: String, client: K) {
    abstract suspend fun fetchName(): String
    abstract suspend fun fetchAirDate(): LocalDate
    abstract suspend fun fetchDuration(): Duration
    abstract suspend fun fetchDescription(): String
    abstract suspend fun fetchSeriesName(): String
    abstract suspend fun fetchVideos(): Set<Video>
    abstract suspend fun fetchSubtitle(): Set<Subtitles>
    abstract suspend fun fetchTags(): Set<String>

    suspend fun convert(): Show = coroutineScope {
        val name = async { fetchName() }
        val airDate = async { fetchAirDate() }
        val duration = async { fetchDuration() }
        val description = async { fetchDescription() }
        val videos = async { fetchVideos() }
        val subtitles = async { fetchSubtitle() }
        val seriesName = async { fetchSeriesName() }
        val tags = async { fetchTags() }
        return@coroutineScope Show(
            name = name.await(),
            airDate = airDate.await(),
            duration = duration.await(),
            description = description.await(),
            videos = videos.await(),
            subtitles = subtitles.await(),
            seriesName = seriesName.await(),
            tags = tags.await()
        )
    }
}