import psycopg2
import json
import requests
import logging
from requests.auth import HTTPBasicAuth

#reading configuration file from system
with open("adaptors_details_migration_config.json") as file:
    config=json.load(file)

# init local variables
postgersDatabaseName = config["postgersDatabaseName"]
postgersUser = config["postgersUser"]
postgersPassword = config["postgersPassword"]
postgersHost = config["postgersHost"]
postgersPort = config["postgersPort"]
host_url_db = config["host_url_db"]
host_url_cache = config["host_url_cache"]
vhost = config["vhost"]
dataBrokerUserName = config['dataBrokerUserName']
dataBrokerPassword = config['dataBrokerPassword']
logLevel = "DEBUG"

request_url_cache = "/iudx/cat/v1/search?property=[id]&value=[[entity]]&filter=[id,provider,name,description,authControlGroup,accessPolicy,iudxResourceAPIs,instance]"
request_url_db = "/api/exchanges/Vhost"
request_url_permission = "/api/permissions"

# connecting with postgers
conn = psycopg2.connect(host=postgersHost, database=postgersDatabaseName, user=postgersUser, password=postgersPassword,port=postgersPort)
cur = conn.cursor()

# Loglevel string to number conversion
logDict={'ERROR':40,'CRITICAL':50, 'WARNING':30, 'INFO':20, 'DEBUG':10}
#setting logging configs
logging.basicConfig(format='%(asctime)s %(levelname)-8s %(message)s',
                    level=logDict[logLevel],
                    datefmt='%Y-%m-%d %H:%M:%S')

# Dict containing exchange_name as key and list of userId as value:
exchangeName_userId = {}

# Flow to get the user_id for a given exchange_name
response_permission = requests.get(host_url_db+request_url_permission,auth=HTTPBasicAuth(dataBrokerUserName,dataBrokerPassword))
permission_result = response_permission.json()

# flow to fill the dict with exchange name and list of user id
for result in permission_result:
    if result['vhost']=="IUDX":
        listOfExchangeName = result['write'].split("|")
        for exchangeName in listOfExchangeName:
            if(len(exchangeName.split('/'))>=4):
                tempList = []
                if exchangeName in exchangeName_userId:
                    tempList = exchangeName_userId.get(exchangeName)
                    tempList.append(result['user'])
                else:
                    tempList.append(result['user'])
                exchangeName_userId.update({exchangeName:tempList})


# Function to insert data into adaptor_details table
def insertInDB(cache_result,dbjson):
    dataset_name= cache_result['results'][0]['name']
    dataset_json= cache_result['results'][0]
    if exchange_name in exchangeName_userId:
        userId = (exchangeName_userId.get(exchange_name))[0]
    else:
        userId = dbjson['user_who_performed_action']
    insert_query ="""INSERT INTO ad (dataset_name,dataset_details_json,user_id,exchange_name,resource_id) VALUES (%s,%s,%s,%s,%s)"""
    val= (dataset_name,json.dumps(dataset_json),userId,exchange_name,exchange_name)
    cur.execute(insert_query,val)
    conn.commit()

# Flow to get data from databroker followed by getting data from cat cache and inserting into database
response_db = requests.get(host_url_db+request_url_db.replace("Vhost",vhost),auth=HTTPBasicAuth(dataBrokerUserName, dataBrokerPassword))
for dbjson in response_db.json():
    array = dbjson['name'].split("/")
    if len(array) >= 4 :
        exchange_name = dbjson['name']
        response_cache = requests.get(host_url_cache+request_url_cache.replace("entity",exchange_name))
        cache_result = response_cache.json()
        if(len(cache_result["results"])==0):
            logging.debug("Empty result for the ID : "+exchange_name)
        else :
            insertInDB(cache_result,dbjson)


# connecting with postgers
conn = psycopg2.connect(host=postgersHost, database=postgersDatabaseName, user=postgersUser, password=postgersPassword,port=postgersPort)
cur = conn.cursor()

# Closing the connection
conn.close()


