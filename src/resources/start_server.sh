#!/bin/bash
cd /home/ec2-user/
env > /home/ec2-user/env_vars.log
source /etc/environment
env > /home/ec2-user/env_vars_updated.log
pkill -f 'java -jar /home/ec2-user/AwsDeveloperProject.jar'

nohup java -jar AwsDeveloperProject.jar > app.log 2>&1 &