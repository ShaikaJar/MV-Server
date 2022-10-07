package crawler.sender.zdf

import crawler.ApiClient
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class ZdfApiClient : ApiClient {
    var searchBearer: String? = null;
    var videoBearer: String? = null;

    init {
        this.init()
    }

    override fun init() {
        val index = Jsoup.connect("https://www.zdf.de").get()
        searchBearer = parseBearerFromDoc(document = index, elementQuery = "head > script", "'")
        videoBearer = parseBearerFromDoc(document = index, elementQuery = "article > script", "\"")
        if (videoBearer.isNullOrEmpty())
            videoBearer = parseBearerFromDoc(document = index, elementQuery = "main > script", "\"")

        if (searchBearer.isNullOrEmpty() || videoBearer.isNullOrEmpty()) {
            println("SearchBearer: $searchBearer")
            println("VideoBearer: $videoBearer")
            throw IllegalStateException()
        }
    }

    fun parseBearerFromDoc(document: Document, elementQuery: String, bearerQuery: String): String? =
        document.select(elementQuery).firstNotNullOfOrNull { element -> parseBearer(element.html(), bearerQuery) }

    fun parseBearer(element: String, bearerQuery: String): String? {
        val JSON_API_TOKEN = "apiToken"
        val indexToken = element.indexOf(JSON_API_TOKEN)

        if (indexToken <= 0)
            return null

        val indexStart = element.indexOf(bearerQuery, indexToken + JSON_API_TOKEN.length + 1) + 1
        val indexEnd = element.indexOf(bearerQuery, indexStart)

        return element.substring(indexStart, indexEnd)

    }

    override fun call(url:String, options: HashMap<String,String>): HttpResponse<String> {
        val bearer = when {
            !options.containsKey("bearer") -> ""
            options["bearer"] == "search" -> searchBearer
            options["bearer"] == "video" -> videoBearer
            else -> videoBearer
        }
        val requestBuilder: HttpRequest.Builder? =
            HttpRequest.newBuilder(URI.create(url)).setHeader("Api-Auth", "Bearer $bearer")

        val response =  HttpClient.newHttpClient().send(requestBuilder?.build(), HttpResponse.BodyHandlers.ofString())

        //println(response.uri())
        //println(response.statusCode())

        return response
    }
}