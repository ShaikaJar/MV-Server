package crawler

import data.Series
import data.StreamingService
import kotlin.collections.HashSet

abstract class Crawler<K:ApiClient, out T : ShowPage<K>> (val service:StreamingService, val client:K){
    protected abstract fun collectPages(client: K): List<T>
    fun crawl(): Set<Series> {
        val showPages: Set<T> = collectPages(client).toSet()
        var seriesSet: HashSet<Series> = HashSet<Series>()
        for (showPage in  showPages){
            var  series = showPage.fetchSeriesPage().convert(service)
            if (seriesSet.contains(series))
                series = seriesSet.elementAt(seriesSet.indexOf(series))
            else
                seriesSet.add(series)
            series.addShow(showPage.convert(series))
        }
        return seriesSet
    }
}