package com.amazonaws.services.lambda.samples.events.msk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.KafkaEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

class HandlerMSKTest {
	private static final String kafkaEventJson = "{\n"
			+ "   \"records\":{\n"
			+ "      \"myTopic-0\":[\n"
			+ "         {\n"
			+ "            \"topic\":\"myTopic\",\n"
			+ "            \"partition\":0,\n"
			+ "            \"offset\":250,\n"
			+ "            \"timestamp\":1678072110111,\n"
			+ "            \"timestampType\":\"CREATE_TIME\",\n"
			+ "            \"value\":\"eyJmaXJzdG5hbWUiOiJcIkVyaWNrXCIiLCJsYXN0bmFtZSI6IlwiRmVyZW5jelwiIiwiY29tcGFueSI6IlwiQ2luZHkgVHVybmVyIEFzc29jaWF0ZXNcIiIsInN0cmVldCI6IlwiMjAgUyBCYWJjb2NrIFN0XCIiLCJjaXR5IjoiXCJGYWlyYmFua3NcIiIsImNvdW50eSI6IlwiRmFpcmJhbmtzIE5vcnRoIFN0YXJcIiIsInN0YXRlIjoiXCJBS1wiIiwiemlwIjoiOTk3MTIiLCJob21lUGhvbmUiOiJcIjkwNy03NDEtMTA0NFwiIiwiY2VsbFBob25lIjoiXCI5MDctMjI3LTY3NzdcIiIsImVtYWlsIjoiXCJlcmljay5mZXJlbmN6QGFvbC5jb21cIiIsIndlYnNpdGUiOiJcImh0dHA6Ly93d3cuY2luZHl0dXJuZXJhc3NvY2lhdGVzLmNvbVwiIn0=\",\n"
			+ "            \"headers\":[\n"
			+ "               \n"
			+ "            ]\n"
			+ "         },\n"
			+ "         {\n"
			+ "            \"topic\":\"myTopic\",\n"
			+ "            \"partition\":0,\n"
			+ "            \"offset\":251,\n"
			+ "            \"timestamp\":1678072111086,\n"
			+ "            \"timestampType\":\"CREATE_TIME\",\n"
			+ "            \"value\":\"eyJmaXJzdG5hbWUiOiJcIkJsYWlyXCIiLCJsYXN0bmFtZSI6IlwiTWFsZXRcIiIsImNvbXBhbnkiOiJcIkJvbGxpbmdlciBNYWNoIFNocCBcdTAwMjYgU2hpcHlhcmRcIiIsInN0cmVldCI6IlwiMjA5IERlY2tlciBEclwiIiwiY2l0eSI6IlwiUGhpbGFkZWxwaGlhXCIiLCJjb3VudHkiOiJcIlBoaWxhZGVscGhpYVwiIiwic3RhdGUiOiJcIlBBXCIiLCJ6aXAiOiIxOTEzMiIsImhvbWVQaG9uZSI6IlwiMjE1LTkwNy05MTExXCIiLCJjZWxsUGhvbmUiOiJcIjIxNS03OTQtNDUxOVwiIiwiZW1haWwiOiJcImJtYWxldEB5YWhvby5jb21cIiIsIndlYnNpdGUiOiJcImh0dHA6Ly93d3cuYm9sbGluZ2VybWFjaHNocHNoaXB5YXJkLmNvbVwiIn0=\",\n"
			+ "            \"headers\":[\n"
			+ "               \n"
			+ "            ]\n"
			+ "         }\n"
			+ "      ]\n"
			+ "   },\n"
			+ "   \"eventSource\":\"aws:kafka\",\n"
			+ "   \"eventSourceArn\":\"arn:aws:kafka:us-west-2:123456789012:cluster/MSKWorkshopCluster/a93759a9-c9d0-4952-984c-492c6bfa2be8-13\",\n"
			+ "   \"bootstrapServers\":\"b-2.mskworkshopcluster.z9kc4f.c13.kafka.us-west-2.amazonaws.com:9098,b-3.mskworkshopcluster.z9kc4f.c13.kafka.us-west-2.amazonaws.com:9098,b-1.mskworkshopcluster.z9kc4f.c13.kafka.us-west-2.amazonaws.com:9098\"\n"
			+ "}";

	@Mock
	DynamoDBUpdater ddbUpdater;	
	
