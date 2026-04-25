package com.vortexai.android.domain.comfy.provider

import com.vortexai.android.domain.comfy.v2.CanonicalGraph

/**
 * Template provider for Serverless Cloud ComfyUI APIs (e.g., RunComfy, NanoBanana).
 * Will be expanded in future cloud-specific tickets to handle Auth Headers and S3 image staging.
 */
class CloudComfyProvider(
    private val endpointUrl: String,
    private val bearerToken: String
) : ComfyUIProvider {

    override suspend fun uploadImage(imageBytes: ByteArray): String {
        // TODO: In cloud deployments, images are typically uploaded to an intermediate S3 
        // bucket or ImgBB, and the resulting public URL is passed to a custom "LoadImageUrl" node
        // instead of the native "LoadImage" node.
        throw NotImplementedError("Cloud Image Staging is not yet implemented.")
    }

    override suspend fun executeGeneration(graph: CanonicalGraph): String {
        // TODO: Map to specific Cloud provider's Serverless Inference API POST hook.
        throw NotImplementedError("Cloud Serverless Execution is not yet implemented.")
    }
}
