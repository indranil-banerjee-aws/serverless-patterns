package com.amazonaws.services.lambda.samples.events.msk;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.lambda.powertools.kafka.Deserialization;
import software.amazon.lambda.powertools.kafka.DeserializationType;
import software.amazon.lambda.powertools.logging.Logging;

public class AvroKafkaHandler implements RequestHandler<ConsumerRecords<String, GenericRecord>, String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AvroKafkaHandler.class);

    @Override
    @Logging
    @Deserialization(type = DeserializationType.KAFKA_AVRO)
    public String handleRequest(ConsumerRecords<String, GenericRecord> records, Context context) {
        for (ConsumerRecord<String, GenericRecord> record : records) {
            // Key remains as string (if present)
            String key = record.key();
            if (key != null) {
                LOGGER.info("Message key: {}", key);
            } else {
            	LOGGER.error("Key is null");
            }

            // Value is deserialized as JSON
            GenericRecord decodedGenericPerson = record.value();
            if (null != decodedGenericPerson) {
            	LOGGER.info("###################################");
            	LOGGER.info("Received message: value = " + decodedGenericPerson);
            	LOGGER.info("Firstname = " + decodedGenericPerson.get("firstname"));
            	LOGGER.info("Lastname = " + decodedGenericPerson.get("lastname"));
            	LOGGER.info("Company = " + decodedGenericPerson.get("company"));
            	LOGGER.info("Street = " + decodedGenericPerson.get("street"));
            	LOGGER.info("City = " + decodedGenericPerson.get("city"));
            	LOGGER.info("County = " + decodedGenericPerson.get("county"));
            	LOGGER.info("State = " + decodedGenericPerson.get("state"));
            	LOGGER.info("Zip = " + decodedGenericPerson.get("zip"));
            	LOGGER.info("HomePhone = " + decodedGenericPerson.get("homePhone"));
            	LOGGER.info("CellPhone = " + decodedGenericPerson.get("cellPhone"));
            	LOGGER.info("Email = " + decodedGenericPerson.get("email"));
            	LOGGER.info("Website = " + decodedGenericPerson.get("website"));
            	LOGGER.info("###################################");
            } else {
            	LOGGER.error("Person Object is null");
            }
        }
        return "OK";
    }
}