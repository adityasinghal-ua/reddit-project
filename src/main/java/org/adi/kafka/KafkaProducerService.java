package org.adi.kafka;

import jakarta.enterprise.context.ApplicationScoped;
import org.adi.config.Constants;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Properties;

@ApplicationScoped
public class KafkaProducerService {
    private KafkaProducer<String, String> producer;

    // constructor to initialize the KafkaProducer
    public KafkaProducerService(
            @ConfigProperty(name = "kafka.bootstrap.servers") String kafkaBootstrapServers
    ){
        Properties properties = new Properties();
        properties.put("bootstrap.servers", kafkaBootstrapServers);      // specifies kafka broker's network location
        properties.put("key.serializer", StringSerializer.class.getName());     // specifies how key and value should be serialized while converting to Bytes
        properties.put("value.serializer", StringSerializer.class.getName());
        properties.put("acks", "all");    // highest level of message durability and safety, introduces slight latency but ensures integrity (trade-off)

        this.producer = new KafkaProducer<>(properties);
    }

    public void sendMessage(String topic, String key, String value){
        // this creates a message that will be sent to a kafka topic
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);

        // used to send message using the KafkaProducer
        producer.send(record, (metadata, e) -> {
            if(e != null){
                e.printStackTrace();
            }else {
//                System.out.println("Message sent to topic: " + metadata.topic() + " partition " + metadata.partition());
            }
        });
    }

    public void close(){
        producer.close();
    }
}
