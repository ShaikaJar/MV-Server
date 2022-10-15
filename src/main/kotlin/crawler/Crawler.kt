package crawler

import data.Show
import data.StreamingService
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

abstract class Crawler<K : ApiClient, T : ShowPage<K>>(val service: StreamingService, val client: K) {

    protected val searchThreadNumbers: Int = 30
    protected val pageThreadNumbers: Int = 50
    protected val searchPool = ThreadPoolExecutor(searchThreadNumbers, searchThreadNumbers, 3L, TimeUnit.SECONDS, LinkedBlockingQueue())
    protected val pagePool = ThreadPoolExecutor(pageThreadNumbers, pageThreadNumbers, 3L, TimeUnit.SECONDS, LinkedBlockingQueue())

    protected abstract suspend fun collectPages(): ReceiveChannel<T>
    suspend fun crawl(): ReceiveChannel<Show> = GlobalScope.produce<Show>(pagePool.asCoroutineDispatcher()) {
        val pages = collectPages()
        var count = 1
        var complete = 0
        for (showPage in pages) {
            async(pagePool.asCoroutineDispatcher()) {
                count++
                //println("Recieved: ${showPage.url}")
                for (i in 0..3) {
                    try {
                        val show = showPage.convert()
                        if (show != null) channel.send(show)
                        break
                    } catch (e: Exception) {
                        //println("Error converting Page: ${e.message}")
                    }
                }
                complete++
                println("crawl(): Completed $complete/$count")
                if (count <= complete) {
                    println("All Pages converted")
                    channel.close()
                }
            }
        }
        complete++
        println("crawl(): Completed $complete/$count")
        if (count <= complete) {
            println("All Pages converted")
            channel.close()
        }
    }
}