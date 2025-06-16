package com.amazonaws.services.lambda.samples.events.msk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.google.gson.Gson;
import org.mockito.ArgumentMatchers;

class DynamoDBUpdaterTest {

	@Test
	void testDynamoDBUpdater() {
		DynamoDBUpdater ddbUpdater = new DynamoDBUpdater("DBTable");
		assertNotNull(ddbUpdater);
		assertEquals(ddbUpdater.dynamoDBTableName, "DBTable");
		assertNotNull(ddbUpdater.client);
		assertNotNull(ddbUpdater.dynamoDB);
		assertNotNull(ddbUpdater.dynamoTable);
	}

	@Test
	void testInsertIntoDynamoDB() {
		KafkaMessage kafkaMessage = new KafkaMessage();
		Person person = new Person();
		person.setFirstname("Robert");
		person.setLastname("Thomas");
		person.setCompany("ABC Company");
		person.setStreet("123 Stone Street");
		person.setCity("Sun City");
		person.setCounty("Mariposa");
		person.setState("CA");
		person.setZip("94436");
		person.setCellPhone("123-456-7890");
		person.setHomePhone("234-567-8901");
		person.setEmail("abc@xyz.com");
		person.setWebsite("www.robert_thomas.com");
		kafkaMessage.setPerson(person);
		kafkaMessage.setTopic("MyTopic");
		kafkaMessage.setPartition(2);
		kafkaMessage.setOffset(2345L);
		kafkaMessage.setTimestamp(1234567890L);
		kafkaMessage.setTimestampType("CREATE");
		kafkaMessage.setKey("eyJmaXJzdG5hbWUiOiJcIkJsYWlyXCIiLCJsYXN0bmFtZSI6IlwiTWFsZXRcIiIsImNvbXBhbnkiOiJcIkJvbGxpbmdlciBNYWNoIFNocCBcdTAwMjYgU2hpcHlhcmRcIiIsInN0cmVldCI6IlwiMjA5IERlY2tlciBEclwiIiwiY2l0eSI6IlwiUGhpbGFkZWxwaGlhXCIiLCJjb3VudHkiOiJcIlBoaWxhZGVscGhpYVwiIiwic3RhdGUiOiJcIlBBXCIiLCJ6aXAiOiIxOTEzMiIsImhvbWVQaG9uZSI6IlwiMjE1LTkwNy05MTExXCIiLCJjZWxsUGhvbmUiOiJcIjIxNS03OTQtNDUxOVwiIiwiZW1haWwiOiJcImJtYWxldEB5YWhvby5jb21cIiIsIndlYnNpdGUiOiJcImh0dHA6Ly93d3cuYm9sbGluZ2VybWFjaHNocHNoaXB5YXJkLmNvbVwiIn0=");
		kafkaMessage.setValue("MyValue");
		kafkaMessage.setDecodedKey("MyDecodedKey");
		Gson gson = new Gson();
		String personJson = gson.toJson(person);
		kafkaMessage.setDecodedValue(personJson);
	    Table dynamoDbTable = mock(Table.class);
	    AmazonDynamoDB client = mock(AmazonDynamoDB.class);
		DynamoDB dynamoDB = mock(DynamoDB.class);
	    PutItemOutcome putoutcome = mock(PutItemOutcome.class);
	    DynamoDBUpdater ddbUpdater = new DynamoDBUpdater("DBTable");
	    ddbUpdater.client = client;
	    ddbUpdater.dynamoDB = dynamoDB;
	    ddbUpdater.dynamoTable = dynamoDbTable;
	    when(ddbUpdater.dynamoTable.putItem(ArgumentMatchers.any(Item.class))).thenReturn(putoutcome);
		PutItemOutcome putOutcome = ddbUpdater.insertIntoDynamoDB(kafkaMessage);
		assertNotNull(putOutcome);
	}
}
