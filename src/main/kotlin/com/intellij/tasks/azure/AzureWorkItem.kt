package com.intellij.tasks.azure

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import com.intellij.tasks.Comment
import com.intellij.tasks.Task
import com.intellij.tasks.TaskType
import java.util.*
import javax.swing.Icon

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
    @JvmField @JsonIgnore val icon: Icon?,
    @JvmField val type: TaskType,
    @JvmField val updated: Date?,
    @JvmField val created: Date?,
    @JvmField var issueUrl: String? = null,
    @JvmField val revision: Int,
    @JvmField var repository: AzureDevOpsRepository? = null
) : Task() {
    override fun getId() = id
    override fun getSummary() = summary
    override fun getDescription() = description
    override fun getComments() = emptyArray<Comment>()
    override fun getIcon() = icon ?: AzureDevOpsIcons.logo
    override fun getType() = type
    override fun getUpdated() = updated
    override fun getCreated() = created
    override fun isClosed() = state?.isClosed() ?: false
    override fun isIssue() = issueUrl != null
    override fun getIssueUrl() = issueUrl
    override fun getRepository() = repository

    companion object {
        /** Attempts to parse a [AzureWorkItem] from the given JSON */
        fun fromJson(json: JsonNode): AzureWorkItem {
            val id = json["id"].asText()
            val summary = json["fields"]["System.Title"].asText()
            val description = json["fields"]["System.Description"]?.asText()
            val state = AzureWorkItemState.fromString(json["fields"]["System.State"].asText())
            val updated = json["fields"]["System.ChangedDate"].asDate()
            val created = json["fields"]["System.CreatedDate"].asDate()
            val revision = json["rev"].asInt()

            val type = json["fields"]["System.WorkItemType"].asText().let {
                when (it) {
                    "Bug" -> TaskType.BUG
                    "Defect" -> TaskType.BUG
                    "Story" -> TaskType.OTHER
                    "Task" -> TaskType.OTHER
                    "Feature" -> TaskType.FEATURE
                    "Epic" -> TaskType.FEATURE
                    else -> TaskType.OTHER
                }
            }

            return AzureWorkItem(
                id = id,
                summary = summary,
                description = description,
                state = state,
                icon = null,
                type = type,
                updated = updated,
                created = created,
                revision = revision
            )
        }
    }
}