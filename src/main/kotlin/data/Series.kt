package data

import java.util.HashSet
import java.util.UUID

data class Series(
    val name: String,
    val tags: Set<String>,
    val service: StreamingService,
) {
    var shows: HashSet<Show> = HashSet<Show>()

    fun addShow(show: Show) {
        shows.add(show)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Series

        if (name != other.name) return false
        if (tags != other.tags) return false
        if (service != other.service) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + tags.hashCode()
        result = 31 * result + service.hashCode()
        return result
    }


}