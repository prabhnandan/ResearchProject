# Schedule

Framework and utilities for task scheduling.

## Install

Download the jar file from the [latest release](https://github.com/UOA-PARC-scheduling/schedule/releases/latest).

## Build

If you want to build this project from source, follow these instructions.

This project is configured to be built by [Maven](https://maven.apache.org/download.cgi). Once Maven is installed:

1. If you have not already done so, clone or download the repository to your local machine.
2. Set up authentication with the GitHub package repository:
    - Find your `{user.home}/.m2/` folder. On Windows, `{user.home}` is usually `C:\Users\<username>`. If you have just installed Maven, the `.m2/` folder may not exist. You can create it yourself.
    - Copy the file `settings.xml` to your `{user.home}/.m2/` folder. Edit the file to replace
the `username` and `password` fields. `username` should be your GitHub username. For `password` you will need to generate a [personal access token](https://help.github.com/en/enterprise/2.17/user/github/authenticating-to-github/creating-a-personal-access-token-for-the-command-line) with at least these permissions - `repo, read:packages`. (If you plan to deploy a new package version, you will also need the `write:packages` permission.) **Do not commit a settings.xml file containing an access token.**
3. From the root folder of your local copy of the repository (where pom.xml is located), use Maven commands to build. The files will be created in the `target/` directory. These operations will likely take some time when first run:
    - To compile, use `mvn compile`.
    - To create a jar, use `mvn package`.
    - To create a jar containing all dependencies, use `mvn clean compile assembly:single`.
    
## Deploy Maven Package

If you wish to deploy a new version of the project as a Maven package to the GitHub Packages repository (i.e. when creating a new release), follow these steps:

1. Edit `pom.xml`, finding the `<version></version>` tag and updating its contents to an appropriate value.
3. Run 'mvn deploy' in the folder containing `pom.xml`.

*Make sure your GitHub access token has the `write:packages` permission.*

The package should be published to `https://maven.pkg.github.com/UOA-PARC-scheduling/schedule`. This address is specified in the `<distributionManagement>` section of `pom.xml`.
