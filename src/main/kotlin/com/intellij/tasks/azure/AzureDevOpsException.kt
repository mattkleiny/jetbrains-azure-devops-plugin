package com.intellij.tasks.azure

/**
 * Indicates that an error occurred while interacting with the [AzureDevOpsClient]
 *
 * The [reason] field is a human-readable string that is mainly useful for debugging in the editor.
 */
class AzureDevOpsException(message: String, val reason: String) : Exception(message)
