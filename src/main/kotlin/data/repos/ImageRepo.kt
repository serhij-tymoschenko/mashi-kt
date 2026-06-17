package com.mashiverse.data.repos

import com.mashiverse.configs.LAYER_ORDER
import com.mashiverse.configs.animSemaphore
import com.mashiverse.configs.compositeSemaphore
import com.mashiverse.data.db.daos.ImageDao
import com.mashiverse.data.models.Asset
import com.mashiverse.data.models.Colors
import com.mashiverse.data.models.ImageType
import com.mashiverse.data.models.Mashup
import com.mashiverse.data.remote.apis.IpfsApi
import com.mashiverse.images.helpers.*
import com.mashiverse.images.playwright.combiners.AnimCombiner
import com.mashiverse.images.playwright.combiners.CompositeCombiner
import com.mashiverse.utils.helpers.readFile
import com.mashiverse.utils.helpers.rmDir
import com.mashiverse.utils.helpers.writeFile
import data.models.DownloadType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

class ImageRepo : KoinComponent {
    private val animCombiner by inject<AnimCombiner>()
    private val compositeCombiner by inject<CompositeCombiner>()
    private val imageDao by inject<ImageDao>()
    private val ipfsApi by inject<IpfsApi>()

    private suspend fun getAsset(asset: Asset, colors: Colors): Pair<String, ByteArray>? {
        return withContext(Dispatchers.IO) {
            return@withContext try {
                val name = asset.name.lowercase()
                val url = asset.image

                var data = imageDao.getImage(url)
                if (data == null) {
                    data = ipfsApi.getImageSrc(url) ?: return@withContext null
                    val imageType = getImageType(data)
                    if (imageType != ImageType.UNKNOWN) {
                        imageDao.addImage(url, data)
                    }
                }

                val imageType = getImageType(data)
                if (imageType == ImageType.SVG) {
                    data = replaceColors(
                        data = data, body = colors.base, eyes = colors.eyes, hair = colors.hair
                    )
                }
                Pair(name, data)
            } catch (e: Exception) {
                print(e.message)
                null
            }
        }
    }

    suspend fun getImage(
        mashup: Mashup, downloadType: DownloadType = DownloadType.PNG, mintedName: String? = null
    ): ByteArray? {
        return withContext(Dispatchers.IO) {
            val tempDir = Paths.get(System.getProperty("java.io.tmpdir")).resolve("mashi-temp")
            Files.createDirectories(tempDir)

            val uniqueDir = tempDir.resolve(UUID.randomUUID().toString())
            Files.createDirectories(uniqueDir)

            try {
                val assets = mashup.traits
                val colors = mashup.colors

                if (assets.isEmpty()) {
                    return@withContext null
                }

                val assetJobs = assets.map { asset ->
                    async { getAsset(asset, colors) }
                }

                val srcs = assetJobs.awaitAll().filterNotNull().toMap()

                val traits = LAYER_ORDER.mapNotNull { name -> srcs[name] }.toMutableList()

                if (!mintedName.isNullOrEmpty()) {
                    val mintedTrait = getMintedTrait(mintedName)
                    traits.add(mintedTrait)
                }

                traits.forEachIndexed { index, bytes ->
                    val mime = getMime(bytes)
                    val b64 = Base64.getEncoder().encodeToString(bytes)
                    val filePath = uniqueDir.resolve(index.toString())
                    val fileContent = "data:$mime;base64,$b64".toByteArray(Charsets.UTF_8)
                    writeFile(filePath, fileContent)
                }

                val imagePath: Path = if (downloadType == DownloadType.PNG) {
                    compositeSemaphore.withPermit {
                        compositeCombiner.generateComposite(uniqueDir)
                    }
                } else {
                    val maxT = getMaxDuration(traits)
                    animSemaphore.withPermit {
                        animCombiner.generateAnim(uniqueDir, maxT)
                    }
                }

                return@withContext readFile(imagePath)
            } catch (e: Exception) {
                return@withContext null
            } finally {
                rmDir(uniqueDir)
            }
        }
    }
}