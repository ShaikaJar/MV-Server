import crawler.Crawler
import crawler.sender.zdf.ZdfApiClient
import crawler.sender.zdf.ZdfCrawler
import crawler.sender.zdf.ZdfShowPage
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {

    val searchUrl =
        "https://api.zdf.de/search/documents?hasVideo=true&q=*&types=page-video&sortOrder=desc&sortBy=date&page=1"
    runBlocking {
        val result = ZdfApiClient().call(
            searchUrl,
            hashMapOf(Pair("bearer", "search"))
        )
        println(result.statusCode())
        println(result.headers().map())
    }

    val crawler = ZdfCrawler()

    runBlocking {
        val shows = crawler.crawl()
        for (show in shows)  println(show.name)
    }

}