package com.intellij.tasks.azure

import com.intellij.tasks.CustomTaskState

/**
 * The state of a single [AzureWorkItem] in Azure DevOps
 *
 * This type is serializable, and will be persisted to the workspace (and so needs to have a default constructor).
 */
data class AzureWorkItemState(
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
        /** Creates a [AzureWorkItemState] from the given string */
        fun fromString(id: String) = AzureWorkItemState(id, id)
    }
}