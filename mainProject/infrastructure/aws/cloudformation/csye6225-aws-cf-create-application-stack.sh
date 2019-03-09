#!/bin/bash
NET_STACK_NAME=$1
keyPair=$2
EC2="${NET_STACK_NAME}-csye6225-ec2"

export vpcID=$(aws ec2 describe-vpcs --filters "Name=tag-key,Values=Name" --query "Vpcs[*].[CidrBlock, VpcId][-1]" --output text|grep 10.0.0.0/16|awk '{print $2}')

export subnet1=$(aws ec2 describe-subnets --filters "Name=vpc-id,Values=$vpcID" --query 'Subnets[*].[SubnetId, VpcId, AvailabilityZone, CidrBlock]' --output text|grep 10.0.11.0/24|grep us-east-1a|awk '{print $1}')

export subnet2=$(aws ec2 describe-subnets --filters "Name=vpc-id,Values=$vpcID" --query 'Subnets[*].[SubnetId, VpcId, AvailabilityZone, CidrBlock]' --output text|grep 10.0.12.0/24|grep us-east-1b|awk '{print $1}')

export subnet3=$(aws ec2 describe-subnets --filters "Name=vpc-id,Values=$vpcID" --query 'Subnets[*].[SubnetId, VpcId, AvailabilityZone, CidrBlock]' --output text|grep 10.0.14.0/24|grep us-east-1c|awk '{print $1}')

export AMI=$(aws ec2 describe-images --filters "Name=name,Values=csye6225" --query 'sort_by(Images, &CreationDate)[-1].ImageId' --output text)

aws cloudformation create-stack --stack-name $NET_STACK_NAME --template-body file://csye6225-cf-application.json --parameters ParameterKey=VpcId,ParameterValue=$vpcID ParameterKey=EC2Name,ParameterValue=$EC2 ParameterKey=SubnetId1,ParameterValue=$subnet1 ParameterKey=SubnetId2,ParameterValue=$subnet2 ParameterKey=SubnetId3,ParameterValue=$subnet3 ParameterKey=AMI,ParameterValue=$AMI ParameterKey=keyName,ParameterValue=$keyPair


export STACK_STATUS=$(aws cloudformation describe-stacks --stack-name $NET_STACK_NAME --query "Stacks[][ [StackStatus ] ][]" --output text)

while [ $STACK_STATUS != "CREATE_COMPLETE" ]
do
	STACK_STATUS=`aws cloudformation describe-stacks --stack-name $NET_STACK_NAME --query "Stacks[][ [StackStatus ] ][]" --output text`
done
echo "Created Stack ${NET_STACK_NAME} successfully!"