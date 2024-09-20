#!/bin/bash
pkill -f 'java -jar /home/ec2-user/AwsDeveloperProject.jar'

nohup java -jar /home/ec2-user/AwsDeveloperProject.jar > /dev/null 2>&1 &