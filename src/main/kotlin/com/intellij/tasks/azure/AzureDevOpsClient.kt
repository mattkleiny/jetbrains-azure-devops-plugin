package com.intellij.tasks.azure

import kotlinx.coroutines.future.await
import java.io.Closeable
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers
import java.time.Duration

/**
 * A client wrapper for interacting with the Azure DevOps REST API.
 *
 * This is a thin wrapper around the HTTP client, and is responsible for handling authentication and serialization.
 */
class AzureDevOpsClient(teamId: String, projectId: String, accessToken: String) : AutoCloseable {
    private val encodedTeamId = teamId.toUrlEncoded()
    private val encodedProjectId = projectId.toUrlEncoded()
    private val encodedToken = ":$accessToken".toBase64()

    private val baseUri = URI("https://dev.azure.com/$encodedTeamId/")

    private val client: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    /** Pings the API and makes sure that we're available, and we can get a response */
    suspend fun ping() {
        // query for projects since they're the most lightweight and available to everyone
        execute("_apis/projects?api-version=2.0")
    }

    /**
     * Queries all [AzureWorkItem]s from Azure DevOps for the current user using the given [query] string.
     * If [includeClosed] is true, then include items that have been closed or resolved in the past.
     */
    suspend fun queryWorkItems(query: String? = null, includeClosed: Boolean = false): Array<AzureWorkItem> {
        val queryOp = QueryOp(
            buildString {
                append("SELECT [System.Id], [System.Title], [System.State] FROM WorkItems")
                if (query.isNullOrBlank()) {
                    append(" WHERE [System.AssignedTo] = @Me")
                } else {
                    append(" WHERE [System.Title] CONTAINS \'$query\'")
                    append(" AND [System.AssignedTo] = @Me")
                }
                if (!includeClosed) {
                    append(" AND [State] <> \'Closed\'")
                    append(" AND [State] <> \'Removed\'")
                }
                append(" ORDER BY [System.CreatedDate] desc")
            }
        )

        val response = execute("_apis/wit/wiql/?api-version=7.2-preview.2") {
            it.header("Content-Type", "application/json")
            it.method("POST", HttpRequest.BodyPublishers.ofString(queryOp.toJson()))
        }

        if (response.statusCode() == 404) {
            return emptyArray();
        }

        // fetch entire set of work items as a single batch call
        val workItemIds = response.asJson()["workItems"].map { it["id"].asText() }

        return getWorkItems(workItemIds)
    }

    /** Attempts to get a [AzureWorkItem] from Azure DevOps by its ID. If it doesn't exist null is returned. */
    suspend fun getWorkItem(workId: String): AzureWorkItem? {
        val response = execute("$encodedProjectId/_apis/wit/workitems/$workId/?api-version=7.2-preview.3")
        if (response.statusCode() == 404) {
            return null;
        }

        return AzureWorkItem.fromJson(response.asJson())
    }

    /** Fetches a set of work items at the same time, based on ids */
    suspend fun getWorkItems(workIds: List<String>): Array<AzureWorkItem> {
        val response = execute("$encodedProjectId/_apis/wit/workitems/?ids=${workIds.joinToString(",")}&api-version=7.2-preview.3")
        if (response.statusCode() == 404) {
            return emptyArray();
        }

        return response.asJson()["value"].map { AzureWorkItem.fromJson(it) }.toTypedArray()
    }

    /** Sets the state of a work item if and only if the [revision] matches. */
    suspend fun setWorkItemState(workId: String, revision: Int, state: String): Boolean {
        val mutation = arrayOf(
            MutationOp("test", "/rev", revision),
            MutationOp("replace", "/fields/System.State", state)
        ).toJson()

        val result = execute("$encodedProjectId/_apis/wit/workitems/$workId?api-version=7.2-preview.3") {
            it.header("Content-Type", "application/json-patch+json")
            it.method("PATCH", HttpRequest.BodyPublishers.ofString(mutation))
        }

        return result.statusCode() != 404
    }

    /** Gets all possible [AzureWorkItemState]s for the given [AzureWorkItem.type] */
    suspend fun getWorkItemStates(type: String): Set<AzureWorkItemState> {
        val response = execute("$encodedProjectId/_apis/wit/workitemtypes/${type.toUrlEncoded()}/states?api-version=7.2-preview.1")
        if (response.statusCode() == 404) {
            return emptySet();
        }

        return response.asJson()["value"].map { AzureWorkItemState.fromJson(it) }.toSet()
    }

    /** Sets the state of a work item, ignoring the revision of the item */
    suspend fun setWorkItemState(workId: String, state: String): Boolean {
        val mutation = arrayOf(
            MutationOp("replace", "/fields/System.State", state)
        ).toJson()

        val result = execute("$encodedProjectId/_apis/wit/workitems/$workId?api-version=7.2-preview.3") {
            it.header("Content-Type", "application/json-patch+json")
            it.method("PATCH", HttpRequest.BodyPublishers.ofString(mutation))
        }

        return result.statusCode() != 404
    }

    /**
     * Executes a request against the Azure DevOps API.
     *
     * If a [builder] is provided, it will be called with the request builder before the request is sent.
     * Use this to customize the request.
     *
     * If the response is not 'valid' an [AzureDevOpsException] will be thrown.
     */
    private suspend fun execute(
        relativeUrl: String,
        builder: (HttpRequest.Builder) -> HttpRequest.Builder = { it }
    ): HttpResponse<String> {
        val request = builder(
            HttpRequest
                .newBuilder(baseUri.resolve(relativeUrl))
                .header("Accept", "application/json")
                .header("Authorization", "Basic $encodedToken")
        ).build()

        val response = client.sendAsync(request, BodyHandlers.ofString()).await()
        val statusCode = response.statusCode()

        if (statusCode != 200 && statusCode != 201 && statusCode != 404) {
            throw AzureDevOpsException("An unexpected status code was returned: $statusCode", response.body())
        }

        return response
    }


    override fun close() {
        if (client is Closeable) {
            client.close()
        }
    }

    private data class QueryOp(val query: String)
    private data class MutationOp(val op: String, val path: String, val value: Any)
}

