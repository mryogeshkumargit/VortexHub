package com.vortexai.android.domain.comfy.provider

import com.vortexai.android.domain.comfy.v2.CanonicalGraph

/**
 * Strategy interface for executing Canonical Graphs.
 * Allows Vortex to seamlessly swap between a Local host PC (direct WebSocket/HTTP)
 * and a Cloud Serverless API (Auto-scaling, Bearer Auth, Storage staging)
 * without rewriting the core Generation Services.
 */
interface ComfyUIProvider {
    
    /**
     * Uploads a base64 or raw byte image to the execution environment.
     * @return The remote filename/URI that can be injected into the LoadImage node.
     */
    suspend fun uploadImage(imageBytes: ByteArray): String

    /**
     * Executes the workflow and blocks/polls until completion.
     * @param graph The fully validated graph ready for JSON compilation and execution.
     * @return The final generated image URL or Base64 string.
     */
    suspend fun executeGeneration(graph: CanonicalGraph): String
}
