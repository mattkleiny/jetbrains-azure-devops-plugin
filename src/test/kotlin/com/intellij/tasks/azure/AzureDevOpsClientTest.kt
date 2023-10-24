package com.intellij.tasks.azure

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Test

@Ignore("These tests require a valid access token, run them manually")
class AzureDevOpsClientTest {
    private val client = AzureDevOpsClient(
        teamId = "",
        projectId = "",
        accessToken = ""
    )

    @Test
    fun `it should get tasks without error`() = runBlocking {
        val result = client.queryWorkItems(includeClosed = true)

        println(jacksonObjectMapper().writeValueAsString(result))
    }

    @Test
    fun `it should get a task without error`() = runBlocking {
        val result = client.getWorkItem("23362")

        println(jacksonObjectMapper().writeValueAsString(result))
    }

    @Test
    fun `it should set the state of a task without error`() = runBlocking {
        val result = client.setWorkItemState("26396", 5, "New")

        assert(result) { "Failed to set state of the work item" }
    }
}