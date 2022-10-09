import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.internal.LinkedTreeMap
import crawler.sender.zdf.ZdfApiClient
import crawler.sender.zdf.ZdfCrawler
import crawler.sender.zdf.ZdfShowPage
import kotlinx.coroutines.runBlocking
import java.util.Objects

fun main(args: Array<String>) {
    val client = ZdfApiClient()


    val testPage: ZdfShowPage = ZdfShowPage(
        "https://www.zdf.de/dokumentation/zdfinfo-doku/die-geheimnisse-von-sakkara-der-sensationsfund-100.html",
        client
    )
    //runBlocking { println(testPage.zdfJson()) }

    println(testPage.zdfJson())

    //println(runBlocking {ZdfCrawler().crawl()})

    return

}