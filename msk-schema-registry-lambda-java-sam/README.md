# msk-schema-registry-lambda-java-sam
# Java AWS Lambda Kafka consumer of messages in AVRO format validated against a schema defined in a Glue Schema Registry and with IAM auth, using AWS SAM

This pattern is an example of a Lambda function that consumes messages from an Amazon Managed Streaming for Kafka (Amazon MSK) topic, where the MSK Cluster has been configured to use IAM authentication and the MSK Event Listener in the Lambda function is configured to validated AVRO messages against an AVRO schema defined in an AWS Glue Schema Registry
This project contains source code and supporting files for a serverless application that you can deploy with the SAM CLI. It includes the following files and folders.

- msk_with_iam_consumer_dynamo_sam/msk_iam_event_consumer_function/src/main/java - Code for the application's Lambda function.
- msk_with_iam_consumer_dynamo_sam/msk_iam_event_consumer_function/src/test/java - Unit tests for the application's Lambda function.
- msk_with_iam_consumer_dynamo_sam/events - Invocation events that you can use to invoke the function.
- msk_with_iam_consumer_dynamo_sam/template_original.yaml - A template that defines the application's Lambda function.
- msk_with_iam_message_sender_json/src/main/java - Code for a Java program that can be used to send Kafka events with a JSON payload to the Lambda function with an MSK event source.
- MSKAndKafkaClientEC2.yaml - A Cloudformation template file that can be used to deploy an MSK cluster and also deploy an EC2 machine with all pre-requisities already installed, so you can directly build and deploy the lambda function and test it out.

