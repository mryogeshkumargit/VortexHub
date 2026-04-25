package com.vortexai.android.domain.comfy.v2

import android.util.Log

/**
 * Executes safe, rollback-capable mutations on a parsed Canonical Graph.
 * Relies on the roles inferred by [SemanticRoleInferencer] rather than hardcoded node IDs.
 */
object GraphTransformationEngine {
    private const val TAG = "GraphTransformationEngine"

    /**
     * Finds the primary text encoder role and injects the user prompt.
     */
    fun injectPrompt(graph: CanonicalGraph, text: String, isNegative: Boolean = false) {
        val targetRole = if (isNegative) NodeRole.TEXT_ENCODER_SECONDARY else NodeRole.TEXT_ENCODER_PRIMARY
        
        // Find nodes matching the role
        val matches = graph.nodes.values.filter { it.role == targetRole }
        
        if (matches.isEmpty()) {
            throw IllegalStateException("Graph Transformation Failed: Could not locate a node with role $targetRole.")
        }

        matches.forEach { node ->
            // Different encoders label their text parameter differently
            // Ex: CLIPTextEncode -> "text"
            // SDXL Encoder -> "text_l", "text_g"
            
            // First try updating existing "text" keys
            var injected = false
            node.parameters.keys.toList().forEach { key ->
                if (key.lowercase().contains("text")) {
                    node.parameters[key] = text
                    injected = true
                    Log.d(TAG, "Injected ${targetRole.name} text into Parameter '$key' on Node ${node.id}")
                }
            }
            
            // Fallback (e.g. they provided a template with an empty JSON object for inputs)
            if (!injected) {
                node.parameters["text"] = text
                Log.d(TAG, "Fallback: Appended 'text' parameter on Node ${node.id}")
            }
        }
    }

    /**
     * Injects an uploaded filename into the IMAGE_INPUT node.
     */
    fun injectImage(graph: CanonicalGraph, filename: String) {
        val matches = graph.nodes.values.filter { it.role == NodeRole.IMAGE_INPUT }
        matches.forEach { node ->
            node.parameters["image"] = filename
            Log.d(TAG, "Injected Image filename into IMAGE_INPUT (Node ${node.id})")
        }
    }

    /**
     * Injects a random seed target into the core sampler.
     */
    fun injectSeed(graph: CanonicalGraph, seed: Long) {
        val match = graph.nodes.values.find { it.role == NodeRole.SAMPLER_CORE }
        if (match != null) {
            // "noise_seed" is common for KSamplerAdvanced, "seed" is common for standard KSampler
            if (match.parameters.containsKey("seed")) {
                match.parameters["seed"] = seed
                Log.d(TAG, "Injected random seed into SAMPLER_CORE (Node ${match.id} - 'seed')")
            } else {
                match.parameters["noise_seed"] = seed
                Log.d(TAG, "Injected random seed into SAMPLER_CORE (Node ${match.id} - 'noise_seed')")
            }
        } else {
            // In Flux workflows, RandomNoise encapsulates the seed, not the sampler.
            // Let's fallback to searching dynamically.
            val noiseNode = graph.nodes.values.find { it.type == "RandomNoise" }
            if (noiseNode != null) {
                noiseNode.parameters["noise_seed"] = seed
                Log.d(TAG, "Injected random seed into RandomNoise (Node ${noiseNode.id})")
            } else {
                Log.w(TAG, "Failed to find a SAMPLER_CORE or RandomNoise to inject seed.")
            }
        }
    }

    /**
     * Injects the designated Checkpoint model name into the MODEL_LOADER.
     * Overrides whichever default checkpoint was saved in the workflow template.
     */
    fun injectCheckpoint(graph: CanonicalGraph, checkpointName: String) {
        if (checkpointName.isBlank()) return
        
        val loaders = graph.nodes.values.filter { it.role == NodeRole.MODEL_LOADER }
        
        val unetLoaders = loaders.filter { it.parameters.containsKey("unet_name") }
        val checkLoaders = loaders.filter { it.parameters.containsKey("ckpt_name") }
        
        // In advanced Flux architectures, graphs often have BOTH a UNETLoader (for the primary model)
        // AND a CheckpointLoaderSimple (often used as a generic fallback or to load the VAE/CLIP).
        // If a UNETLoader exists, it's virtually guaranteed the user intends to swap the UNET model.
        if (unetLoaders.isNotEmpty()) {
            unetLoaders.forEach { node ->
                node.parameters["unet_name"] = checkpointName
                Log.d(TAG, "Injected Override '$checkpointName' into UNET_LOADER (Node ${node.id})")
            }
        } else {
            // Fallback: If no UNETLoader is found, assume the primary inference is via CheckpointLoaderSimple
            checkLoaders.forEach { node ->
                node.parameters["ckpt_name"] = checkpointName
                Log.d(TAG, "Injected Override '$checkpointName' into CHECKPOINT_LOADER (Node ${node.id})")
            }
        }
    }

    /**
     * Safely mutates the graph topology by sandwiching a new LoraLoader node 
     * between the MODEL_LOADER and whatever consumes it.
     */
    fun injectLora(graph: CanonicalGraph, loraName: String, weight: Float) {
        val loader = graph.nodes.values.find { it.role == NodeRole.MODEL_LOADER } 
            ?: throw IllegalStateException("Cannot inject LoRA: Missing MODEL_LOADER in graph.")

        // 1) Find the highest node ID in the graph to assign a new safe ID
        val currentMaxId = graph.nodes.keys.mapNotNull { it.toIntOrNull() }.maxOrNull() ?: 9000
        val loraNodeId = (currentMaxId + 1).toString()

        // 2) Create the Canonical Lora node
        val loraNode = CanonicalNode(
            id = loraNodeId,
            type = "LoraLoader",
            role = NodeRole.LORA_INJECTION_POINT
        )
        loraNode.parameters["lora_name"] = loraName
        loraNode.parameters["strength_model"] = weight
        loraNode.parameters["strength_clip"] = weight

        // 3) Find all Edges that emanate FROM the MODEL_LOADER
        val edgesFromLoader = graph.edges.filter { it.fromNode == loader.id }
        
        var modelPortIndex = 0
        var clipPortIndex = 1

        // 4) Rewire the graph safely
        // Any edge extending from the loader must now extend from the Lora.
        val edgesToRewire = edgesFromLoader.toMutableList()
        
        edgesToRewire.forEach { edgeToRewire ->
            // Disconnect original edge
            graph.edges.remove(edgeToRewire)
            
            // Reconnect the consumer to point to the new LoRA node instead
            val rewiredEdge = edgeToRewire.copy(
                fromNode = loraNodeId,
                // LoraLoader outputs MODEL as 0, CLIP as 1 regardless of upstream index
                fromPortIndex = if (edgeToRewire.fromPortIndex == modelPortIndex) 0 else 1 
            )
            graph.edges.add(rewiredEdge)
        }

        // 5) Connect the new LoRA node to the original MODEL_LOADER
        val loraModelInEdge = Edge(fromNode = loader.id, fromPortIndex = modelPortIndex, toNode = loraNodeId, toPortName = "model")
        val loraClipInEdge = Edge(fromNode = loader.id, fromPortIndex = clipPortIndex, toNode = loraNodeId, toPortName = "clip")
        
        graph.edges.add(loraModelInEdge)
        graph.edges.add(loraClipInEdge)
        
        // Finalize node insertion
        graph.nodes[loraNodeId] = loraNode
        
        Log.d(TAG, "Safely transformed graph: Sandwiched LoraLoader($loraName) under Node ${loader.id}")
    }
}
