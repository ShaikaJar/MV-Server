package crawler

import data.Series
import data.StreamingService
import java.net.URL

abstract class SeriesPage(val url: URL) {

    fun convert(service: StreamingService): Series =
        Series(name = fetchName(), tags = fetchTags(), service = service)

    protected abstract fun fetchName(): String
    protected abstract fun fetchTags(): Set<String>
}