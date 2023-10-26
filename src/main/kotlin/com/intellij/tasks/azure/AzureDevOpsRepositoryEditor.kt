package com.intellij.tasks.azure

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.tasks.azure.AzureDevOpsRepositoryEditor.SimpleDocumentListener
import com.intellij.tasks.config.TaskRepositoryEditor
import com.intellij.ui.components.JBLabel
import com.intellij.util.Consumer
import com.intellij.util.ui.FormBuilder
import javax.swing.JCheckBox
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
    private lateinit var txtTeamId: JTextField
    private lateinit var txtProjectId: JTextField
    private lateinit var txtAccessToken: JTextField
    private lateinit var chkIsShared: JCheckBox
    private lateinit var chkFormatCommitMessage: JCheckBox
    private lateinit var docCommitMessage: Document
    private lateinit var txtCommitMessage: Editor

    override fun createComponent(): JComponent {
        txtTeamId = JTextField().also {
            it.text = repository.teamId
            it.toolTipText = "The ID of the Team in Azure DevOps that you want to connect to"
            it.document.addDocumentListener(SimpleDocumentListener {
                repository.teamId = txtTeamId.text
                changeListener.consume(repository)
            })
        }

        txtProjectId = JTextField().also {
            it.text = repository.projectId
            it.toolTipText = "The ID of the project in Azure DevOps that you want to connect to"
            it.document.addDocumentListener(SimpleDocumentListener {
                repository.projectId = txtProjectId.text
                changeListener.consume(repository)
            })
        }

        txtAccessToken = JTextField().also {
            it.text = repository.accessToken
            it.toolTipText = "The Personal Access Token (PAT) of your user in Azure DevOps"
            it.document.addDocumentListener(SimpleDocumentListener {
                repository.accessToken = txtAccessToken.text
                changeListener.consume(repository)
            })
        }

        chkIsShared = JCheckBox().also {
            it.isSelected = repository.isShared
            it.text = "Share with other team members"
            it.toolTipText = "Whether or not to share these settings with other team members"
            it.addActionListener {
                repository.isShared = chkIsShared.isSelected
                changeListener.consume(repository)
            }
        }

        chkFormatCommitMessage = JCheckBox().also {
            it.isSelected = repository.isShouldFormatCommitMessage
            it.text = "Format commit messages"
            it.toolTipText = "Whether or not to format commit messages"
            it.addActionListener {
                txtCommitMessage.component.isEnabled = chkFormatCommitMessage.isSelected
                repository.isShouldFormatCommitMessage = chkFormatCommitMessage.isSelected
                changeListener.consume(repository)
            }
        }

        docCommitMessage = EditorFactory.getInstance().createDocument(repository.commitMessageFormat)
        txtCommitMessage = EditorFactory.getInstance().createEditor(docCommitMessage).also {
            it.component.isEnabled = repository.isShouldFormatCommitMessage
            it.component.toolTipText = "The format of the commit message"
            docCommitMessage.addDocumentListener(EditorDocumentListener {
                repository.commitMessageFormat = docCommitMessage.text
                changeListener.consume(repository)
            })
        }

        val lblTeamId = JBLabel("Team ID:").also {
            it.labelFor = txtTeamId
        }
        val lblProjectId = JBLabel("Project ID:").also {
            it.labelFor = txtProjectId
        }
        val lblAccessToken = JBLabel("Access token:").also {
            it.labelFor = txtAccessToken
        }
        val lblCommitMessageFormat = JBLabel("Commit message:").also {
            it.labelFor = txtCommitMessage.component
        }

        return FormBuilder.createFormBuilder()
            .addLabeledComponent(lblTeamId, txtTeamId)
            .addLabeledComponent(lblProjectId, txtProjectId)
            .addLabeledComponent(lblAccessToken, txtAccessToken)
            .addLabeledComponent(lblCommitMessageFormat, txtCommitMessage.component)
            .addComponent(chkFormatCommitMessage)
            .addComponent(chkIsShared)
            .panel
    }

    /** Listens for changes in the text fields and invokes a delegate when they do. */
    fun interface SimpleDocumentListener : DocumentListener {
        fun onUpdate(e: DocumentEvent?)

        override fun insertUpdate(e: DocumentEvent?) = onUpdate(e)
        override fun removeUpdate(e: DocumentEvent?) = onUpdate(e)
        override fun changedUpdate(e: DocumentEvent?) = onUpdate(e)
    }

    /** Listens for changes in the text fields and invokes a delegate when they do. */
    fun interface EditorDocumentListener : com.intellij.openapi.editor.event.DocumentListener {
        fun onUpdate(e: com.intellij.openapi.editor.event.DocumentEvent)

        override fun documentChanged(event: com.intellij.openapi.editor.event.DocumentEvent) {
            onUpdate(event)
        }
    }
}
