package crawler

import java.net.http.HttpResponse

interface ApiClient {
    fun init()
    fun call(url:String, options: HashMap<String, String>): HttpResponse<String>
}