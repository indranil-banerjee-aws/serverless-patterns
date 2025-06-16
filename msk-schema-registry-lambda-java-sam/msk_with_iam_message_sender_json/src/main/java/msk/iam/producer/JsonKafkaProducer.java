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

public class JsonKafkaProducer {

	Properties prop;

	public static void main(String[] args) {
		String propertyFile = args[0];
		String kafkaTopic = args[1];
		String seederKeyString = args[2];
		int numberOfMessages = Integer.parseInt(args[3]);
		Properties prop = null;
		try {
			prop = JsonKafkaProducer.readPropertiesFile(propertyFile);
		} catch (IOException e) {
			System.out.println("Please specify a valid Kafka Properties File as the first argument");
			e.printStackTrace();
		}
		if (null != prop) {
			Properties properties = new Properties(prop);
			properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, prop.getProperty("bootstrap.servers"));
			properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
			properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
			properties.setProperty("security.protocol", "SASL_SSL");
			properties.setProperty("sasl.mechanism", "AWS_MSK_IAM");
			properties.setProperty("sasl.jaas.config", "software.amazon.msk.auth.iam.IAMLoginModule required;");
			properties.setProperty("sasl.client.callback.handler.class", "software.amazon.msk.auth.iam.IAMClientCallbackHandler");
			String kafkaMessageKey = seederKeyString + "-" + JsonKafkaProducer.getTodayDate();
			JsonKafkaProducer.kafkaSender(properties, kafkaTopic, kafkaMessageKey, numberOfMessages);
		}
		
	}

	public static void kafkaSender(Properties prop, String kafkaTopic, String seederKeyString, int numberOfMessages) {
		List<String> people = JsonKafkaProducer.readDataFile();
		int numberOfMessagesToSend=0;
		if (people.size() > numberOfMessages) {
			numberOfMessagesToSend = numberOfMessages;
		} else {
			numberOfMessagesToSend = people.size();
		}
		Producer<String, String> producer = new KafkaProducer<String, String>(prop);
		for (int i = 1; i <= numberOfMessagesToSend; i++) {
			String thisKey = seederKeyString.concat("-" + Integer.toString(i));
			Person thisPerson = JsonKafkaProducer.getPersonFromLine(people.get(i));
			String thisPersonJson = thisPerson.toJson();
			try {
				producer.send(new ProducerRecord<String, String>(kafkaTopic, thisKey, thisPersonJson));
				producer.flush();
			} catch (Exception e) {
				System.out.println("Encountered a problem when sending a Kafka message. Ensure topic is valid");
				e.printStackTrace();
			}
			System.out.println("Sent out one Kafka message with key = " + thisKey + " and value = " + thisPersonJson);
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
		InputStream is = JsonKafkaProducer.class.getClassLoader().getResourceAsStream("us-500.csv");
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

}
