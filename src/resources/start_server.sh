#!/bin/bash
cd /home/ec2-user/
source /home/ec2-user/app_env.sh
env > /home/ec2-user/env_vars.log
pkill -f 'java -jar /home/ec2-user/AwsDeveloperProject.jar'

nohup java -jar AwsDeveloperProject.jar > app.log 2>&1 &