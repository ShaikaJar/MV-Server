package data

import java.net.URL
import java.util.Locale

data class Video (val quality:Quality, val url:String, val sizeInBytes: Long?, val locale: Locale, val mimeType: String)