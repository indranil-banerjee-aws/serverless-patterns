package com.amazonaws.services.lambda.samples.events.msk;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.lambda.powertools.kafka.Deserialization;
import software.amazon.lambda.powertools.kafka.DeserializationType;
import software.amazon.lambda.powertools.logging.Logging;

public class AvroKafkaHandler implements RequestHandler<ConsumerRecords<String, Person>, String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AvroKafkaHandler.class);

    @Override
    @Logging
    @Deserialization(type = DeserializationType.KAFKA_JSON)
    public String handleRequest(ConsumerRecords<String, Person> records, Context context) {
        for (ConsumerRecord<String, Person> record : records) {
            // Key remains as string (if present)
            String key = record.key();
            if (key != null) {
                LOGGER.info("Message key: {}", key);
            } else {
            	LOGGER.error("Key is null");
            }

            // Value is deserialized as JSON
            Person person = record.value();
            if (null != person) {
            	LOGGER.info("Firstname {} - Email: ${}", person.getFirstname(), person.getEmail());
            } else {
            	LOGGER.error("Person Object is null");
            }
        }
        return "OK";
    }
}