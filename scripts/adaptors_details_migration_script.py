import psycopg2
import json
import requests
import logging
from requests.auth import HTTPBasicAuth

#reading configuration file from system
with open("config-file-path.json") as file:
    config=json.load(file)
#init local variables

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

# connecting with postgers
conn = psycopg2.connect(host=postgersHost, database=postgersDatabaseName, user=postgersUser, password=postgersPassword,port=postgersPort)
cur = conn.cursor()

# Loglevel string to number conversion
logDict={'ERROR':40,'CRITICAL':50, 'WARNING':30, 'INFO':20, 'DEBUG':10}
#setting logging configs
logging.basicConfig(format='%(asctime)s %(levelname)-8s %(message)s',
        level=logDict[logLevel],
       datefmt='%Y-%m-%d %H:%M:%S') 



response_db = requests.get(host_url_db+request_url_db.replace("Vhost",vhost),auth=HTTPBasicAuth(dataBrokerUserName, dataBrokerPassword))

def insertInDB(cache_result,dbjson):
          dataset_name= cache_result['results'][0]['name']
          dataset_json= cache_result['results'][0]
          user_id= dbjson['user_who_performed_action']
          print("userId"+user_id)
          insert_query ="""INSERT INTO adaptors_details (dataset_name,dataset_details_json,user_id,exchange_name,resource_id) VALUES (%s,%s,%s,%s,%s)"""
          val= (dataset_name,json.dumps(dataset_json),user_id,exchange_name,exchange_name)
          cur.execute(insert_query,val)
          conn.commit()


for dbjson in response_db.json():
    array = dbjson['name'].split("/")
    print(dbjson['name'])
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



