package wikimedia.consumer;

import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.hc.core5.http.HttpHost;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class App {
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String TOPIC = "wikimedia.recentchange";
    private static final String OPENSEARCH_HOST = "http://localhost:9200";

    private static final Logger _logger = LoggerFactory.getLogger(App.class.getSimpleName());
    private static final Gson gson = new Gson();

    public static void main(String[] args) throws URISyntaxException, JsonSyntaxException, InterruptedException {
        _logger.info("Welcome to Wikimedia Producer!");

        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "wikimedia-consumer");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);
        consumer.subscribe(Collections.singleton(TOPIC));

        OpenSearchClient openSearchClient = createOpenSearchClient();
        createIndex(openSearchClient, "wikimedia-changes");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            _logger.info("Stopping application...");
            consumer.close();
            _logger.info("Application stopped.");
        }));

        while (true) {
            _logger.info("Polling...");
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
            if (records.count() == 0) {
                _logger.info("No records found.");
                try {
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException e) {
                    break;
                }
            } else {
                int inserted = 0;
                for (var record : records) {
                    String json = record.value();
                    Map<String, Object> document = gson.fromJson(json, Map.class);
                    Map<String, Object> meta = (Map<String, Object>) document.get("meta");
                    String id = meta.get("id").toString();

                    try {
                        openSearchClient.index(i -> i
                                .index("wikimedia-changes")
                                .id(id)
                                .document(document));
                        inserted++;
                    } catch (Exception e) {
                        _logger.error("Error inserting record with id " + id + ": " + e.getMessage(), e);
                    }                    
                }

                _logger.info("Inserted " + inserted + " of " + records.count() + " records.");                
                TimeUnit.SECONDS.sleep(1);
            }
        }
    }

    private static OpenSearchClient createOpenSearchClient() throws URISyntaxException {

        HttpHost[] hosts = new HttpHost[] { HttpHost.create(OPENSEARCH_HOST) };

        OpenSearchTransport transport = ApacheHttpClient5TransportBuilder.builder(hosts)
                .setMapper(new JacksonJsonpMapper())
                .build();
        return new OpenSearchClient(transport);
    }

    private static void createIndex(OpenSearchClient openSearchClient, String indexName) {
        try {
            boolean indexExists = openSearchClient.indices().exists(e -> e.index(indexName)).value();
            if (!indexExists) {
                openSearchClient.indices().create(c -> c.index(indexName));
                _logger.info("Index " + indexName + " created.");
            } else {
                _logger.info("Index " + indexName + " already exists.");
            }
        } catch (Exception e) {
            _logger.error("Error creating index: " + e.getMessage(), e);
        }
    }
}