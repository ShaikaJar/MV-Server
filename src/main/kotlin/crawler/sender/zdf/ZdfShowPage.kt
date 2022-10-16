package crawler.sender.zdf

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import crawler.ShowPage
import data.Quality
import data.Subtitles
import data.Video
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.lang.Exception
import java.lang.IllegalStateException
import java.net.URL
import java.time.LocalDate
import java.util.*
import kotlin.time.Duration


class ZdfShowPage(
    url: String,
    val zdfClient: ZdfApiClient,
    val name: String,
    val airDate: LocalDate,
    val description: String,
    val seriesName: String,
    val tags: Set<String>,
    val duration: Duration,
    val streamsUrl: String
) : ShowPage<ZdfApiClient>(
    url.split('.')[0].replace("http://api.zdf.de/content/documents/zdf/", "https://zdf.de/"),
    zdfClient
) {
    private var zdfVideoJson: JsonObject

    init {
        runBlocking {
            zdfVideoJson = Gson().fromJson(
                zdfClient.call(
                    streamsUrl, hashMapOf(Pair("bearer", "video"))
                ).body(), JsonObject().javaClass
            )
        }
    }

    override suspend fun fetchName(): String = name
    override suspend fun fetchAirDate(): LocalDate = airDate

    override suspend fun fetchOriginalAirDate(): LocalDate? = null

    override suspend fun fetchDescription(): String = description
    override suspend fun fetchSeriesName() = seriesName
    override suspend fun fetchTags() = tags
    override suspend fun fetchVideos(): Set<Video> {
        val videos: LinkedList<Video> = LinkedList<Video>()

        val priorityList = zdfVideoJson.getAsJsonArray("priorityList").map(JsonElement::getAsJsonObject)
        for (prio in priorityList) {
            val formitaetenList = prio.getAsJsonArray("formitaeten").map(JsonElement::getAsJsonObject)
            for (formitaet in formitaetenList) {
                val mimetype: String = formitaet["mimeType"].asString

                val qualityList = formitaet.getAsJsonArray("qualities").map(JsonElement::getAsJsonObject)
                for (qualityEntry in qualityList) {
                    val quality = parseQuality(qualityEntry)
                    val audioJson: JsonObject = (qualityEntry["audio"] ?: continue).asJsonObject

                    val tracks: JsonArray = audioJson.getAsJsonArray("tracks")
                    for (track in tracks) videos.add(parseTrack(track.asJsonObject, mimetype, quality))
                }
            }
        }

        return videos.toHashSet()
    }

    private fun parseTrack(trackJson: JsonObject, mimeType: String, quality: Quality): Video = Video(
        quality = quality, url = trackJson["uri"].asString, locale = Locale.forLanguageTag(
            trackJson["language"].asString + when {
                trackJson["class"].asString == "ad" -> "ad"
                else -> ""
            }
        ), mimeType = mimeType, sizeInBytes = null
    )

    private fun parseQuality(qualityObj: JsonObject): Quality {
        return when {
            qualityObj["hd"].asBoolean -> Quality.HIGH
            qualityObj["quality"].asString == "veryhigh" -> Quality.MID
            else -> Quality.LOW
        }
    }

    override suspend fun fetchSubtitle(): Set<Subtitles> = zdfVideoJson.getAsJsonArray("captions").map {
        Subtitles(
            it.asJsonObject["uri"].asString,
            Locale.forLanguageTag(it.asJsonObject["language"].asString)
        )
    }.toSet()

    override suspend fun fetchDuration(): Duration = duration
}