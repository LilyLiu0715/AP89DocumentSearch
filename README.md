# AP89 Document Search - CS 6200 Course Project User Guide

## Project Structure

This project has two components (see design in the project report):

* Index pipeline, implemented as a Java Maven project.
* Search frontend, implemented as a Spring Boot project.

Github repository link: <https://github.com/LilyLiu0715/AP89DocumentSearch>

Web app url: <https://ap89documentsearch.uc.r.appspot.com/>

### Indexer Pipeline

```bash
ap89-indexer
├── data/
│    ├── stoplist.txt   
│    ├── ap89_collection   
│        ├── ap89* (original input files)
│        ├── ap89*.xml (transformed XML input files)
│        ├── readme
├── pom.xml
├── src
│    ├── main/java/com/ir/ap89
│        ├── Indexer.java/
│        ├── model/
│               ├── Document.java
├── target
     ├── ap89-indexer-1.0-SNAPSHOT-jar-with-dependencies.jar
```

### Search Frontend

```bash
ap89-search-frontend
├── pom.xml
├── src
│    ├── main/
│        ├── appengine/
│        │   ├── app.yaml
│        ├── java/
│        │   ├── com/ir/ap89
│        |       ├── model/
│        |       |      ├── Document.java
│        |       |      ├── SearchResult.java
│        |       ├── ap89searchfrontend/
│        |              ├── Searcher.java
│        |              ├── SearchController.java
│        |              ├── Ap89SearchFrontendApplication.java
│        ├── resources/
│            ├── application.properties 
│            ├── static/
│                 ├── index.html
│            ├── templates/
│                 ├── search.html
├── target
     ├── ap89-search-frontend-0.0.1-SNAPSHOT.jar
```

## Deployment & Testing

### Running Indexer Pipeline

The index pipeline only needs to run once locally to index all the document into the cloud ElasticSearch instance, which has already been done.
Subsequent runs will overwrite the previous data and hence are no-op.

Note that input data is not included in the github repository due to its large volume. To run the pipeline, put the document files `ap89*` under `ap89-indexer/data/ap89_collection` and `stoplist.txt` under `ap89-indexer/data` like the [Project Structure](#indexer-pipeline) shows.

### Running Search Frontend

The search frontend is already deployed on Google Cloud Platform. To access the web application, go the the following url:

<https://ap89documentsearch.uc.r.appspot.com/>

Note that the cloud ElasticSearch instance is during the free trial period, which will expire on 12/29/2021. Please let me know if access after this date is needed.
