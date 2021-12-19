package com.ir.ap89;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.ir.ap89.model.Document;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import co.elastic.clients.base.BooleanResponse;
import co.elastic.clients.base.RestClientTransport;
import co.elastic.clients.base.Transport;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._core.IndexRequest;
import co.elastic.clients.elasticsearch._core.IndexResponse;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch.indices.CreateRequest;
import co.elastic.clients.elasticsearch.indices.CreateResponse;
import co.elastic.clients.elasticsearch.indices.DeleteRequest;
import co.elastic.clients.elasticsearch.indices.DeleteResponse;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;

/**
 * This class index AP89 documents into a cloud ElasticSearch instance
 * by doing the following in order:
 * 1. Clean up existing indexes generated from previous runs.
 * 2. Create a new index with custom stopwords provided in the text file.
 * 3. Transform original input files into valid XML files.
 * 4. Read documents from XML files.
 * 5. Upload documents to the ES instance for indexing.
 */
public class Indexer {
    private static final Logger LOGGER = Logger.getLogger(Indexer.class.getName());

    private static final String DATA_COLLECTION_PATH = "data/ap89_collection";
    private static final String INPUT_FILE_NAME_PREFIX = "ap89";
    private static final String INPUT_XML_FILE_NAME_SUFFIX = ".xml";
    private static final String XML_DOCUMENT_TAG_NAME = "DOC";

    private static final String STOPWORDS_FILE_PATH = "data/stoplist.txt";

    private static final String INDEX_NAME = "ap89-index";
    private static final String HOST_NAME = "ir-projects.es.us-central1.gcp.cloud.es.io";
    private static final int PORT_NUMBER = 9243;
    private static final String USER_NAME = "elastic";
    private static final String PASSWORD = "sYCAq2PuJqsEUyKH4EW7rMzS";

    private final ElasticsearchClient client;

