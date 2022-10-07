package crawler.sender.zdf

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import crawler.SeriesPage
import crawler.ShowPage
import data.Quality
import data.Subtitles
import data.Video
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.time.Duration


class ZdfShowPage(url: String, val zdfClient: ZdfApiClient) : ShowPage<ZdfApiClient>(url, zdfClient) {
    private val document: Document = Jsoup.connect(url).get()

    private val zdfJson: JsonObject = Gson().fromJson(
        zdfClient.call(
            url.replace("https://www.zdf.de/", "https://api.zdf.de/content/documents/zdf/")
                .replace(".html", ".json?profile=player-3"),
            hashMapOf(Pair("bearer", "video"))

        ).body(), JsonObject().javaClass
    )

    private val zdfVideoJson: JsonObject = Gson().fromJson(
        zdfClient.call(
            "https://api.zdf.de" +
                    zdfJson.getAsJsonObject("mainVideoContent")
                        .getAsJsonObject("http://zdf.de/rels/target")
                            ["http://zdf.de/rels/streams/ptmd-template"].asString.replace(
                        "{playerId}",
                        "ngplayer_2_4"
                    ),
            hashMapOf(Pair("bearer", "video"))
        ).body(),
        JsonObject().javaClass
    )

    override fun fetchName(): String = zdfJson["name"].asString

    override fun fetchAirDate(): LocalDate = LocalDate.parse(
        zdfJson["editorialDate"].asString,
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    )


    override fun fetchDescription(): String = zdfJson["leadParagraph"].asString

    override fun fetchSeriesPage(): SeriesPage<ZdfApiClient> =
        ZdfSeriesPage(document.select("a.m-clickarea-action js-track-click")[0].attr("href"), zdfClient)

    override fun fetchVideos(): Set<Video> {
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

    override fun fetchSubtitle(): Set<Subtitles> {
        TODO("Not yet implemented")
    }

    override fun fetchDuration(): Duration {
        TODO("Not yet implemented")
    }
}