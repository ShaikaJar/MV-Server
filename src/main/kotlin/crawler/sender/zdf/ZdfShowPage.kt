package crawler.sender.zdf

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import crawler.ShowPage
import data.Quality
import data.Subtitles
import data.Video
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.time.Duration


class ZdfShowPage(url: String, val zdfClient: ZdfApiClient) : ShowPage<ZdfApiClient>(url, zdfClient) {
    private val document: Document = Jsoup.connect(url).get()

    private var _zdfJson: Deferred<JsonObject> = GlobalScope.async {
        Gson().fromJson(
            zdfClient.call(
                url.replace("https://www.zdf.de/", "https://api.zdf.de/content/documents/zdf/")
                    .replace(".html", ".json?profile=player-3"),
                hashMapOf(Pair("bearer", "video"))
            ).body(), JsonObject().javaClass
        )
    }

    public fun zdfJson() = runBlocking { _zdfJson.await() }

    private var _zdfVideoJson: Deferred<JsonObject> = GlobalScope.async {
        Gson().fromJson(
            zdfClient.call(
                "https://api.zdf.de" +
                        zdfJson().getAsJsonObject("mainVideoContent")
                            .getAsJsonObject("http://zdf.de/rels/target")
                                ["http://zdf.de/rels/streams/ptmd-template"].asString.replace(
                            "{playerId}",
                            "ngplayer_2_4"
                        ),
                hashMapOf(Pair("bearer", "video"))
            ).body(),
            JsonObject().javaClass
        )
    }

    public fun zdfVideoJson() = runBlocking { _zdfVideoJson.await() }

    override suspend fun fetchName(): String = zdfJson()["title"].asString

    override suspend fun fetchAirDate(): LocalDate = LocalDate.parse(
        zdfJson()["editorialDate"].asString,
        DateTimeFormatter.ISO_OFFSET_DATE_TIME
    )


    override suspend fun fetchDescription(): String = zdfJson()["leadParagraph"].asString

    override suspend fun fetchSeriesName() = zdfJson()["http://zdf.de/rels/brand"].asJsonObject["title"].asString
    override suspend fun fetchTags() = setOf(zdfJson()["http://zdf.de/rels/category"].asJsonObject["title"].asString)
    override suspend fun fetchVideos(): Set<Video> {
        val videos: LinkedList<Video> = LinkedList<Video>()

        val priorityList = zdfVideoJson().getAsJsonArray("priorityList").map(JsonElement::getAsJsonObject)
        for (prio in priorityList) {
            val formitaetenList = prio.getAsJsonArray("formitaeten").map(JsonElement::getAsJsonObject)
            for (formitaet in formitaetenList) {
                val mimetype: String = formitaet["mimeType"].asString

                val qualityList = formitaet.getAsJsonArray("qualities").map(JsonElement::getAsJsonObject)
                for (qualityEntry in qualityList) {
                    val quality = parseQuality(qualityEntry)
                    val audioJson: JsonObject = (qualityEntry["audio"] ?: continue).asJsonObject

                    val tracks: JsonArray = audioJson.getAsJsonArray("tracks")
                    for (track in tracks)
                        videos.add(parseTrack(track.asJsonObject, mimetype, quality))
                }
            }
        }

        return videos.toHashSet()
    }

    private fun parseTrack(trackJson: JsonObject, mimeType: String, quality: Quality): Video =
        Video(
            quality = quality,
            url = trackJson["uri"].asString,
            locale = Locale.forLanguageTag(
                trackJson["language"].asString +
                        when {
                            trackJson["class"].asString == "ad" -> "ad"
                            else -> ""
                        }
            ),
            mimeType = mimeType,
            sizeInBytes = null
        )

    private fun parseQuality(qualityObj: JsonObject): Quality {
        return when {
            qualityObj["hd"].asBoolean -> Quality.HIGH
            qualityObj["quality"].asString == "veryhigh" -> Quality.MID
            else -> Quality.LOW
        }
    }

    override suspend fun fetchSubtitle(): Set<Subtitles> {
        //TODO("Not yet implemented")
        return emptySet()
    }

    override suspend fun fetchDuration(): Duration = Duration.ZERO
}