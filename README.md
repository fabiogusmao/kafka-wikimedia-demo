# Kafka Wikimedia Demo
Sample application inspired by the LinkedIn Learning course [Complete Guide to Apache Kafka for Beginners](https://www.linkedin.com/learning/complete-guide-to-apache-kafka-for-beginners). I wrote this after taking the course and am happy to share it with you.

## Overview

Here is a high level orverview of what this sample does:

- **Wikimedia Producer:** connects to the [Wikimedia Recent Changes Stream](https://stream.wikimedia.org/v2/stream/recentchange) to get the latest changes to all Wikimedia sites and dumps these changes to an Apache Kafka stream.
- **Wikimedia Consumer:** reads from the Apache Kafka stream and saves records into an OpenSearch database.

This is basically the same architecture used in the course, I've just improved a few things:
- **I provide a Dockerfile for all the dependencies:** so there is no need to install Kafka locally on WSL or anything. Just spin the [/docker-compose.yml](./docker-compose.yml) file and you're good to go!
- **I've added kafbat-ui:** this is a lightweight Kafka UI for you to view and manage cluster, topics etc.
- **OpenSearch 3 and Dashboards are included:** these are updated versions of OpenSearch.
- **Updated Java libraries:** these are the most recent versions at the time of writing.

## Prerequisites
- [Docker](https://www.docker.com/)
- [JDK](https://adoptium.net/)
- [Visual Studio Code](https://code.visualstudio.com/)
    - [Extension Pack for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack): this includes all the extensions you need for Java development
    - [Docker extensions](https://marketplace.visualstudio.com/items?itemName=ms-azuretools.vscode-docker)

## Installing Kafka and OpenSearch
Open your Terminal on this repository's root folder and type:
`docker-compose up`
This is the same as opening the [docker-compose.yml](./docker-compose.yml) file on VS Code and clicking on *Run All Services*.

This will create a container group with the Kafka, OpenSearch and their respective management tools. Make sure to check on Docker Desktop's user interface wheither these are running properly before trying to run the producer or the consumer.

## Running
After you install all the prerequisites and run the necessary Docker containers, open each project ([wikimedia-producer](./wikimedia-producer/) on VSCode and [wikimedia-consumer](./wikimedia-consumer/) and do the following:
### Restore Gradle packages
Open the Terminal (from menu *View -> Terminal*) and type the following command
- On Windows: `.\gradlew.bat`
- On Linux and MacOS: `./gradlew`

### Start Debugging
Just press `F5` or via the menu *Debug -> Start Debugging*. Launch both applications.

### Stopping the Programs
When you are done, press Ctrl+C (or Command+C on the Mac) to interrupt the program and gracefuly stop debugging.