    public Indexer() {
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, 
            new UsernamePasswordCredentials(USER_NAME, PASSWORD));
        RestClient restClient = RestClient.builder(
            new HttpHost(HOST_NAME, PORT_NUMBER, "https"))
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                    .setDefaultCredentialsProvider(credentialsProvider)).build();
        JacksonJsonpMapper jsonMapper = new JacksonJsonpMapper();
        Transport transport = new RestClientTransport(restClient, jsonMapper);
        this.client = new ElasticsearchClient(transport);
    }
        
    /**
     * Delete the index generated from previous runs if it exists.
     * @return true when clean-up was successful.
     */
    public boolean cleanUp() {
        try {
            if (indexExists()) {
                DeleteRequest deleteRequest = new DeleteRequest.Builder().index(INDEX_NAME).build();
                DeleteResponse deleteResponse = client.indices().delete(deleteRequest);
                LOGGER.info("Delete index response: " + deleteResponse);
            }
            return true;
        } catch (IOException e) {
            LOGGER.severe("Failed to clean up existing data. Error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Index all the Youtube comments stored in the CSV dataset.
     */
    public void indexDocuments() {
        transformToXMLFiles(true);
        List<Document> documents = readDocuments();
        LOGGER.info(String.format("Start indexing %d documents in total.", documents.size()));
        int indexedDocumentCount = 0;
        for (Document document : documents) {
            IndexRequest<Document> request = new IndexRequest.Builder<Document>()
                .index(INDEX_NAME)
                .document(document)
                .build();

            try {
                IndexResponse response = client.index(request);
                if (response.result() == Result.Created) {
                    indexedDocumentCount++;
                    LOGGER.info(String.format("Finished indexing document %s. Id: %s", document.docNo, response.id()));
                    LOGGER.info(String.format("Indexed %d documents so far.", indexedDocumentCount));
                } else {
                    LOGGER.warning(String.format("Failed to index comment %s. Response: %s", document.docNo, response.toString()));
                }
            } catch (IOException e) {
                LOGGER.warning(String.format("Failed to index comment %s. Error: %s", document.docNo, e.getMessage()));
            }
        }
        LOGGER.info(String.format("Finished indexing %d of %d comments in total.", indexedDocumentCount, documents.size()));
    }


    /**
     * Check whether there is an existing index.
     * @return true when the index exists.
     */
    private boolean indexExists() {
        try {
            ExistsRequest existsRequest = new ExistsRequest.Builder().index(INDEX_NAME).build();
            BooleanResponse existsResponse = client.indices().exists(existsRequest);
            return existsResponse.value();
        } catch (IOException e) {
            LOGGER.severe("Failed to check whether the index exists. Error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Create a new index with a custom analyzer.
     * @return true when index creation was successful.
     */
    private boolean createIndex() {
        try {
            // Set up a standard analyzer with default stop words.
            Map<String, JsonData> settings = new HashMap<>();
            JsonArrayBuilder stopwordsBuilder = Json.createArrayBuilder();
            for (String stopword : readStopwords()) {
                stopwordsBuilder.add(stopword);
            }
            JsonObject analysis = Json.createObjectBuilder()
                .add("analyzer", Json.createObjectBuilder()
                    .add("standard_with_custom_stopwords", Json.createObjectBuilder()
                        .add("tokenizer", "standard")
                        .add("filter", Json.createArrayBuilder()
                            .add("custom_stopwords_filter").build()
                        ).build()
                    ).build()
                ).add("filter", Json.createObjectBuilder()
                    .add("custom_stopwords_filter", Json.createObjectBuilder()
                        .add("type", "stop")
                        .add("ignore_case", true)
                        .add("stopwords", stopwordsBuilder.build())
                    ).build()
                ).build();
            settings.put("analysis", JsonData.of(analysis));
            CreateRequest createRequest = new CreateRequest.Builder().index(INDEX_NAME).settings(settings).build();
            CreateResponse createResponse = client.indices().create(createRequest);
            LOGGER.info("Create index response: " + createResponse);
            return true;
        } catch (IOException e) {
            LOGGER.severe("Failed to create new index. Error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Read stopwords from the text file.
     * @return a list of stopwords.
     */
    private List<String> readStopwords() {
        List<String> stopwords = new ArrayList<>();
        try (
            BufferedReader br = new BufferedReader(new FileReader(new File(STOPWORDS_FILE_PATH)));
        ) {
            String line;
            while ((line = br.readLine()) != null) {
                stopwords.add(line);
            }
        } catch (IOException e) {
            LOGGER.warning(String.format("Caught exception when reading stopword file: %s", e.getMessage()));
        }

        return stopwords;
    }

    /**
     * Read all AP89 documents in XML format from the data collection files.
     * @return a list of all documents.
     */
    private List<Document> readDocuments() {
        File dataCollectionFolder = new File(DATA_COLLECTION_PATH);
        File[] dataFiles = dataCollectionFolder.listFiles();
        dataFiles = Arrays.stream(dataFiles).filter( 
            // Skip non input files and original non-XML input files.
            f -> f.getName().startsWith(INPUT_FILE_NAME_PREFIX) && f.getName().endsWith(INPUT_XML_FILE_NAME_SUFFIX)
        ).toArray(File[]::new);
        Arrays.sort(dataFiles, (f1, f2) -> f1.getName().compareTo(f2.getName()));
        List<Document> documents = new ArrayList<>();
        
        for (int i = 0; i < dataFiles.length; i++) {
            System.out.println("Reading documents from file " + dataFiles[i].getName());

            try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                DocumentBuilder db = dbf.newDocumentBuilder();
                org.w3c.dom.Document doc = db.parse(new FileInputStream(dataFiles[i]));
                doc.getDocumentElement().normalize();

                NodeList docList = doc.getElementsByTagName(XML_DOCUMENT_TAG_NAME);
                int documentCount = 0;
                for (int j = 0; j < docList.getLength(); j++) {
                    Node node = docList.item(j);
                    if (node.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }
                    Element record = (Element) node;
                    Document document = Document.parseFromXML(record);
                    documents.add(document);
                    documentCount++;
                }

                System.out.println(String.format("Read %d documents from %d nodes.", documentCount, docList.getLength()));
            } catch (IOException | ParserConfigurationException | SAXException e) {
                LOGGER.warning(String.format("Caught exception when reading document: %s", e.getMessage()));
            }
        }

        return documents;
    }

    /**
     * Transform all original AP89 documents into XML format.
     * @param skipExistingFiles whether to skip overwriting existing XML files generated from previsou runs.
     */
    private void transformToXMLFiles(boolean skipExistingFiles) {
        File dataCollectionFolder = new File(DATA_COLLECTION_PATH);
        File[] dataFiles = dataCollectionFolder.listFiles();
        dataFiles = Arrays.stream(dataFiles).filter( 
            // Skip non input files and already generated XML input files.
            f -> f.getName().startsWith(INPUT_FILE_NAME_PREFIX) && !f.getName().endsWith(INPUT_XML_FILE_NAME_SUFFIX)
        ).toArray(File[]::new);
        Arrays.sort(dataFiles, (f1, f2) -> f1.getName().compareTo(f2.getName()));
        
        for (int i = 0; i < dataFiles.length; i++) {
            System.out.println("Adding ROOT tag to file: " + dataFiles[i].getName());

            File newFile = new File(dataFiles[i].getAbsolutePath() + INPUT_XML_FILE_NAME_SUFFIX);
            try {
                if (!newFile.createNewFile()) {
                    if (skipExistingFiles) {
                        LOGGER.warning("Output XML file already exists. Skipping.");
                        continue;
                    }
                }
            } catch (IOException e) {
                LOGGER.warning(String.format("Caught exception when creating output XML file: %s", e.getMessage()));
                continue;
            }

            try (
                BufferedReader br = new BufferedReader(new FileReader(dataFiles[i]));
                BufferedWriter bw = new BufferedWriter(new FileWriter(newFile));
            ) {
                String line = br.readLine();
                if (line == null) {
                    continue;
                }
                // An XML file does now allow the same tag to occur multiple times at root level.
                // Hence a <ROOT> tag is added here to include all <DOC> tags.
                boolean hasRootTag = line.equals("<ROOT>");
                if (!hasRootTag) {
                    bw.write("<ROOT>");
                    bw.newLine();
                }
                bw.write(line);
                bw.newLine();
                while ((line = br.readLine()) != null) {
                    bw.write(line);
                    bw.newLine();
                }
                if (!hasRootTag) {
                    bw.write("</ROOT>");
                    bw.newLine();
                }
                bw.flush();
            } catch (IOException e) {
                LOGGER.warning(String.format("Caught exception when generating output XML file: %s", e.getMessage()));
            }
        }
    }

    public static void main( String[] args ) {
        Indexer indexer = new Indexer();
        
        if (!indexer.cleanUp()) {
            System.exit(-1);
        }

        if (!indexer.createIndex()) {
            System.exit(-1);
        }
        
        indexer.indexDocuments();

        System.exit(0);
    }
}
