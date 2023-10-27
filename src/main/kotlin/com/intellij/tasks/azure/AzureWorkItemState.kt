package com.intellij.tasks.azure

import com.fasterxml.jackson.databind.JsonNode
import com.intellij.tasks.CustomTaskState

/**
 * The state of a single [AzureWorkItem] in Azure DevOps
 *
 * This type is serializable, and will be persisted to the workspace (and so needs to have a default constructor).
 */
class AzureWorkItemState(
    @JvmField val id: String,
    @JvmField val state: String,
) : CustomTaskState(id, state) {
    /** For serialization */
    @Suppress("unused")
    constructor() : this("", "")

    override fun getId() = id
    override fun getPresentableName() = state

    fun isClosed() = state.equals("closed", true) || state.equals("resolved", true)

    companion object {
        fun fromString(id: String) = AzureWorkItemState(id, id)
        fun fromJson(json: JsonNode) = fromString(json["name"].asText())
    }
}