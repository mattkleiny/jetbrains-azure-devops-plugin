[![Build and Publish](https://github.com/mattkleiny/jetbrains-azure-devops-plugin/actions/workflows/master-build.yml/badge.svg)](https://github.com/mattkleiny/jetbrains-azure-devops-plugin/actions/workflows/master-build.yml)
# JetBrains Azure DevOps plugin

This plugin enables [JetBrains](https://www.jetbrains.com/) IDEs (like Rider, DataGrip, etc) to access [Azure DevOps](https://azure.microsoft.com/en-au/products/devops)
for [Task Management](https://www.jetbrains.com/help/idea/managing-tasks-and-context.html).

![Example image](./docs/example.png)

## Installation

* Download the latest build from [GitHub actions](https://github.com/mattkleiny/jetbrains-azure-devops-plugin/actions)
* Install the [plugin from disk in your IDE](https://www.jetbrains.com/help/idea/managing-plugins.html#install_plugin_from_disk). All JetBrains IDEs are supported.
* Add a new server and select `Azure DevOps` as the server type. You can find the settings under `Tools -> Tasks -> Servers`.
  * Enter the Team ID from Azure DevOps.
  * Enter the Project Name from Azure DevOps.
  * Enter a [Personal Access Token](https://learn.microsoft.com/en-us/azure/devops/organizations/accounts/use-personal-access-tokens-to-authenticate?view=azure-devops&tabs=Windows) for Azure DevOps.
    * It needs the following permissions:
      * Project and Team: Read
      * Work Items: Read & Write
* Open the `Tasks` tool window, and it should now display Work Items that are assigned to you.
* You can now use all the standard [JetBrains Task Management features](https://www.jetbrains.com/help/idea/managing-tasks-and-context.html)