package crawler.sender.zdf

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import crawler.Crawler
import crawler.ShowPage
import data.StreamingService
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import java.util.LinkedList

class ZdfCrawler() : Crawler<ZdfApiClient, ShowPage<ZdfApiClient>>(StreamingService.ZDF, ZdfApiClient()) {

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    override suspend fun collectPages(): ReceiveChannel<ZdfShowPage> = GlobalScope.produce<ZdfShowPage>(searchPool.asCoroutineDispatcher()){
        var count = 1
        var complete = 0
        for (i in 0..20){
            //println("Searching Page $i")
            val searchUrl =
                "https://api.zdf.de/search/documents?hasVideo=true&q=*&types=page-video&sortOrder=desc&sortBy=date&page=${i + 1}"
            val result = client.call(
                searchUrl,
                hashMapOf(Pair("bearer", "search"))
            ).body()

            val showEntryList = Gson().fromJson<JsonObject>(
                result, JsonObject().javaClass
            ).getAsJsonArray("http://zdf.de/rels/search/results").map { jsonElement -> jsonElement.asJsonObject }

            for (showEntry in showEntryList) {
                //println("Adding Show from Page $i")
                try {
                    val target: JsonObject = showEntry.getAsJsonObject("http://zdf.de/rels/target")

                    try {
                        val urlElement: JsonElement = target["http://zdf.de/rels/sharing-url"]

                        try {
                            val url: String = urlElement.asString
                            async(searchPool.asCoroutineDispatcher()) {
                                count++
                                try {
                                    val page = ZdfShowPage(
                                        url,
                                        client
                                    )
                                    send(page)
                                }catch (_:java.lang.Exception){
                                    println("Seite konnte nicht abgerufen werden")
                                }
                                complete++
                                //println("ZDF: Completed $complete/$count")
                                if (count <= complete) {
                                    println("All Pages collected")
                                    channel.close()
                                }
                            }

                        }catch (_:Exception){
                            println("Couldn't convert Url")
                            println("Element was: $urlElement")
                        }

                    }catch (_:Exception){
                        println("Couldn't find Url-Element")
                        println("Available Keys were: ${target.keySet()}")
                    }

                }catch (_:Exception){
                    println("Couldn't find Target")
                    println("Available Keys were: ${showEntry.keySet()}")
                }

            }

        }

        complete++
        println("crawl(): Completed $complete/$count")
        if (count <= complete) {
            println("All Pages converted")
            channel.close()
        }
    }
}