
# Image Management Application

## Overview

This project is an Image Management Application designed to demonstrate knowledge and proficiency in developing and deploying applications on AWS using various services like EC2, S3, RDS, Lambda, SNS, SQS, and others. The application allows users to upload, manage, and view images. It also includes features like image metadata retrieval, email notifications upon image uploads, and data consistency checks between the database and S3 bucket.

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [AWS Services Used](#aws-services-used)
- [Prerequisites](#prerequisites)
- [Deployment Instructions](#deployment-instructions)
- [Usage Guidelines](#usage-guidelines)
- [Configuration Details](#configuration-details)
- [Limitations and Considerations](#limitations-and-considerations)
- [License](#license)

## Features

- **Image Upload**: Users can upload images to the application.
- **Image Download**: Users can download images by name.
- **Image Deletion**: Users can delete images by name.
- **View Metadata**: Retrieve metadata for a specific image or a random image.
- **Email Notifications**: Subscribers receive email notifications when new images are uploaded.
- **Subscription Management**: Users can subscribe or unsubscribe from email notifications.
- **Data Consistency Check**: Regular checks to ensure the database metadata aligns with images stored in S3.
- **Scalable Infrastructure**: Utilizes AWS services to ensure the application scales efficiently.

## Architecture

The application architecture leverages various AWS services to provide a robust and scalable solution.

- **Amazon EC2**: Hosts the web application.
- **Amazon S3**: Stores the uploaded images.
- **Amazon RDS**: Manages the database storing image metadata.
- **AWS Lambda**: Executes serverless functions for data consistency checks and processing upload notifications.
- **Amazon SNS/SQS**: Handles messaging for notifications and decoupling services.
- **AWS CloudFormation**: Automates the provisioning of the AWS infrastructure.
- **AWS SAM (Serverless Application Model)**: Manages the serverless components of the application.

## AWS Services Used

- Amazon EC2
- Amazon S3
- Amazon RDS
- AWS Lambda
- Amazon SNS
- Amazon SQS
- AWS CloudFormation
- AWS SAM
- AWS IAM
- Amazon VPC
- Amazon CloudWatch

## Prerequisites

- **AWS Account**: You need an AWS account with permissions to create the resources.
- **AWS CLI**: Installed and configured with appropriate credentials.
- **AWS SAM CLI**: Installed for deploying serverless components.
- **Java 17**: The application is built using Java 17.
- **Gradle**: Used for building the application.
- **Postman**: For testing API endpoints (optional).

## Deployment Instructions

### 1. Clone the Repository

```bash
git clone https://github.com/KukushIvan/AwsDeveloperProject.git
cd AwsDeveloperProject
```

### 2. Build the Application

```bash
gradle clean build
```

### 3. Deploy CloudFormation Stack

Deploy the main CloudFormation template to set up the core infrastructure.

```bash
aws cloudformation deploy   --template-file cloudformation/main-template.yaml   --stack-name image-management-stack   --parameter-overrides       ParameterKey=HomeIp,ParameterValue=YOUR_IP_ADDRESS       ParameterKey=AMIId,ParameterValue=YOUR_AMI_ID       ParameterKey=InstanceType,ParameterValue=t2.micro       ParameterKey=DBUsername,ParameterValue=YOUR_DB_USERNAME       ParameterKey=DBPassword,ParameterValue=YOUR_DB_PASSWORD       ParameterKey=DBName,ParameterValue=YOUR_DB_NAME   --capabilities CAPABILITY_NAMED_IAM
```

### 4. Deploy Serverless Components with AWS SAM

Navigate to each Lambda function directory and deploy using SAM.

#### UploadsNotificationFunction

```bash
cd lambda/UploadsNotificationFunction
sam build
sam deploy   --template-file .aws-sam/build/template.yaml   --stack-name uploads-notification-stack   --s3-bucket your-s3-bucket-for-lambda-artifacts   --capabilities CAPABILITY_NAMED_IAM
cd ../..
```

#### DataConsistencyFunction

```bash
cd lambda/DataConsistencyFunction
sam build
sam deploy   --template-file .aws-sam/build/template.yaml   --stack-name data-consistency-stack   --s3-bucket your-s3-bucket-for-lambda-artifacts   --capabilities CAPABILITY_NAMED_IAM   --parameter-overrides       ParameterKey=DBUsername,ParameterValue=YOUR_DB_USERNAME       ParameterKey=DBPassword,ParameterValue=YOUR_DB_PASSWORD
cd ../..
```

### 5. Configure the Application Load Balancer (Optional)

If using an Auto Scaling group and Load Balancer, ensure they are set up and configured correctly.

### 6. Update DNS Settings (Optional)

Update your DNS settings if you want to access the application via a custom domain.

## Usage Guidelines

### API Endpoints

The application exposes several API endpoints. Use the provided Postman collection or the following endpoints.

#### Upload an Image

```http
POST http://{EC2_INSTANCE_IP}:8080/images/upload
```

- Body: Form data with a file field named `image`.

#### Download an Image

```http
GET http://{EC2_INSTANCE_IP}:8080/images/download/{imageName}
```

#### Delete an Image

```http
DELETE http://{EC2_INSTANCE_IP}:8080/images/delete/{imageName}
```

#### Get Image Metadata

```http
GET http://{EC2_INSTANCE_IP}:8080/images/metadata/{imageName}
```

#### Get Random Image Metadata

```http
GET http://{EC2_INSTANCE_IP}:8080/images/metadata/random
```

#### Subscribe to Notifications

```http
POST http://{EC2_INSTANCE_IP}:8080/subscription/subscribe
```

- Body: Form data with a field named `email`.

#### Unsubscribe from Notifications

```http
POST http://{EC2_INSTANCE_IP}:8080/subscription/unsubscribe
```

- Body: Form data with a field named `email`.

#### Data Consistency Check

To trigger a data consistency check:

```http
GET http://{EC2_INSTANCE_IP}:8080/trigger-data-consistency
```

## Configuration Details

### Environment Variables

Ensure the following environment variables are set for the application and Lambda functions:

- `DB_USER`: Database username.
- `DB_PASSWORD`: Database password.
- `DB_HOST`: Database endpoint.
- `DB_NAME`: Database name.
- `S3_BUCKET_NAME`: Name of the S3 bucket storing images.
- `NOTIFICATION_QUEUE`: SQS queue name.
- `NOTIFICATION_TOPIC`: SNS topic ARN.

### AWS Parameters

Parameters used in CloudFormation and SAM templates:

- `HomeIp`: Your IP address for SSH access.
- `AMIId`: ID of your custom AMI.
- `InstanceType`: EC2 instance type (e.g., t2.micro).
- `DBUsername`: Database username.
- `DBPassword`: Database password.
- `DBName`: Name of the database.
- `AWSRegion`: AWS region for deployment.

## Limitations and Considerations

- **Cost Management**: Be aware of AWS costs, especially for NAT Gateways, VPC Endpoints, and running EC2 instances. Remove or stop resources when not in use.
- **AWS Limits**: Ensure you stay within AWS free-tier limits to avoid unexpected charges.
- **Security**: Do not commit sensitive information like AWS credentials or passwords to version control.
- **Resource Cleanup**: After testing or demoing the application, clean up AWS resources to prevent incurring costs.
- **Email Limits**: AWS SES has limits on sending emails. Monitor your usage to prevent hitting those limits.

## License

This project is licensed under the MIT License.
