package emse;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;

public class EC2Worker {

	/* Read a CSV file */
    public static List<List<String>> readFile(String csvName) throws IOException, CsvException {
        try(CSVReader reader = new CSVReader(new FileReader(csvName))) {
            List<List<String>> result = new ArrayList<List<String>>();
            try (CSVReader csvReader = new CSVReader(new FileReader(csvName));) {
                String[] values = null;
                csvReader.readNext();
                while ((values = csvReader.readNext()) != null) {
                    result.add(Arrays.asList(values));
                }
            }
            return result;
        }
    }
    /* */

    
    /* Time waiter */
    public static void wait(int s) {
        try {
            Thread.sleep(s * 1000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
    /* */

	
	public static void main(String[] args) throws IOException, CsvException {
		
		/* Create ec2 Instance */
		String [] tab = {"ec2Worker", "ami-0218d08a1f9dac831"};

        final String USAGE = "\n" +
                "Usage:\n" +
                "   <name> <amiId>\n\n" +
                "Where:\n" +
                "   name - an instance name value that you can obtain from the AWS Console (for example, ami-xxxxxx5c8b987b1a0). \n\n" +
                "   amiId - an Amazon Machine Image (AMI) value that you can obtain from the AWS Console (for example, i-xxxxxx2734106d0ab). \n\n" ;

       if (tab.length != 2) {
            System.out.println(USAGE);
            System.exit(1);
       }

        String name = tab[0]; /* Ec2 Worker */
        String amiId = tab[1]; /* ami-09d4b65ff082c3c6a */
        Region region = Region.AP_NORTHEAST_1;
        Ec2Client ec2 = Ec2Client.builder()
                .region(region)
                .build();

        String instanceId = CreateEC2Instance.createEC2Instance(ec2, name, amiId) ;
        System.out.println("The Amazon EC2 Instance ID is "+ instanceId);
        ec2.close();
        /* */
		
        
		/* Create a Queue for the Inbox and a Queue for the Outbox */
        String queueName1 = "Inbox";
        String queueName2 = "Outbox";

        SqsClient sqsClient = SqsClient.builder()
                .region(Region.AP_NORTHEAST_1)
                .build();
        
        System.out.println("Sqs Client");
        System.out.println(sqsClient);

        String queueUrl1 = SQS.createQueue(sqsClient, queueName1);
        System.out.println(queueUrl1);
        String queueUrl2 = SQS.createQueue(sqsClient, queueName2);
        System.out.println(queueUrl2);
        /* */
        
        
        S3Client s3 = S3Client.builder()
                .region(Region.AP_NORTHEAST_1)
                .build();
      	String key = "1";
		String WorkerfileName = "resultfile";
        
      	
        /* Check for messages every 1 minute in the Inbox
         * If their is one, retrieve and then delete it */
        List<Message> messages = SQS.receiveMessages(sqsClient, queueUrl1);
        while(true) { //Infinite loop
        	if (messages.isEmpty()) {
            	System.out.println("No new messages");
            }
            else {
        	System.out.println(messages);
        	SQS.deleteMessages(sqsClient, queueUrl1, messages);
            
        	
        	/* Retrieve file */
    		Message MessageBucket = messages.get(0);
    		Message MessageFile = messages.get(1);
    		
    		String bucketName = MessageBucket.body();
    		String fileName = MessageFile.body();
    		
    		System.out.println(bucketName);
            System.out.println(fileName);
    		
    		GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3.getObject(getObjectRequest);
            /* */
            
            
            /* Calculs */
    		String csvfile = "src/emse/sales-2021-01-02.csv";
    	        List<List<String>> result = readFile(csvfile);
    	        for (List<String> row :result){
    	            System.out.println(row.get(2));
    	        }
    	        System.out.println(Calculs.countSales(result));
    	        System.out.println(String.valueOf(Calculs.totalAmountSold(result)));
    	        System.out.println(String.valueOf(Calculs.averageSold(result)));
            /* */
         

            /* Write the result in a file */
    	    String csv = "/Users/emmacremon/Desktop/results.csv"; //Create an empty file on your device to store results
    	    try {
    	    	CSVWriter writer = new CSVWriter(new FileWriter(csv));
    	        String [] record = {""+Calculs.countSales(result),""+Calculs.totalAmountSold(result),""+Calculs.averageSold(result)};
    	        writer.writeNext(record);
    	        writer.close();
    	        }
    	    catch (IOException e) {
    	        e.printStackTrace();
    	        }
            /* */
    	    
    	    /* Put the file in Amazon S3 */
    	    PutObjectRequest objectRequest = PutObjectRequest.builder()
    	              .bucket(bucketName)
    	              .key(key)
    	              .build();

    	    Path path = Paths.get("/Users/emmacremon/Desktop/result.csv");
    		s3.putObject(objectRequest, RequestBody.fromFile(path));
    	    /* */
            
            
            /* Send a response message in the Outbox queue to the client 
             * with the name of the incoming file and 
             * the output file containing the result */
            String msg = "Incoming file: "+fileName+"; Output file: "+WorkerfileName;
            
            SQS.sendMessages(sqsClient, queueUrl1, msg);
            System.out.println(msg);
            /* */
           
            }
        	
        	wait(60); //Wait one minute before checking again
        }
        /* */
       
      
        
    }
	
}
