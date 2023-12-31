package com.intellij.tasks.azure

import com.fasterxml.jackson.databind.JsonNode
import com.intellij.tasks.Comment
import com.intellij.tasks.Task
import com.intellij.tasks.TaskState
import com.intellij.tasks.TaskType
import java.util.*

/**
 * A [Task] representing a work item in Azure DevOps.
 *
 * This is fetched from the API and deserialized from JSON, and then kept
 * in memory by IntelliJ. A variant of this will be stored locally that
 * references the task id, and that data will be re-fetched from the API
 * when IntelliJ needs to query the data.
 */
class AzureWorkItem(
    @JvmField val id: String,
    @JvmField val summary: String,
    @JvmField val description: String?,
    @JvmField val state: AzureWorkItemState?,
    @JvmField val type: String,
    @JvmField val updated: Date?,
    @JvmField val created: Date?,
    @JvmField var issueUrl: String? = null,
    @JvmField var repository: AzureDevOpsRepository? = null
) : Task() {
    override fun getId() = id
    override fun getSummary() = summary
    override fun getDescription() = description
    override fun getComments() = emptyArray<Comment>()
    override fun getIcon() = AzureDevOpsIcons.logo
    override fun getUpdated() = updated
    override fun getCreated() = created
    override fun isClosed() = state?.isClosed() ?: false
    override fun isIssue() = issueUrl != null
    override fun getIssueUrl() = issueUrl
    override fun getRepository() = repository

    override fun getType() = when (type) {
        "Bug" -> TaskType.BUG
        "Defect" -> TaskType.BUG
        "Story" -> TaskType.OTHER
        "Task" -> TaskType.OTHER
        "Feature" -> TaskType.FEATURE
        "Epic" -> TaskType.FEATURE
        else -> TaskType.OTHER
    }

    companion object {
        fun fromJson(json: JsonNode) = AzureWorkItem(
            id = json["id"].asText(),
            summary = json["fields"]["System.Title"].asText(),
            description = json["fields"]["System.Description"]?.asText(),
            state = AzureWorkItemState.fromString(json["fields"]["System.State"].asText()),
            type = json["fields"]["System.WorkItemType"].asText(),
            updated = json["fields"]["System.ChangedDate"].asDate(),
            created = json["fields"]["System.CreatedDate"].asDate()
        )
    }
}