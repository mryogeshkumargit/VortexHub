package com.vortexai.android.domain.comfy.v2

import android.util.Log

data class ValidationResult(val isValid: Boolean, val reasons: List<String>)

/**
 * Validates a CanonicalGraph before converting it back to JSON.
 * Ensures the mutations didn't leave dangling edges or miss required core components.
 */
object GraphValidator {
    private const val TAG = "GraphValidator"

    fun validate(graph: CanonicalGraph): ValidationResult {
        val violations = mutableListOf<String>()

        Log.d(TAG, "Running Pre-Flight Graph Validation Pass...")

        // 1. Structural Checks: Dangling Edges
        graph.edges.forEach { edge ->
            if (!graph.nodes.containsKey(edge.fromNode)) {
                violations.add("Dangling Edge Source: Edge originating from Node ${edge.fromNode} points to non-existent node.")
            }
            if (!graph.nodes.containsKey(edge.toNode)) {
                violations.add("Dangling Edge Target: Edge pointing to Node ${edge.toNode} references a non-existent node.")
            }
        }

        // 2. Role Existence Guarantees
        val rolesPresent = graph.nodes.values.mapNotNull { it.role }.toSet()

        if (!rolesPresent.contains(NodeRole.TEXT_ENCODER_PRIMARY)) {
            violations.add("Missing Required Component: Graph must contain at least one TEXT_ENCODER_PRIMARY.")
        }

        if (!rolesPresent.contains(NodeRole.SAMPLER_CORE)) {
            // Edge case: Some graphs just encode images
            val hasLatentInit = rolesPresent.contains(NodeRole.LATENT_INITIALIZER)
            if (hasLatentInit) {
                violations.add("Missing Required Component: Graph has a Latent Generator but no SAMPLER_CORE.")
            }
        }

        if (violations.isEmpty()) {
            Log.d(TAG, "Validation Passed (0 violations).")
        } else {
            Log.e(TAG, "Validation Failed! (${violations.size} violations)")
            violations.forEach { Log.e(TAG, " - $it") }
        }

        return ValidationResult(isValid = violations.isEmpty(), reasons = violations)
    }
}
