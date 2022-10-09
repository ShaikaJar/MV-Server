package crawler

import java.net.http.HttpResponse

interface ApiClient {
    suspend fun call(url:String, options: HashMap<String, String>): HttpResponse<String>
}