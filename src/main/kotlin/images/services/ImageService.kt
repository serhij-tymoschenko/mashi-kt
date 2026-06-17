package images.services

import com.mashiverse.data.models.Mashup
import com.mashiverse.data.remote.apis.MashitApi
import com.mashiverse.data.repos.ImageRepo
import data.models.DownloadType
import data.models.mappers.toMashup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ImageService : KoinComponent {
    private val imageRepo by inject<ImageRepo>()

    suspend fun requestComposite(
        wallet: String? = null,
        mashup: Mashup? = null,
        downloadType: DownloadType,
        mintedName: String? = null
    ): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                val input = when {
                    wallet != null -> {
                        MashitApi().getMashup(wallet)?.toMashup()
                    }

                    mashup != null -> mashup
                    else -> return@withContext null
                }

                if (input == null) return@withContext null

                return@withContext imageRepo.getImage(
                    mashup = input,
                    downloadType = downloadType,
                    mintedName = mintedName
                )

            } catch (e: Exception) {
                println("❌ Balancer Error: ${e.message}")
                return@withContext null
            }
        }
    }
}
