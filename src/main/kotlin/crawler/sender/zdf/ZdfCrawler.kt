package crawler.sender.zdf

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import crawler.Crawler
import crawler.ShowPage
import data.StreamingService
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.produce
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

class ZdfCrawler() : Crawler<ZdfApiClient, ShowPage<ZdfApiClient>>(StreamingService.ZDF, ZdfApiClient()) {


    var searchCount = 0
    var searchCompleted = 0

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun autoCloseChannel(channel: SendChannel<ZdfShowPage>, callback: () -> Unit) {
        searchCount++
        GlobalScope.async(searchPool.asCoroutineDispatcher()) {
            callback.invoke()
        }.await()
        searchCompleted++
        if (searchCount >= searchCompleted)
            channel.close()
    }

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    override suspend fun collectPages(): ReceiveChannel<ZdfShowPage> =
        GlobalScope.produce<ZdfShowPage>(searchPool.asCoroutineDispatcher()) {
            autoCloseChannel(
                channel
            ) {
                runBlocking {
                    var totalresults = Int.MAX_VALUE
                    var i = 0
                    var resultCount = 0
                    val date = DateTimeFormatter.ISO_DATE.format(LocalDate.now())
                    while (totalresults > resultCount) {

                        println("Found ${resultCount}/${totalresults}")
                        //println("Searching Page $i")
                        val searchUrl =
                            "https://api.zdf.de/search/documents?hasVideo=true&q=*&types=page-video&from=${date}T00:00:00.000%2B01:00&to=${date}T23:59:59.999%2B01:00&sortOrder=desc&sortBy=date&page=${i + 1}"
                        val result = client.call(
                            searchUrl,
                            hashMapOf(Pair("bearer", "search"))
                        ).body()

                        val resultObj = Gson().fromJson<JsonObject>(
                            result, JsonObject().javaClass
                        )

                        totalresults = resultObj["totalResultsCount"].asInt


                        val showEntryList = resultObj.getAsJsonArray("http://zdf.de/rels/search/results")
                            .map { jsonElement -> jsonElement.asJsonObject }
                        i++
                        resultCount += showEntryList.size


                        for (showEntry in showEntryList) {
                            println("Start-0")
                            parsePage(showEntry, channel)
                        }

                        println("End ${resultCount}/${totalresults}")
                    }
                }
            }
        }


    @OptIn(ExperimentalTime::class, DelicateCoroutinesApi::class)
    suspend fun parsePage(showEntry: JsonObject, channel: SendChannel<ZdfShowPage>) = autoCloseChannel(
        channel = channel
    ) {
        runBlocking {
            println("Start")
            var target: JsonObject? = null
            try {
                target = showEntry.getAsJsonObject("http://zdf.de/rels/target")
            } catch (_: Exception) {
                return@runBlocking
            }

            println("target aquired")

            var mainVideoContent: JsonObject? = null
            try {
                mainVideoContent =
                    target.getAsJsonObject("mainVideoContent").getAsJsonObject("http://zdf.de/rels/target")
            } catch (_: Exception) {
                return@runBlocking
            }

            println("mainVideoContent aquired")

            var urlString: String? = null
            try {
                urlString = "http://api.zdf.de" + target["self"].asString
            } catch (_: Exception) {
                return@runBlocking
            }

            println("urlString aquired")

            var name: String? = null
            try {
                name = mainVideoContent["title"].asString
            } catch (_: Exception) {
                return@runBlocking
            }
            println("name aquired")

            var airDate: LocalDate? = null
            try {
                val dateString = target["editorialDate"].toString().replace("\"","")
                println(dateString)
                airDate = LocalDate.parse(dateString, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            } catch (_: Exception) {
                return@runBlocking
            }
            println("airDate aquired")

            var description: String? = null
            try {
                description = target["teasertext"].asString
            } catch (_: Exception) {
                return@runBlocking
            }
            println("description aquired")

            var seriesName: String? = null
            try {
                seriesName = target.getAsJsonObject("http://zdf.de/rels/brand")["title"].asString
            } catch (_: Exception) {
                return@runBlocking
            }
            println("seriesName aquired")

            var category: String? = null
            try {
                category = target.getAsJsonObject("http://zdf.de/rels/category")["title"].asString
            } catch (_: Exception) {
                return@runBlocking
            }
            println("category aquired")


            var duration: Int? = null
            try {
                duration = mainVideoContent["duration"].asInt
            } catch (_: Exception) {
                return@runBlocking
            }
            println("duration aquired")

            var streamsUrl: String? = null
            try {
                streamsUrl =
                    "https://api.zdf.de" + mainVideoContent["http://zdf.de/rels/streams/ptmd-template"].asString.replace(
                        "{playerId}",
                        "ngplayer_2_4"
                    )
            } catch (_: Exception) {
                return@runBlocking
            }
            println("streamsUrl aquired")
            println(streamsUrl)

            try {
                val page = ZdfShowPage(
                    url = urlString,
                    zdfClient = client,
                    name = name,
                    airDate = airDate,
                    description = description,
                    seriesName = seriesName,
                    tags = setOf(category),
                    duration = Duration.seconds(duration),
                    streamsUrl = streamsUrl
                )
                channel.send(page)
            } catch (e: java.lang.Exception) {
                println("Seite konnte nicht abgerufen werden")
                println(e.message)
            }
        }
    }
}