Important: this application uses various AWS services and there are costs associated with these services after the Free Tier usage - please see the [AWS Pricing page](https://aws.amazon.com/pricing/) for details. You are responsible for any AWS costs incurred. No warranty is implied in this example.

## Requirements

* [Create an AWS account](https://portal.aws.amazon.com/gp/aws/developer/registration/index.html) if you do not already have one and log in. The IAM user that you use must have sufficient permissions to make necessary AWS service calls and manage AWS resources.

## Run the Cloudformation template to create the MSK Cluster and Client EC2 machine

* [Run the Cloudformation template using the file MSKAndKafkaClientEC2.yaml] - You can go to the AWS Cloudformation console, create a new stack by specifying the template file. You can keep the defaults for input parameters or modify them as necessary. Wait for the Cloudformation stack to be created. This Cloudformation template will create an MSK cluster (Provisioned or Serverless based on your selection). It will also create an EC2 machine that you can use as a client.

* [Connect to the EC2 machine] - Once the Cloudformation stack is created, you can go to the EC2 console and log into the machine using either "Connect using EC2 Instance Connect" or "Connect using EC2 Instance Connect Endpoint" option under the "EC2 Instance Connect" tab.
Note: You may need to wait for some time after the Cloudformation stack is created, as some UserData scripts continue running after the Cloudformation stack shows Created.

* [Check if Kafka Topic has been created] - Once you are inside the EC2 machine, you should be in the /home/ec2-user folder. Check to see the contents of the file kafka_topic_creator_output.txt by running the command cat kafka_topic_creator_output.txt. You should see an output such as "Created topic MskIamJavaLambdaTopic."

If you are not able to find the file kafka_topic_creator_output.txt or if it is blank or you see an error message, then you need to run the file ./kafka_topic_creator.sh. This file runs a script that goes and creates the Kafka topic that the Lambda function will subscribe to.

## Pre-requisites to Deploy the sample Lambda function

The EC2 machine that was created by running the Cloudformation template has all the software that will be needed to deploy the Lambda function.

The AWS SAM CLI is a serverless tool for building and testing Lambda applications. It uses Docker to locally test your functions in an Amazon Linux environment that resembles the Lambda execution environment. It can also emulate your application's build environment and API.

* Java - On the EC2 machine, we have installed the version of Java that you selected. We have installed Amazon Corrretto JDK of the version that you had selected at the time of specifying the input parameters in the Cloudformation template. At the time of publishing this pattern, only Java versions 11, 17 and 21 are supported by AWS SAM
* Maven - On the EC2 machine, we have installed Maven (https://maven.apache.org/install.html)
* AWS SAM CLI - We have installed the AWS SAM CLI (https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-install.html)
* Docker - We have installed the Docker Community Edition on the EC2 machine (https://hub.docker.com/search/?type=edition&offering=community)

We have also cloned the Github repository for serverless-patterns on the EC2 machine already by running the below command
    ``` 
    git clone https://github.com/aws-samples/serverless-patterns.git
    ```
Change directory to the pattern directory:
    ```
    cd serverless-patterns/msk-schema-registry-lambda-java-sam/msk_with_iam_consumer_dynamo_sam
    ```

## Use the SAM CLI to build and test locally

Build your application with the `sam build` command.

```bash
sam build
```

The SAM CLI installs dependencies defined in `kafka_event_consumer_function/pom.xml`, creates a deployment package, and saves it in the `.aws-sam/build` folder.


## Deploy the sample application


To deploy your application for the first time, run the following in your shell:

```bash
sam deploy --capabilities CAPABILITY_IAM --no-confirm-changeset --no-disable-rollback --region $AWS_REGION --stack-name msk-schema-registry-lambda-java-sam --guided
```

The sam deploy command will package and deploy your application to AWS, with a series of prompts. You can accept all the defaults by hitting Enter:

* **Stack Name**: The name of the stack to deploy to CloudFormation. This should be unique to your account and region, and a good starting point would be something matching your project name.
* **AWS Region**: The AWS region you want to deploy your app to.
* **Parameter MSKClusterName**: The name of the MSKCluster
* **Parameter MSKClusterId**: The unique ID of the MSKCluster
* **Parameter MSKTopic**: The Kafka topic on which the lambda function will listen on
* **Confirm changes before deploy**: If set to yes, any change sets will be shown to you before execution for manual review. If set to no, the AWS SAM CLI will automatically deploy application changes.
* **Allow SAM CLI IAM role creation**: Many AWS SAM templates, including this example, create AWS IAM roles required for the AWS Lambda function(s) included to access AWS services. By default, these are scoped down to minimum required permissions. To deploy an AWS CloudFormation stack which creates or modifies IAM roles, the `CAPABILITY_IAM` value for `capabilities` must be provided. If permission isn't provided through this prompt, to deploy this example you must explicitly pass `--capabilities CAPABILITY_IAM` to the `sam deploy` command.
* **Disable rollback**: Defaults to No and it preserves the state of previously provisioned resources when an operation fails
* **Save arguments to configuration file**: If set to yes, your choices will be saved to a configuration file inside the project, so that in the future you can just re-run `sam deploy` without parameters to deploy changes to your application.
* **SAM configuration file [samconfig.toml]**: Name of the configuration file to store configuration information locally
* **SAM configuration environment [default]**: Environment for storing deployment information locally

You should get a message "Successfully created/updated stack - <StackName> in <Region>" if all goes well
    
This template will deploy two lambda functions - java-msk-iam-consumer-powertools-dynamodb-sam and java-msk-iam-consumer-legacy-dynamodb-sam

The java-msk-iam-consumer-powertools-dynamodb-sam function has been deployed using the Lambda Powertools library that automagically deserializes the incoming AVRO messages using an annotation provided by Powertools.

The java-msk-iam-consumer-legacy-dynamodb-sam function does not use the Lambda Powertools library and shows a way of deserializing the Kafka AVRO messages using the legacy approach for deserializing Kafka messages in a lambda function without using any annotations.
    
**Note: In case you want to deploy the Lambda function by pointing to an existing MSK Cluster and not the one created by running the CloudFormation template provided in this pattern, you will need to modify the values of the parameters MSKClusterName and MSKClusterId accordingly**


## Test the sample application

Once the lambda function is deployed, send some Kafka messages on the topic that the lambda function is listening on, on the MSK server.

For your convenience, a Java program has been created on the EC2 machine that was provisioned using Cloudformation.

/home/ec2-user/serverless-patterns/msk-schema-registry-lambda-java-sam/msk_with_iam_message_sender_json

To run this Java program, we have created a script called kafka_message_sender.sh in the /home/ec2-user folder. Run that script and you should be able to send a new Kafka message in every line as shown below

Execute the script by passing two parameters, a random string and an integer between 1 and 500

```
sh /home/ec2-user/kafka_message_sender.sh <Random String> <Number between 1 and 500>

For example, 

sh /home/ec2-user/kafka_message_sender.sh FirstBatch 100

```

The kafka_message_sender.sh in turn invokes the java class, as shown below


```
java -classpath /home/ec2-user/serverless-patterns/msk-schema-registry-lambda-java-sam/msk_with_iam_message_sender_json/target/json-msk-iam-producer-0.0.1-SNAPSHOT.jar msk.iam.producer.JsonKafkaProducer /home/ec2-user/client.properties MskIamJsonTestTopic <Random String> <Number between 1 and 500>
```

Either send at least 10 messages or wait for 300 seconds (check the values of BatchSize: 10 and MaximumBatchingWindowInSeconds: 300 in the template.yaml file)

Then check Cloudwatch logs and you should see messages for the Cloudwatch Log Group with the name of the deployed Lambda function.

The lambda code parses the Kafka messages and outputs the fields in the Kafka messages to Cloudwatch logs

A single lambda function receives a batch of messages. The messages are received as a map with each key being a combination of the topic and the partition, as a single batch can receive messages from multiple partitions.

Each key has a list of messages. Each Kafka message has the following properties - Topic, Partition, Offset, TimeStamp, TimeStampType, Key and Value

The Key and Value are base64 encoded and have to be decoded. A message can also have a list of headers, each header having a key and a value.

The code in this example prints out the fields in the Kafka message and also decrypts the key and the value and logs them in Cloudwatch logs.

## Cleanup

You can first clean-up the Lambda function by running the sam delete command

```
cd /home/ec2-user/serverless-patterns/msk-lambda-iam-java-sam
sam delete

```
confirm by pressing y for both the questions
You should see the lambda function getting deleted and a final confirmation "Deleted successfully" on the command-line

Next you need to delete the Cloudformation template that created the MSK Server and the EC2 machine by going to the Cloudformation console and selecting the stack and then hitting the "Delete" button. It will run for sometime but eventually you should see the stack getting cleaned up. If you get an error message that says the stack could not be deleted, please retry again and do a Force Delete. The reason this may happen is because ENIs created by the deplayed Lambda function in your VPC may prevent the VPC from being deleted even after deleting the lambda function.