package com.vortexai.android.domain.comfy.v2

/**
 * Represents the strict semantic roles a node can play in a ComfyUI generation pipeline.
 * By decoupling execution from hardcoded node types (e.g. CLIPTextEncode vs EffNetTextEncode),
 * the graph compiler can safely mutate workflows irrespective of custom community nodes.
 */
enum class NodeRole {
    TEXT_ENCODER_PRIMARY,
    TEXT_ENCODER_SECONDARY,
    LATENT_INITIALIZER,
    SAMPLER_CORE,
    MODEL_LOADER,
    LORA_INJECTION_POINT,
    IMAGE_INPUT,
    IMAGE_OUTPUT,
    UNKNOWN
}

/**
 * A strongly-typed connection port on a ComfyUI node.
 * For an output port, it defines data produced.
 * For an input port, it can define static parameters or a dynamic [Edge] connection.
 */
data class Port(
    val name: String,
    val type: String? = null // e.g., "MODEL", "CONDITIONING", "LATENT"
)

/**
 * A directed edge tracing data flow from one node's output port to another's input port.
 */
data class Edge(
    val fromNode: String,
    val fromPortIndex: Int,
    val toNode: String,
    val toPortName: String
)

/**
 * Represents the metadata associated with the workflow template itself.
 */
data class GraphMetadata(
    val templateId: String = "",
    val version: Int = 1,
    val architecture: String = "UNKNOWN"
)

/**
 * A unified internal representation of a ComfyUI JSON block operations.
 * 
 * @property id The unique ComfyUI node ID (e.g., "15").
 * @property type The internal class_type (e.g., "KSamplerAdvanced").
 * @property role The semantic role assigned by the SemanticRoleInferencer.
 * @property inputs Maps input port names to upstream graph connections (Edges) or raw static JSON elements.
 * @property outputs Emitted data ports.
 * @property parameters Static primitive values (String, Int, Float, Boolean) assigned to this node's inputs.
 * @property tags Tags parsed from `_meta.title` or V2 anchors.
 */
data class CanonicalNode(
    val id: String,
    val type: String,
    var role: NodeRole? = null,
    val inputs: MutableMap<String, Port> = mutableMapOf(),
    val outputs: MutableMap<String, Port> = mutableMapOf(),
    val parameters: MutableMap<String, Any> = mutableMapOf(),
    val tags: MutableSet<String> = mutableSetOf()
)

/**
 * The core isolated abstract representation of a generation workflow.
 * Transformations (like adding a LoRA) mutate this object state, which is later compiled back
 * into a ComfyUI execution JSON.
 */
data class CanonicalGraph(
    val nodes: MutableMap<String, CanonicalNode> = mutableMapOf(),
    val edges: MutableList<Edge> = mutableListOf(),
    var metadata: GraphMetadata = GraphMetadata()
)
