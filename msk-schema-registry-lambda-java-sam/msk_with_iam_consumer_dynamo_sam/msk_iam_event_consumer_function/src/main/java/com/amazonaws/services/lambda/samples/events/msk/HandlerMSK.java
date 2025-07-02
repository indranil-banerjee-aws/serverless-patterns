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
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.KafkaEvent;
import com.amazonaws.services.lambda.runtime.events.KafkaEvent.KafkaEventRecord;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class HandlerMSK implements RequestHandler<KafkaEvent, String>{
	//We initialize an empty list of the KafkaMessage class
	List<KafkaMessage> listOfMessages = new ArrayList<KafkaMessage>();
	Gson gson = new GsonBuilder().setPrettyPrinting().create();
	String dynamoDBTableName = System.getenv("DYNAMO_DB_TABLE");
	DynamoDBUpdater ddbUpdater = new DynamoDBUpdater(dynamoDBTableName);

	@Override
	public String handleRequest(KafkaEvent event, Context context) {
		LambdaLogger logger = context.getLogger();
		String response = new String("200 OK");
		this.listOfMessages = new ArrayList<KafkaMessage>();
		//Incoming KafkaEvent object has a property called records that is a map
		//Each key in the map is a combination of a topic and a partition
		Map<String, List<KafkaEventRecord>> record=event.getRecords();
		Set<String> keySet = record.keySet();  
		Iterator<String> iterator = keySet.iterator();
		//We iterate through each of the keys in the map
		while (iterator.hasNext()) {
			String thisKey=(String)iterator.next();
			//Using the key we retrieve the value of the map which is a list of KafkaEventRecord
	    	//One object of KafkaEventRecord represents an individual Kafka message
			List<KafkaEventRecord>  thisListOfRecords = record.get(thisKey);
			//We now iterate through the list of KafkaEventRecords
			for(KafkaEventRecord thisRecord : thisListOfRecords) {
				/*
	    		We initialize a new object of the KafkaMessage class which is a simplified representation in our models package
	    		We then get the fields from each kafka message in the object of KafkaEventRecord class and set them to the fields
	    		of the KafkaRecord class
	    		*/
				KafkaMessage thisMessage = new KafkaMessage();
				thisMessage.setTopic(thisRecord.getTopic());
				thisMessage.setPartition(thisRecord.getPartition());
				thisMessage.setOffset(thisRecord.getOffset());
				thisMessage.setTimestamp(thisRecord.getTimestamp());
				thisMessage.setTimestampType(thisRecord.getTimestampType());
				String key = thisRecord.getKey();
				String value = thisRecord.getValue();
				String decodedKey = "null";
				String decodedValue = "null";
				Person decodedPerson = null;
				GenericRecord decodedGenericPerson = null;
				//the key and value inside a kafka message are base64 encrypted and will need to be decrypted
				if (null != key) {
					byte[] decodedKeyBytes = Base64.getDecoder().decode(key);
					decodedKey = new String(decodedKeyBytes);
				} 
				if (null != value) {
					try {
						byte[] decodedValueBytes = Base64.getDecoder().decode(value);
						//DatumReader<Person> reader = new SpecificDatumReader<>(Person.class);
						DatumReader<GenericRecord> reader = new GenericDatumReader<>();
						Decoder decoder = DecoderFactory.get().binaryDecoder(decodedValueBytes, null);
						//decodedPerson = reader.read(null, decoder);
						decodedGenericPerson = reader.read(null, decoder);
						logger.log("###################################");
						logger.log("Received message: value = " + decodedGenericPerson);
						logger.log("Firstname = " + decodedGenericPerson.get("firstname"));
						logger.log("Lastname = " + decodedGenericPerson.get("lastname"));
						logger.log("Company = " + decodedGenericPerson.get("company"));
						logger.log("Street = " + decodedGenericPerson.get("street"));
						logger.log("City = " + decodedGenericPerson.get("city"));
						logger.log("County = " + decodedGenericPerson.get("county"));
						logger.log("State = " + decodedGenericPerson.get("state"));
						logger.log("Zip = " + decodedGenericPerson.get("zip"));
						logger.log("HomePhone = " + decodedGenericPerson.get("homePhone"));
						logger.log("CellPhone = " + decodedGenericPerson.get("cellPhone"));
						logger.log("Email = " + decodedGenericPerson.get("email"));
						logger.log("Website = " + decodedGenericPerson.get("website"));
						logger.log("###################################");
						
					} catch (IOException e) {
						logger.log(e.getMessage());
					}
				} 
				thisMessage.setKey(key);
	    		thisMessage.setValue(value);
	    		thisMessage.setDecodedKey(decodedKey);
	    		
	    		thisMessage.setDecodedValue(decodedGenericPerson.toString());
//	    		String AWS_SAM_LOCAL = System.getenv("AWS_SAM_LOCAL");
//	    		if (null == AWS_SAM_LOCAL) {
//	    			ddbUpdater.insertIntoDynamoDB(thisMessage);
//	    		} 
				listOfMessages.add(thisMessage);
			}
		}
		logger.log("All Messages in this batch = " + gson.toJson(listOfMessages));
		return response;
	}
	
	private String getPersonString(Person person) {
		
		String returnString = "";
		returnString.concat("Firstname = " + person.getFirstname().toString() + ",\n");
		returnString.concat("Lastname = " + person.getLastname().toString() + ",\n");
		returnString.concat("Street = " + person.getStreet().toString() + ",\n");
		returnString.concat("City = " + person.getCity().toString() + ",\n");
		returnString.concat("County = " + person.getCounty().toString() + ",\n");
		returnString.concat("State = " + person.getState().toString() + ",\n");
		returnString.concat("Zip = " + person.getZip().toString() + ",\n");
		returnString.concat("HomePhone = " + person.getHomePhone().toString() + ",\n");
		returnString.concat("CellPhone = " + person.getCellPhone().toString() + ",\n");
		returnString.concat("EMail = " + person.getEmail().toString() + ",\n");
		returnString.concat("Company = " + person.getCompany().toString() + ",\n");
		returnString.concat("Website = " + person.getWebsite().toString() + ",\n");
		return returnString;
		
	}
	
}

