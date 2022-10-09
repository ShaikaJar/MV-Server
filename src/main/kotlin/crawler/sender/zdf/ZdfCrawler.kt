package crawler.sender.zdf

import com.google.gson.Gson
import com.google.gson.JsonObject
import crawler.Crawler
import crawler.ShowPage
import data.StreamingService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.yield

class ZdfCrawler() : Crawler<ZdfApiClient, ShowPage<ZdfApiClient>>(StreamingService.ZDF, ZdfApiClient()) {

    override suspend fun collectPages(): Flow<ZdfShowPage> = flow {
        repeat(10) { i ->
            val searchUrl =
                "https://api.zdf.de/search/documents?hasVideo=true&q=*&types=page-video&sortOrder=desc&sortBy=date&page=${i + 1}"
            val showEntryList = Gson().fromJson<JsonObject>(
                client.call(
                    searchUrl,
                    hashMapOf(Pair("bearer", "search"))
                ).body(), JsonObject().javaClass
            ).getAsJsonArray("http://zdf.de/rels/search/results").map { jsonElement -> jsonElement.asJsonObject }

            for (showEntry in showEntryList) {
                emit(
                    ZdfShowPage(
                        showEntry.getAsJsonObject("http://zdf.de/rels/target")["http://zdf.de/rels/sharing-url"].asString,
                        client
                    )
                )
            }
        }
    }
}