package msk.iam.producer;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.amazonaws.services.schemaregistry.deserializers.GlueSchemaRegistryKafkaDeserializer;
import com.amazonaws.services.schemaregistry.serializers.GlueSchemaRegistryKafkaSerializer;
import com.amazonaws.services.schemaregistry.utils.AWSSchemaRegistryConstants;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.glue.GlueClient;
import software.amazon.awssdk.services.glue.model.GetSchemaRequest;
import software.amazon.awssdk.services.glue.model.GetSchemaResponse;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.glue.GlueClient;
import software.amazon.awssdk.services.glue.model.*;


public class AvroKafkaProducer {

	Properties prop;

	public static void main(String[] args) {
		String propertyFile = args[0];
		String kafkaTopic = args[1];
		String seederKeyString = args[2];
		int numberOfMessages = Integer.parseInt(args[3]);
		Properties prop = null;
		try {
			prop = AvroKafkaProducer.readPropertiesFile(propertyFile);
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

            // Create Kafka producer with AWS Glue Schema Registry serializer
            Producer<String, GenericRecord> producer = KafkaProducerHelper.createProducer(
                    bootstrapBrokers, region, registryName, schemaName);
            
            //Sending out Kafka messages with AVRO schema
			String kafkaMessageKey = seederKeyString + "-" + AvroKafkaProducer.getTodayDate();
			AvroKafkaProducer.kafkaSender(prop, kafkaTopic, kafkaMessageKey, numberOfMessages, producer, schema);
		}
		
	}

	public static void kafkaSender(Properties prop, String kafkaTopic, String seederKeyString, int numberOfMessages, 
			                       Producer<String, GenericRecord> producer, Schema schema) {
		List<String> people = AvroKafkaProducer.readDataFile();
		int numberOfMessagesToSend=0;
		if (people.size() > numberOfMessages) {
			numberOfMessagesToSend = numberOfMessages;
		} else {
			numberOfMessagesToSend = people.size();
		}
		//Producer<String, String> producer = new KafkaProducer<String, String>(prop);
		for (int i = 1; i <= numberOfMessagesToSend; i++) {
			String thisKey = seederKeyString.concat("-" + Integer.toString(i));
			Person thisPerson = AvroKafkaProducer.getPersonFromLine(people.get(i));
			
			// Create AVRO record
            GenericRecord messageAvroRecord = createAvroRecord(schema, thisPerson);
			
			
			try {
				KafkaProducerHelper.sendAvroMessage(producer, kafkaTopic, thisKey, messageAvroRecord);
			} catch (Exception e) {
				System.out.println("Encountered a problem when sending a Kafka message. Ensure topic is valid");
				e.printStackTrace();
			}
			System.out.println("Sent out one Kafka message with key = " + thisKey + " and value = " + thisPerson.getFirstname() + " " + thisPerson.getLastname());
		}
		producer.close();

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
	
	public static List<String> readDataFile() {
		List<String> personList = new ArrayList<String>();
		InputStream is = AvroKafkaProducer.class.getClassLoader().getResourceAsStream("us-500.csv");
		BufferedReader bf = new BufferedReader(new InputStreamReader(is));
		String thisLine = null;
		try {
			thisLine = bf.readLine();
			while (null != thisLine) {
				personList.add(thisLine);
				thisLine = bf.readLine();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return personList;
	}
	
	public static Person getPersonFromLine(String line) {
		
		String[] fields = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
		Person thisPerson = new Person();
		thisPerson.setFirstname(fields[0]);
		thisPerson.setLastname(fields[1]);
		thisPerson.setCompany(fields[2]);
		thisPerson.setStreet(fields[3]);
		thisPerson.setCity(fields[4]);
		thisPerson.setCounty(fields[5]);
		thisPerson.setState(fields[6]);
		thisPerson.setZip(fields[7]);
		thisPerson.setHomePhone(fields[8]);
		thisPerson.setCellPhone(fields[9]);
		thisPerson.setEmail(fields[10]);
		thisPerson.setWebsite(fields[11]);
		return thisPerson;
	}
	
	public static String getTodayDate() {
		
		LocalDateTime ldt = LocalDateTime.now();
        String formattedDateStr = DateTimeFormatter.ofPattern("MM-dd-YYYY-HH-MM-SS").format(ldt);
        return formattedDateStr;
	}
	


    /**
     * Create an AVRO record from a Contact object
     * 
     * @param schema AVRO schema
     * @param contact Contact object
     * @return GenericRecord
     */
    private static GenericRecord createAvroRecord(Schema schema, Person person) {
        GenericRecord avroRecord = new GenericData.Record(schema);
        
        // Populate the record with data from the Contact object
        avroRecord.put("firstname", person.getFirstname());
        avroRecord.put("lastname", person.getLastname());
        avroRecord.put("company", person.getCompany());
        avroRecord.put("street", person.getStreet());
        avroRecord.put("city", person.getCity());
        avroRecord.put("county", person.getCounty());
        avroRecord.put("state", person.getState());
        avroRecord.put("zip", person.getZip());
        avroRecord.put("homePhone", person.getHomePhone());
        avroRecord.put("cellPhone", person.getCellPhone());
        avroRecord.put("email", person.getEmail());
        avroRecord.put("website", person.getWebsite());
        
        return avroRecord;
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
