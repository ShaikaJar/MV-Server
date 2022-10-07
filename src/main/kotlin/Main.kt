import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.internal.LinkedTreeMap
import crawler.sender.zdf.ZdfApiClient
import crawler.sender.zdf.ZdfShowPage
import java.util.Objects

fun main(args: Array<String>) {

    val testPage: ZdfShowPage = ZdfShowPage("https://www.zdf.de/dokumentation/planet-e/planet-e-trueffel---der-geheime-schatz-unserer-waelder-100.html", ZdfApiClient())
    println(testPage.fetchVideos())

}