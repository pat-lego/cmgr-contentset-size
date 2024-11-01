# AEM project

This is a project template for AEM-based applications. It is intended as a best-practice set of examples as well as a potential starting point to develop your own functionality.

## Modules

The main parts of the template are:

* [core:](core/README.md) Java bundle containing all core functionality like OSGi services, listeners or schedulers, as well as component-related Java code such as servlets or request filters.

## How to build

To build all the modules run in the project root directory the following command with Maven 3:

    mvn clean install

Or to deploy only the bundle to the author, run

    mvn clean install -PautoInstallBundle

## Documentation

`POST /cbf/contentset/size/` with a `com.adobe.aem.support.contentbackflow.entity.ContentSetInput` object this results in a Sling Job ID being returned

`GET /cbf/contentset/size/<sling_job_id>` returns `com.adobe.aem.support.contentbackflow.entity.ContentSetResult` object
- N.B. You will only see the result once the job completes. 
- N.B. If the job fails it will not appear in the API but in the logs as an error.

`GET /cbf/contentset/sizes?limit=X` returns a set of `com.adobe.aem.support.contentbackflow.entity.ContentSetResult` objects

### Unit tests

This show-cases classic unit testing of the code contained in the bundle. To
test, execute:

    mvn clean test

## Maven settings

The project comes with the auto-public repository configured. To setup the repository in your Maven settings, refer to:

    http://helpx.adobe.com/experience-manager/kb/SetUpTheAdobeMavenRepository.html
