package crawler.sender.zdf

import com.google.gson.Gson
import com.google.gson.JsonObject
import crawler.ApiClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
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
        runBlocking {
            val index = Jsoup.connect("https://www.zdf.de").get()
            val videoBearerTask = async {
                var videoToken = parseBearerFromDoc(document = index, elementQuery = "article > script")
                if (videoToken.isNullOrEmpty())
                    videoToken = parseBearerFromDoc(document = index, elementQuery = "main > script")
                videoBearer = videoToken
            }
            val searchBearerTask =
                async { searchBearer = parseBearerFromDoc(document = index, elementQuery = "head > script") }
            awaitAll(searchBearerTask, videoBearerTask)
        }

        if (searchBearer.isNullOrEmpty() || videoBearer.isNullOrEmpty()) {
            println("SearchBearer: $searchBearer")
            println("VideoBearer: $videoBearer")
            throw IllegalStateException()
        }
    }

    fun parseBearerFromDoc(document: Document, elementQuery: String): String? =
        document.select(elementQuery).firstNotNullOfOrNull { element ->
            when {
                element.html().contains("apiToken") -> element.html().split("apiToken")[1].split('"', '\'')[1]
                else -> null
            }
        }


    override suspend fun call(url: String, options: HashMap<String, String>): HttpResponse<String> {
        val bearer = when {
            !options.containsKey("bearer") -> ""
            options["bearer"] == "search" -> searchBearer
            options["bearer"] == "video" -> videoBearer
            else -> videoBearer
        }
        val requestBuilder: HttpRequest.Builder? =
            HttpRequest.newBuilder(URI.create(url)).setHeader("Api-Auth", "Bearer $bearer")

        val response = HttpClient.newHttpClient().send(requestBuilder?.build(), HttpResponse.BodyHandlers.ofString())

        //println(response.uri())
        //println(response.statusCode())

        return response
    }
}