import psycopg2
import json
import requests
import logging

#reading configuration file from system
with open("config-file-path") as file:
    config=json.load(file)


#init local variables

postgersDatabaseName = config["postgersDatabaseName"]
postgersUser = config["postgersUser"]
postgersPassword = config["postgersPassword"]
postgersHost = config["postgersHost"]
postgersPort = config["postgersPort"]
host_url = config["host_url"]
logLevel = "DEBUG"

request_url = "/iudx/cat/v1/search?property=[id]&value=[[entity]]&filter=[id,provider,name,description,authControlGroup,accessPolicy,iudxResourceAPIs,instance]"

# connecting with postgers
conn = psycopg2.connect(host=postgersHost, database=postgersDatabaseName, user=postgersUser, password=postgersPassword,port=postgersPort)
cur = conn.cursor()

# get data from subscriptions table in postgers
select_stmt = "select _id,queue_name,entity from testing_subscriptions;"
cur.execute(select_stmt)
records=cur.fetchall()

for data in records:
	_id=data[0]
	print(_id)
	queue_name=data[1]
	entity= data[2]
	user_id = queue_name.split("/")[0]
	response = requests.get(host_url+request_url.replace("entity",entity))
	result= response.json()
	dataset_name= result['results'][0]['name']
	dataset_json= result['results'][0]
	insert_query ="""UPDATE subscriptions set dataset_name=%s,dataset_json=%s,user_id= %s where _id = %s and queue_name= %s"""
	val= (dataset_name,json.dumps(dataset_json),user_id,_id,queue_name)
	cur.execute(insert_query,val)
	
	conn.commit()
	
# Closing the connection
conn.close()  

