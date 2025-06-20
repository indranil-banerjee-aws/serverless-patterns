//Lambda Runtime delivers a batch of messages to the lambda function
//Each batch of messages has two fields EventSource and EventSourceARN
//Each batch of messages also has a field called Records
//The Records is a map with multiple keys and values
//Each key is a combination of the Topic Name and the Partition Number
//One batch of messages can contain messages from multiple partitions

/*
To simplify representing a batch of Kafka messages as a list of messages
We have created a Java class called KafkaMessage under the models package
Here we are mapping the structure of an incoming Kafka event to a list of
objects of the KafkaMessage class
*/

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

public class HandlerMSK implements RequestHandler<ConsumerRecords<String, Person>, String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HandlerMSK.class);
    String dynamoDBTableName = System.getenv("DYNAMO_DB_TABLE");
    DynamoDBUpdater ddbUpdater = new DynamoDBUpdater(dynamoDBTableName);
    String AWS_SAM_LOCAL = System.getenv("AWS_SAM_LOCAL");
    
    @Override
    @Logging
    @Deserialization(type = DeserializationType.KAFKA_AVRO)
    public String handleRequest(ConsumerRecords<String, Person> records, Context context) {
    	String response = new String("200 OK");
        for (ConsumerRecord<String, Person> record : records) {
            Person person = record.value(); // Person class is auto-generated from Avro schema
            LOGGER.info("Processing person: {}, email {}", person.getFirstname().toString() + " " + person.getLastname().toString(), person.getEmail());
            KafkaMessage thisMessage = new KafkaMessage();
            thisMessage.key = record.key();
            thisMessage.offset = record.offset();
            thisMessage.partition = record.partition();
            thisMessage.timestamp = record.timestamp();
            thisMessage.timestampType = record.timestampType().name();
            thisMessage.topic = record.topic();
            thisMessage.value = record.value();
    		if (null == AWS_SAM_LOCAL) {
    			ddbUpdater.insertIntoDynamoDB(thisMessage);
    		} 
        }
        return response;
    }
}
