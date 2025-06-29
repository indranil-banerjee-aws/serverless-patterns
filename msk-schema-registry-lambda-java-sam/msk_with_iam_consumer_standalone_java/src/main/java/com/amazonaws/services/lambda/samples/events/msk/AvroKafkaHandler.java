package com.amazonaws.services.lambda.samples.events.msk;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;


import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.glue.GlueClient;
import software.amazon.awssdk.services.glue.model.GetSchemaVersionRequest;
import software.amazon.awssdk.services.glue.model.GetSchemaVersionResponse;
import software.amazon.awssdk.services.glue.model.SchemaId;
import software.amazon.awssdk.services.glue.model.SchemaVersionNumber;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringSerializer;

import com.amazonaws.services.schemaregistry.deserializers.avro.AWSKafkaAvroDeserializer;
import com.amazonaws.services.schemaregistry.utils.AvroRecordType;
import com.amazonaws.services.schemaregistry.utils.AWSSchemaRegistryConstants;
import org.apache.kafka.common.errors.SerializationException;
import java.util.Collections;
import java.time.Duration;

public class AvroKafkaHandler {

	Properties prop;
	private static final Properties properties = new Properties();

	public static void main(String[] args) {
		String propertyFile = args[0];
		String kafkaTopic = args[1];

		Properties prop = null;
		try {
			prop = AvroKafkaHandler.readPropertiesFile(propertyFile);
		} catch (IOException e) {
			System.out.println("Please specify a valid Kafka Properties File as the first argument");
			e.printStackTrace();
		}
		if (null != prop) {
            String schemaName = prop.getProperty("contact.schema.name");
            String region = prop.getProperty("aws.region");
            String registryName = prop.getProperty("registry.name") != null ? 
            		prop.getProperty("registry.name") : "default-registry";
            String bootstrapBrokers = prop.getProperty("bootstrap.servers");

            if (bootstrapBrokers == null || kafkaTopic == null || schemaName == null) {
                throw new RuntimeException("Required environment variables not set: MSK_CLUSTER_ARN, KAFKA_TOPIC, CONTACT_SCHEMA_NAME");
            }

            // Get the schema definition from AWS Glue Schema Registry
            String schemaDefinition = getSchemaDefinitionFromRegistry(region, registryName, schemaName);
            System.out.println("Retrieved schema definition from registry for: " + schemaName);
            Schema schema = new Schema.Parser().parse(schemaDefinition);
            
            
            System.setProperty("software.amazon.awssdk.http.service.impl", "software.amazon.awssdk.http.urlconnection.UrlConnectionSdkHttpService");
            properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapBrokers);
            properties.put(ConsumerConfig.GROUP_ID_CONFIG, "avro-standalone-client");
            properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, AWSKafkaAvroDeserializer.class.getName());
            properties.put(AWSSchemaRegistryConstants.AWS_REGION, region);
            properties.put(AWSSchemaRegistryConstants.REGISTRY_NAME, registryName);
            properties.put(AWSSchemaRegistryConstants.SCHEMA_NAME, schemaName);
            properties.put(AWSSchemaRegistryConstants.AVRO_RECORD_TYPE, AvroRecordType.GENERIC_RECORD.getName());
            properties.put(AWSSchemaRegistryConstants.SCHEMA_AUTO_REGISTRATION_SETTING, true);
            
            
            // Configure IAM authentication
            properties.put("security.protocol", "SASL_SSL");
            properties.put("sasl.mechanism", "AWS_MSK_IAM");
            properties.put("sasl.jaas.config", "software.amazon.msk.auth.iam.IAMLoginModule required;");
            properties.put("sasl.client.callback.handler.class", "software.amazon.msk.auth.iam.IAMClientCallbackHandler");
                   
       
            

            try {
            	KafkaConsumer<String, GenericRecord> consumer = new KafkaConsumer<String, GenericRecord>(properties);
                consumer.subscribe(Collections.singletonList(kafkaTopic));
                while (true) {
                    ConsumerRecords<String, GenericRecord> records = consumer.poll(Duration.ofMillis(1000));
                    for (final ConsumerRecord<String, GenericRecord> record : records) {
                    	GenericRecord value = record.value();
                    	System.out.println("Received message: value = " + value);
                    } 
                }
            } catch (final SerializationException e) {
            		e.printStackTrace();
            }
		}
		
	}
	
	public static Properties readPropertiesFile(String fileName) throws FileNotFoundException, IOException {
		FileInputStream fis = null;
		Properties prop = null;
		try {
			fis = new FileInputStream(fileName);
			prop = new Properties();
			prop.load(fis);
		} catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
			throw new FileNotFoundException("Not a valid property file path");
		} catch (IOException ioe) {
			ioe.printStackTrace();
			throw new IOException("Problem reading property file. Check permissions");
		} finally {
			fis.close();
		}
		return prop;
	}	

    /**
     * Get schema definition from AWS Glue Schema Registry
     * 
     * @param region AWS region
     * @param registryName Registry name
     * @param schemaName Schema name
     * @return Schema definition as a string
     */
    private static String getSchemaDefinitionFromRegistry(String region, String registryName, String schemaName) {
        try {
            // Create Glue client with explicit HTTP client
            GlueClient glueClient = GlueClient.builder()
                    .httpClientBuilder(UrlConnectionHttpClient.builder())
                    .region(software.amazon.awssdk.regions.Region.of(region))
                    .build();
            
            // Get schema definition
            GetSchemaVersionRequest request = GetSchemaVersionRequest.builder()
                    .schemaId(SchemaId.builder()
                            .registryName(registryName)
                            .schemaName(schemaName)
                            .build())
                    .schemaVersionNumber(SchemaVersionNumber.builder().latestVersion(true).build())
                    .build();
            
            GetSchemaVersionResponse response = glueClient.getSchemaVersion(request);
            String schemaVersionId = response.schemaVersionId();
            String schemaDefinition = response.schemaDefinition();
            
            System.out.println("Retrieved schema version ID: " + schemaVersionId);
            System.out.println("Retrieved schema definition: " + schemaDefinition);
            
            return schemaDefinition;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get schema definition from registry: " + e.getMessage(), e);
        }
    }
	
}