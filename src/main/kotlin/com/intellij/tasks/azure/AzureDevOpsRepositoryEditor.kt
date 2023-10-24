package com.intellij.tasks.azure

import com.intellij.tasks.config.TaskRepositoryEditor
import com.intellij.ui.components.JBLabel
import com.intellij.util.Consumer
import com.intellij.util.ui.FormBuilder
import com.intellij.tasks.azure.AzureDevOpsRepositoryEditor.SimpleDocumentListener
import javax.swing.JComponent
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * A custom [TaskRepositoryEditor] for the Azure DevOps task repository settings.
 *
 * This is used to define the UI for the settings page, and to handle the events
 * and changes to those settings, replicating them back to the [AzureDevOpsRepository].
 */
class AzureDevOpsRepositoryEditor(
    private val repository: AzureDevOpsRepository,
    private val changeListener: Consumer<in AzureDevOpsRepository>
) : TaskRepositoryEditor() {
    private lateinit var teamIdText: JTextField
    private lateinit var projectIdText: JTextField
    private lateinit var accessTokenText: JTextField

    override fun createComponent(): JComponent {
        teamIdText = JTextField().also {
            it.text = repository.teamId
            it.toolTipText = "The ID of the Team in Azure DevOps that you want to connect to"
            it.document.addDocumentListener(SimpleDocumentListener {
                repository.teamId = teamIdText.text
                changeListener.consume(repository)
            })
        }
        projectIdText = JTextField().also {
            it.text = repository.projectId
            it.toolTipText = "The ID of the project in Azure DevOps that you want to connect to"
            it.document.addDocumentListener(SimpleDocumentListener {
                repository.projectId = projectIdText.text
                changeListener.consume(repository)
            })
        }
        accessTokenText = JTextField().also {
            it.text = repository.accessToken
            it.toolTipText = "The Personal Access Token (PAT) of your user in Azure DevOps"
            it.document.addDocumentListener(SimpleDocumentListener {
                repository.accessToken = accessTokenText.text
                changeListener.consume(repository)
            })
        }

        val myTeamIdLabel = JBLabel("Team ID:").also {
            it.labelFor = teamIdText
        }
        val myProjectIdLabel = JBLabel("Project ID:").also {
            it.labelFor = projectIdText
        }
        val myAccessTokenLabel = JBLabel("Access token:").also {
            it.labelFor = accessTokenText
        }

        return FormBuilder.createFormBuilder()
            .addLabeledComponent(myTeamIdLabel, teamIdText)
            .addLabeledComponent(myProjectIdLabel, projectIdText)
            .addLabeledComponent(myAccessTokenLabel, accessTokenText)
            .panel
    }

    /** Listens for changes in the text fields and invokes a delegate when they do. */
    fun interface SimpleDocumentListener : DocumentListener {
        fun onUpdate(e: DocumentEvent?)

        override fun insertUpdate(e: DocumentEvent?) = onUpdate(e)
        override fun removeUpdate(e: DocumentEvent?) = onUpdate(e)
        override fun changedUpdate(e: DocumentEvent?) = onUpdate(e)
    }
}
