#!/bin/bash
cd /home/ec2-user/
pkill -f 'java -jar /home/ec2-user/AwsDeveloperProject.jar'

nohup java -jar AwsDeveloperProject.jar > app.log 2>&1 &