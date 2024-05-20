import psycopg2
import json
import requests
import logging

#reading configuration file from system
with open("subscription-migration-config.json") as file:
    config=json.load(file)


#init local variables

postgersDatabaseName = config["postgersDatabaseName"]
postgersUser = config["postgersUser"]
postgersPassword = config["postgersPassword"]
postgersHost = config["postgersHost"]
postgersPort = config["postgersPort"]
host_url = config["host_url"]
logLevel = "DEBUG"

request_url = "/iudx/cat/v1/search?property=[id]&value=[[entity]]&filter=[id,provider,name,description,authControlGroup,accessPolicy,iudxResourceAPIs,instance,resourceGroup,type]"

# connecting with postgers
conn = psycopg2.connect(host=postgersHost, database=postgersDatabaseName, user=postgersUser, password=postgersPassword,port=postgersPort)
cur = conn.cursor()

# get data from subscriptions table in postgers
select_stmt = "select _id,queue_name,entity from subscriptions where provider_id is null or resource_group is null or delegator_id is null or item_type is null"
cur.execute(select_stmt)
records=cur.fetchall()

for data in records:
	_id=data[0]
	queue_name=data[1]
	entity= data[2]
	user_id = queue_name.split("/")[0]
	response = requests.get(host_url+request_url.replace("entity",entity))
	result= response.json()
	resource_group= result['results'][0]['resourceGroup']
	provider_id= result['results'][0]['provider']

	insert_query ="""UPDATE subscriptions set provider_id=%s,resource_group=%s,delegator_id= %s,item_type=%s where _id = %s and queue_name= %s"""
	val= (provider_id,resource_group,user_id,"RESOURCE",_id,queue_name)
	print(insert_query)
	print(val)
	cur.execute(insert_query,val)

	conn.commit()
	
# Closing the connection
conn.close()
