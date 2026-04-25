package com.vortexai.android.ui.screens.settings.managers

import com.vortexai.android.ui.screens.settings.ModelInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelCacheManager @Inject constructor() {
    private val modelsByProvider = mutableMapOf<String, List<ModelInfo>>()
    private val imageModelsByProvider = mutableMapOf<String, List<String>>()

    fun getCachedModels(provider: String): List<ModelInfo>? {
        return modelsByProvider[provider]
    }

    fun setCachedModels(provider: String, models: List<ModelInfo>) {
        modelsByProvider[provider] = models
    }

    fun getCachedImageModels(provider: String): List<String>? {
        return imageModelsByProvider[provider]
    }

    fun setCachedImageModels(provider: String, models: List<String>) {
        imageModelsByProvider[provider] = models
    }

    fun clearModelCache(provider: String? = null) {
        if (provider != null) {
            modelsByProvider.remove(provider)
        } else {
            modelsByProvider.clear()
        }
    }

    fun clearImageModelCache(provider: String? = null) {
        if (provider != null) {
            imageModelsByProvider.remove(provider)
        } else {
            imageModelsByProvider.clear()
        }
    }

    fun clearAllCaches() {
        modelsByProvider.clear()
        imageModelsByProvider.clear()
    }

    fun getAllModelsByProvider(): Map<String, List<ModelInfo>> {
        return modelsByProvider.toMap()
    }

    fun getAllImageModelsByProvider(): Map<String, List<String>> {
        return imageModelsByProvider.toMap()
    }
}