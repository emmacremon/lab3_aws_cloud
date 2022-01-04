# lab3_aws_cloud
A Java application based on the Web-Queue-Worker architecture (Microsoft, 2021) to summarize sale transactions in a Cloud environment. 
Amazon AWS is used as the cloud provider.

The application is divided in two main components: the Client and the Worker.

The Worker is responsible for :
  - waiting for a message from the Client
  - once the message is received with the name of the file to process, reading the file
  - calculating (a) the Total Number of Sales, (b) the Total Amount Sold and (c) the Average Sold per country and per product
  - writing a file in the cloud
  - sending a message with the name of the file to the Client
  - waiting for another message
  
The Client is responsible for :
  - reading the CSV file
  - uploading it into the cloud
  - sending a message to the Worker signaling that there is a file ready to be processed
  - waiting until it receives a message from the Worker that the summarization was completed, and
  - downloading the resulting file.

Here is the Worker part of the application.
