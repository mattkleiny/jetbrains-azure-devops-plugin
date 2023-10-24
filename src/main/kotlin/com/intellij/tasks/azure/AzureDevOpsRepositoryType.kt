package com.intellij.tasks.azure

import com.intellij.openapi.project.Project
import com.intellij.tasks.TaskRepositoryType
import com.intellij.tasks.config.TaskRepositoryEditor
import com.intellij.util.Consumer

/**
 * An integration point that defines a new kind of [com.intellij.tasks.TaskRepository].
 *
 * This is the entry point to the plugin, and allows us to inject new functionality into the editor.
 */
class AzureDevOpsRepositoryType : TaskRepositoryType<AzureDevOpsRepository>() {
    override fun getName() = "Azure DevOps"
    override fun getIcon() = AzureDevOpsIcons.logo
    override fun createRepository() = AzureDevOpsRepository(this)
    override fun getRepositoryClass() = AzureDevOpsRepository::class.java

    override fun createEditor(
        repository: AzureDevOpsRepository,
        project: Project,
        changeListener: Consumer<in AzureDevOpsRepository>
    ): TaskRepositoryEditor {
        return AzureDevOpsRepositoryEditor(repository, changeListener)
    }
}
