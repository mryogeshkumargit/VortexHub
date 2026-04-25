package com.vortexai.android.domain.comfy.v2

import org.json.JSONArray
import org.json.JSONObject

/**
 * Transforms between a Raw ComfyUI Execution JSON and the Internal Canonical Graph.
 * Ensures the runtime mutator engine only deals with strong types and isolates JSON structure details.
 */
object ComfyGraphBuilder {

    /**
     * Parses a raw ComfyUI API-format JSON array/object into a [CanonicalGraph].
     */
    fun parse(jsonString: String): CanonicalGraph {
        val root = JSONObject(jsonString)
        val graph = CanonicalGraph()

        root.keys().forEach { nodeId ->
            val nodeObject = root.optJSONObject(nodeId)
            if (nodeObject != null) {
                val type = nodeObject.optString("class_type", "Unknown")
                val node = CanonicalNode(
                    id = nodeId,
                    type = type
                )

                // Parse _meta data for tags
                val meta = nodeObject.optJSONObject("_meta")
                if (meta != null) {
                    val title = meta.optString("title")
                    if (title.isNotEmpty()) {
                        node.tags.add(title)
                    }
                }

                // Traverse inputs to deduce edges and extract static parameters
                val inputsObj = nodeObject.optJSONObject("inputs")
                if (inputsObj != null) {
                    inputsObj.keys().forEach { inputKey ->
                        val value = inputsObj.get(inputKey)
                        
                        // Detect ComfyUI Edge formats -> ["source_node_id", output_index]
                        if (value is JSONArray && value.length() >= 2) {
                            val sourceId = value.optString(0)
                            val sourcePortIndex = value.optInt(1)
                            
                            val edge = Edge(
                                fromNode = sourceId,
                                fromPortIndex = sourcePortIndex,
                                toNode = nodeId,
                                toPortName = inputKey
                            )
                            graph.edges.add(edge)
                        } else {
                            // Raw parameter value (Integer, Float, String, Boolean)
                            node.parameters[inputKey] = value
                        }
                    }
                }
                
                graph.nodes[nodeId] = node
            }
        }
        
        return graph
    }

    /**
     * Compiles the mutated canonical graph back into an execution-friendly ComfyUI JSON format.
     */
    fun compileToJson(graph: CanonicalGraph): String {
        val rootJson = JSONObject()

        graph.nodes.values.forEach { node ->
            val nodeJson = JSONObject()
            nodeJson.put("class_type", node.type)

            // Rebuild meta
            if (node.tags.isNotEmpty()) {
                val metaJson = JSONObject()
                // Pick the first tag as the title to mimic original UI
                metaJson.put("title", node.tags.first())
                nodeJson.put("_meta", metaJson)
            }

            // Rebuild inputs
            val inputsJson = JSONObject()
            
            // 1) Write static parameters
            node.parameters.forEach { (key, value) ->
                inputsJson.put(key, value)
            }
            
            // 2) Rebuild Edges pointing TO this node
            val incomingEdges = graph.edges.filter { it.toNode == node.id }
            incomingEdges.forEach { edge ->
                val edgeArray = JSONArray()
                edgeArray.put(edge.fromNode)
                edgeArray.put(edge.fromPortIndex)
                inputsJson.put(edge.toPortName, edgeArray)
            }
            
            nodeJson.put("inputs", inputsJson)
            
            rootJson.put(node.id, nodeJson)
        }

        return rootJson.toString()
    }
}
