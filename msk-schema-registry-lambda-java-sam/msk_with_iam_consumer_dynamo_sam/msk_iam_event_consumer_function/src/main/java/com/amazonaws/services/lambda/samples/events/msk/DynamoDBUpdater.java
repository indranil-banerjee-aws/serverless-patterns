package com.amazonaws.services.lambda.samples.events.msk;

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;

public class DynamoDBUpdater {

	String dynamoDBTableName;
	AmazonDynamoDB client;
	DynamoDB dynamoDB;
	Table dynamoTable;
	

	public DynamoDBUpdater(String dynamoDBTableName) {
		super();
		if (null == dynamoDBTableName) {
			this.dynamoDBTableName = "MSK_LAMBDA_DYNAMO_TABLE";
		} else {
			this.dynamoDBTableName = dynamoDBTableName;
		}
		String AWS_SAM_LOCAL = System.getenv("AWS_SAM_LOCAL");
		if (null == AWS_SAM_LOCAL) {
			this.client = AmazonDynamoDBClientBuilder.standard().build();
		} else {
			this.client = AmazonDynamoDBClientBuilder.standard().withEndpointConfiguration(new EndpointConfiguration("http://127.0.0.1:8000", "")).build();
			this.dynamoDBTableName = "MSK_LAMBDA_DYNAMO_TABLE";
		}
		//this.client = AmazonDynamoDBClientBuilder.standard().build();
		this.dynamoDB = new DynamoDB(client);
		this.dynamoTable = dynamoDB.getTable(this.dynamoDBTableName);
	}
	
//	public PutItemOutcome insertIntoDynamoDB(KafkaMessage message) {
//		Item item = new Item();
//		item.withPrimaryKey("MessageKey", message.key);
//		item.withString("Topic", message.getTopic());
//		item.withInt("Partition", message.getPartition());
//		item.withLong("Offset", message.getOffset());
//		item.withLong("Timestamp", message.getTimestamp());
//		item.withString("TimeStampType", message.getTimestampType());
//		item.withString("Firstname", message.getValue().getFirstname().toString());
//		item.withString("Lastname", message.getValue().getLastname().toString());
//		item.withString("Company", message.getValue().getCompany().toString());
//		item.withString("Address_Street", message.getValue().getStreet().toString());
//		item.withString("City", message.getValue().getCity().toString());
//		item.withString("County", message.getValue().getCounty().toString());
//		item.withString("State", message.getValue().getState().toString());
//		item.withString("Zip", message.getValue().getZip().toString());
//		item.withString("Home_Phone", message.getValue().getHomePhone().toString());
//		item.withString("Cell_Phone", message.getValue().getCellPhone().toString());
//		item.withString("Email", message.getValue().getEmail().toString());
//		item.withString("Website", message.getValue().getWebsite().toString());
//		return dynamoTable.putItem(item);
//	}
	
}