	@Test
	@ExtendWith(MockitoExtension.class)
	void invokeTest() {
//		Map<String, String> envMap = new HashMap<String, String>();
//		envMap.put("AWS_REGION", "us-west-2");
//		envMap.put("DYNAMO_DB_TABLE", "DummyTable");
//		try {
//			setEnv(envMap);
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		KafkaEvent event = gson.fromJson(kafkaEventJson, KafkaEvent.class);
		Context context = new TestContext();
		PutItemOutcome putItemOutcome = mock(PutItemOutcome.class);
		DynamoDBUpdater dbUpdater = mock(DynamoDBUpdater.class);
		HandlerMSK handler = new HandlerMSK();
		handler.ddbUpdater = dbUpdater;
		when(handler.ddbUpdater.insertIntoDynamoDB(ArgumentMatchers.any(KafkaMessage.class))).thenReturn(putItemOutcome);
		String result = handler.handleRequest(event, context);
		assertEquals(result, "200 OK");
		assertEquals(handler.listOfMessages.size(), 2);
		assertEquals(handler.listOfMessages.get(0).getTopic(), "myTopic");
		assertEquals(handler.listOfMessages.get(0).getPartition(), 0);
		assertEquals(handler.listOfMessages.get(0).getOffset(), 250L);
		assertEquals(handler.listOfMessages.get(0).getTimestamp(), 1678072110111L);
		assertEquals(handler.listOfMessages.get(0).getTimestampType(), "CREATE_TIME");
		assertEquals(handler.listOfMessages.get(0).getDecodedKey(), "null");
		String keyJson1 = "{\"firstname\":\"\\\"Erick\\\"\",\"lastname\":\"\\\"Ferencz\\\"\",\"company\":\"\\\"Cindy Turner Associates\\\"\",\"street\":\"\\\"20 S Babcock St\\\"\",\"city\":\"\\\"Fairbanks\\\"\",\"county\":\"\\\"Fairbanks North Star\\\"\",\"state\":\"\\\"AK\\\"\",\"zip\":\"99712\",\"homePhone\":\"\\\"907-741-1044\\\"\",\"cellPhone\":\"\\\"907-227-6777\\\"\",\"email\":\"\\\"erick.ferencz@aol.com\\\"\",\"website\":\"\\\"http://www.cindyturnerassociates.com\\\"\"}";
		assertEquals(handler.listOfMessages.get(0).getDecodedValue(), keyJson1);
		assertEquals(handler.listOfMessages.get(1).getTopic(), "myTopic");
		assertEquals(handler.listOfMessages.get(1).getPartition(), 0);
		assertEquals(handler.listOfMessages.get(1).getOffset(), 251L);
		assertEquals(handler.listOfMessages.get(1).getTimestamp(), 1678072111086L);
		assertEquals(handler.listOfMessages.get(1).getTimestampType(), "CREATE_TIME");
		assertEquals(handler.listOfMessages.get(1).getDecodedKey(), "null");
		String keyJson2 = "{\"firstname\":\"\\\"Blair\\\"\",\"lastname\":\"\\\"Malet\\\"\",\"company\":\"\\\"Bollinger Mach Shp \\u0026 Shipyard\\\"\",\"street\":\"\\\"209 Decker Dr\\\"\",\"city\":\"\\\"Philadelphia\\\"\",\"county\":\"\\\"Philadelphia\\\"\",\"state\":\"\\\"PA\\\"\",\"zip\":\"19132\",\"homePhone\":\"\\\"215-907-9111\\\"\",\"cellPhone\":\"\\\"215-794-4519\\\"\",\"email\":\"\\\"bmalet@yahoo.com\\\"\",\"website\":\"\\\"http://www.bollingermachshpshipyard.com\\\"\"}";
		assertEquals(handler.listOfMessages.get(1).getDecodedValue(), keyJson2);
	}
	
//	@SuppressWarnings({ "unchecked", "rawtypes" })
//	protected static void setEnv(Map<String, String> newenv) throws Exception {
//		try {
//			Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
//			Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
//			theEnvironmentField.setAccessible(true);
//			Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
//			env.putAll(newenv);
//			Field theCaseInsensitiveEnvironmentField = processEnvironmentClass
//					.getDeclaredField("theCaseInsensitiveEnvironment");
//			theCaseInsensitiveEnvironmentField.setAccessible(true);
//			Map<String, String> cienv = (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
//			cienv.putAll(newenv);
//		} catch (NoSuchFieldException e) {
//			Class[] classes = Collections.class.getDeclaredClasses();
//			Map<String, String> env = System.getenv();
//			for (Class cl : classes) {
//				if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
//					Field field = cl.getDeclaredField("m");
//					field.setAccessible(true);
//					Object obj = field.get(env);
//					Map<String, String> map = (Map<String, String>) obj;
//					map.clear();
//					map.putAll(newenv);
//				}
//			}
//		}
//	}

}
