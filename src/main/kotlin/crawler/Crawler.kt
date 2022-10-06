package crawler

import data.Series
import data.StreamingService
import kotlin.collections.HashSet

abstract class Crawler<out T : ShowPage> (val service:StreamingService){
    protected abstract fun collectPages(): List<T>
    fun crawl(): Set<Series> {
        val showPages: Set<T> = collectPages().toSet()
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