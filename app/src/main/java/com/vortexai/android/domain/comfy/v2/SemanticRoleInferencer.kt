package com.vortexai.android.domain.comfy.v2

import android.util.Log

/**
 * Deduces the semantic purpose of nodes within a given CanonicalGraph.
 * This decoupled inference engine allows Vortex to support hundreds of custom
 * ComfyUI extensions without needing hardcoded mapping lists.
 */
object SemanticRoleInferencer {
    private const val TAG = "SemanticRoleInferencer"

    /**
     * Executes a heuristic pass over the un-mutated graph to tag nodes with [NodeRole]s.
     */
    fun inferRoles(graph: CanonicalGraph) {
        // 1. Tag anchor nodes first (if workflows provide explicit vortex_anchor meta)
        // ... (Reserved for custom workflow metadata injections later)

        // 2. Pass 1: Simple Type Matching Heuristics
        graph.nodes.values.forEach { node ->
            if (node.role != null) return@forEach // skip if already tagged

            val classType = node.type.lowercase()

            when {
                // MODEL LOADERS
                classType.contains("checkpointloadersimple") || classType.contains("unetloader") -> {
                    node.role = NodeRole.MODEL_LOADER
                }
                // SAMPLERS
                classType.contains("ksampler") || classType.contains("samplercustom") -> {
                    node.role = NodeRole.SAMPLER_CORE
                }
                // TEXT ENCODERS
                classType.contains("cliptextencode") || classType.contains("effnettextencode") -> {
                    // Check tags for negative/positive hints
                    val tagsLower = node.tags.joinToString(" ").lowercase()
                    if (tagsLower.contains("negative") || tagsLower.contains("neg")) {
                        node.role = NodeRole.TEXT_ENCODER_SECONDARY
                    } else {
                        // Default to primary. If multiple primaries exist, distance-to-sampler will break ties later.
                        node.role = NodeRole.TEXT_ENCODER_PRIMARY
                    }
                }
                // IMAGE LOADERS
                classType.contains("loadimage") -> {
                    node.role = NodeRole.IMAGE_INPUT
                }
                // IMAGE OUTPUT / SAVING
                classType.contains("saveimage") || classType.contains("previewimage") -> {
                    node.role = NodeRole.IMAGE_OUTPUT
                }
                // LATENT
                classType.contains("emptylatentimage") -> {
                    node.role = NodeRole.LATENT_INITIALIZER
                }
            }
        }

        // 3. Pass 2: Contextual Heuristics (Topological Analysis)
        // If we have multiple text encoders but no explicit "Negative" tagged,
        // we can deduce role by inspecting what port they plug into on the KSampler or BasicGuider.
        resolveTextEncoderAmbiguities(graph)

        // Log results
        val taggedCount = graph.nodes.values.count { it.role != null }
        Log.d(TAG, "Inferred roles for $taggedCount / ${graph.nodes.size} nodes.")
    }

    private fun resolveTextEncoderAmbiguities(graph: CanonicalGraph) {
        val primaries = graph.nodes.values.filter { it.role == NodeRole.TEXT_ENCODER_PRIMARY }
        if (primaries.size > 1) {
            // Find which one connects to a negative tracking edge
            Log.d(TAG, "Multiple TEXT_ENCODER_PRIMARY nodes detected. Running contextual resolution...")
            
            primaries.forEach { encoder ->
                // Find edges projecting AWAY from this encoder
                val outgoingEdges = graph.edges.filter { it.fromNode == encoder.id }
                outgoingEdges.forEach { edge ->
                    // E.g., plugs into KSampler's 'negative' port
                    if (edge.toPortName.lowercase().contains("negative")) {
                        encoder.role = NodeRole.TEXT_ENCODER_SECONDARY
                        Log.d(TAG, "Re-classified node ${encoder.id} as SECONDARY based on downstream KSampler port names.")
                    }
                }
            }
        }
    }
}
