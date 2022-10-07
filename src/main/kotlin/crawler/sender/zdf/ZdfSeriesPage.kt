package crawler.sender.zdf

import com.google.gson.Gson
import com.google.gson.JsonObject
import crawler.SeriesPage
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class ZdfSeriesPage(url: String, client: ZdfApiClient) : SeriesPage<ZdfApiClient>(url, client) {
    val document: Document = Jsoup.connect(url).get()

    override fun fetchName(): String = document.select("span.no-link")[0].text().trim()

    override fun fetchTags(): Set<String> = setOf(
        Gson().fromJson(
            document.select("script[type=application/ld+json]")[0].text(),
            JsonObject().javaClass
        )["genre"].asString
    )
}