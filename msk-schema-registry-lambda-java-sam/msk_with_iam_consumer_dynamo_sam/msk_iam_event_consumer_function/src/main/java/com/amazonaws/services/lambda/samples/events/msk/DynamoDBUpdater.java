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
	
	public PutItemOutcome insertIntoDynamoDB(KafkaMessage message) {
		Item item = new Item();
		item.withPrimaryKey("MessageKey", message.getDecodedKey());
		item.withString("Topic", message.getTopic());
		item.withInt("Partition", message.getPartition());
		item.withLong("Offset", message.getOffset());
		item.withLong("Timestamp", message.getTimestamp());
		item.withString("TimeStampType", message.getTimestampType());
		item.withString("Firstname", message.getPerson().getFirstname());
		item.withString("Lastname", message.getPerson().getLastname());
		item.withString("Company", message.getPerson().getCompany());
		item.withString("Address_Street", message.getPerson().getStreet());
		item.withString("City", message.getPerson().getCity());
		item.withString("County", message.getPerson().getCounty());
		item.withString("State", message.getPerson().getState());
		item.withString("Zip", message.getPerson().getZip());
		item.withString("Home_Phone", message.getPerson().getHomePhone());
		item.withString("Cell_Phone", message.getPerson().getCellPhone());
		item.withString("Email", message.getPerson().getEmail());
		item.withString("Website", message.getPerson().getWebsite());
		return dynamoTable.putItem(item);
	}
	
}
