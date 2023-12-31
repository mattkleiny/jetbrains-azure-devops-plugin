package com.intellij.tasks.azure

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.tasks.CustomTaskState
import com.intellij.tasks.Task
import com.intellij.tasks.TaskRepository
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.Transient
import kotlinx.coroutines.runBlocking

/**
 * Defines a [TaskRepository] for Azure DevOps.
 *
 * This is the main component that will broker calls to the API and define settings that
 * the user wishes to persist between sessions.
 *
 * This type is serializable, and will be persisted to the workspace (and so needs to have a default constructor).
 */
@Tag("AzureDevOps")
class AzureDevOpsRepository : TaskRepository {
    @Suppress("unused")
    constructor() : super()

    constructor(type: AzureDevOpsRepositoryType) : super(type)

    constructor(other: AzureDevOpsRepository) : super(other) {
        teamId = other.teamId
        projectId = other.projectId
        accessToken = other.accessToken
        preferredOpenTaskState = other.preferredOpenTaskState
        preferredCloseTaskState = other.preferredCloseTaskState
    }

    /** The ID of the team in Azure DevOps that we want to connect to */
    var teamId: String? = null

    /** The ID of the project in Azure DevOps that we want to connect to */
    var projectId: String? = null

    /** The Personal Access Token (PAT) of the user in Azure DevOps */
    // we persist this item via the [PasswordSafe] API instead of in the workspace files
    var accessToken: String?
        @Transient
        get() {
            val passwordSafe = PasswordSafe.instance
            val credentials = passwordSafe.get(credentialAttributes)

            return credentials?.getPasswordAsString()
        }
        set(value) {
            val passwordSafe = PasswordSafe.instance
            val credentials = passwordSafe.get(credentialAttributes)

            passwordSafe.set(credentialAttributes, Credentials(credentials?.userName, value))
        }

    /** The preferred state to set a [Task] to when it is closed */
    @JvmField
    var preferredCloseTaskState: CustomTaskState? = null

    /** The preferred state to set a [Task] to when it is opened */
    @JvmField
    var preferredOpenTaskState: CustomTaskState? = null

    /** Generates the [CredentialAttributes] for the attribute store. */
    private val credentialAttributes
        get() = CredentialAttributes(
            userName = "$teamId/$projectId",
            serviceName = generateServiceName(
                subsystem = "Tasks",
                key = "${repositoryType.name} ${getUrl()}"
            )
        )

    /**
     * Executes some code with an [AzureDevOpsClient] and closes it when done.
     * Automatically runs the code in a [runBlocking] closure since the UI for it runs in a discrete thread.
     */
    private fun <T> withClient(body: suspend (AzureDevOpsClient) -> T): T {
        if (!isConfigured) {
            throw IllegalStateException("The repository is not fully configured")
        }

        AzureDevOpsClient(teamId!!, projectId!!, accessToken!!).use {
            try {
                return runBlocking { body(it) }
            } catch (exception: InterruptedException) {
                // translate cancellations into IDE-specific ignores
                throw ProcessCanceledException(exception)
            }
        }
    }

    /** Determines if the repository is configured with the required settings */
    override fun isConfigured() =
        !teamId.isNullOrEmpty() && !projectId.isNullOrEmpty() && !accessToken.isNullOrEmpty()

    override fun getUrl() =
        "https://dev.azure.com/${teamId?.toUrlEncoded() ?: "<team-id>"}/${projectId?.toUrlEncoded() ?: "<project-id>"}/"

    override fun extractId(taskName: String) =
        taskName.substringBefore(':')

    override fun getIssues(
        query: String?,
        offset: Int,
        limit: Int,
        withClosed: Boolean,
        cancelled: ProgressIndicator
    ): Array<Task> {
        return withClient {
            it.queryWorkItems(query, withClosed).map { workItem ->
                // remember which repository we came from and encode a user-friendly URL for the task,
                // so the user can go straight to the appropriate page in Azure DevOps
                workItem.repository = this
                workItem.issueUrl = url + "_workitems/edit/${workItem.id}"
                workItem
            }.toTypedArray()
        }
    }

    override fun findTask(id: String): Task? {
        return withClient {
            val workItem = it.getWorkItem(id)

            if (workItem != null) {
                // remember which repository we came from and encode a user-friendly URL for the task,
                // so the user can go straight to the appropriate page in Azure DevOps
                workItem.repository = this
                workItem.issueUrl = url + "_workitems/edit/$id"
            }

            workItem
        }
    }

    /** A cache of [CustomTaskState]s for each work item type */
    private val workItemStateCache: MutableMap<String, Set<CustomTaskState>> = mutableMapOf()

    override fun getAvailableTaskStates(task: Task): MutableSet<CustomTaskState> {
        // we need to know the type of the work item, so we'll have to re-query here; IntelliJ doesn't cache it
        return withClient {
            val workItem = it.getWorkItem(task.id)
            if (workItem == null) {
                mutableSetOf()
            } else {
                workItemStateCache.getOrPut(workItem.type) { it.getWorkItemStates(workItem.type) }.toMutableSet()
            }
        }
    }

    override fun setPreferredOpenTaskState(state: CustomTaskState?) {
        preferredOpenTaskState = state
    }

    override fun getPreferredOpenTaskState(): CustomTaskState? {
        return preferredOpenTaskState
    }

    override fun setPreferredCloseTaskState(state: CustomTaskState?) {
        preferredCloseTaskState = state
    }

    override fun getPreferredCloseTaskState(): CustomTaskState? {
        return preferredCloseTaskState
    }

    override fun setTaskState(task: Task, state: CustomTaskState) {
        withClient {
            it.setWorkItemState(task.id, state.id)
        }
    }

    override fun getFeatures(): Int {
        // we allow searching tasks using a small string query, and updating tasks
        return NATIVE_SEARCH or STATE_UPDATING
    }

    override fun createCancellableConnection(): CancellableConnection {
        if (!isConfigured) {
            throw IllegalStateException("Repository is not configured")
        }

        // proxy test connections through the client
        return object : CancellableConnection() {
            private var isCancelled = false
            private val client = AzureDevOpsClient(teamId!!, projectId!!, accessToken!!)

            override fun doTest() {
                runBlocking {
                    client.ping()
                    if (!isCancelled) {
                        client.close()
                    }
                }
            }

            override fun cancel() {
                client.close()
                isCancelled = true
            }
        }
    }

    override fun clone() = AzureDevOpsRepository(this)
}
