package wikimedia.producer;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.sse.EventSources;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

public class App {
    private static String BOOTSTRAP_SERVERS = "localhost:9092";
    private static String TOPIC = "wikimedia.recentchange";
    private static String STREAM_URL = "https://stream.wikimedia.org/v2/stream/recentchange";

    private static final Logger _logger = LoggerFactory.getLogger(App.class.getSimpleName());

    public static void main(String[] args) throws InterruptedException {

        _logger.info("Welcome to Wikimedia Producer!");

        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", BOOTSTRAP_SERVERS);
        properties.setProperty("key.serializer", StringSerializer.class.getName());
        properties.setProperty("value.serializer", StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        properties.setProperty(ProducerConfig.LINGER_MS_CONFIG, "500");
        properties.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, Integer.toString(32 * 1024));

        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);

        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();

        Request request = new Request.Builder()
                .url(STREAM_URL)
                .header("User-Agent", "wikimedia-producer")
                .build();

        EventSource eventSource = EventSources.createFactory(client).newEventSource(request, new EventSourceListener() {
            @Override
            public void onOpen(EventSource eventSource, okhttp3.Response response) {
                _logger.info("Connection opened");
            }

            @Override
            public void onEvent(EventSource eventSource, String id, String type, String data) {
                ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, null, data);
                producer.send(record, (metadata, exception) -> {
                    if (exception != null) {
                        _logger.error("Error while producing", exception);
                    }
                });
            }

            @Override
            public void onClosed(EventSource eventSource) {
                _logger.info("Connection closed");
            }

            @Override
            public void onFailure(EventSource eventSource, Throwable t, Response response) {
                if (t instanceof IOException && t.getMessage().equalsIgnoreCase("canceled")) {
                    return;
                }
                _logger.error("Error in event source", t);
            }
        });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            _logger.info("Stopping application...");
            eventSource.cancel();
            producer.flush();
            producer.close();

            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
            
            _logger.info("Application stopped.");
        }));
    }
}
