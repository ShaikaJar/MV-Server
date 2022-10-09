package crawler

import data.Show
import data.StreamingService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking

abstract class Crawler<K : ApiClient, out T : ShowPage<K>>(val service: StreamingService, val client: K) {
    protected abstract suspend fun collectPages(): Flow<T>
    fun crawl(): Set<Show> = runBlocking {
        val showSet = HashSet<Show>()
        collectPages().collect { showPage -> showSet.add(showPage.convert()) }
        return@runBlocking showSet
    }
}