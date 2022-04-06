# below are the perequisite packages :
# pip install requests
# pip install psycopg2
# pip install schedule

import requests
import json
import time
import psycopg2
import schedule
from urllib.parse import quote

from datetime import datetime as dt

with open("script-config.json") as file:
  config=json.load(file)

dataBrokerHost = config["dataBrokerHost"]
dataBrokerPort = config["dataBrokerPort"]
dataBrokerUser = config["dataBrokerUser"]
dataBrokerPassword = config["dataBrokerPassword"]
dataBrokerVhost = config["dataBrokerVhost"]

dataBaseName = config["dataBaseName"]
dataBaseUser = config["dataBaseUser"]
dataBasePassword = config["dataBasePassword"]
dataBaseHost = config["dataBaseHost"]
dataBasePort = config["dataBasePort"]

scheduleTime = config["scheduleTime"]



def unbind():  

    #connection with the database
    conn = psycopg2.connect(dbname=dataBaseName,user=dataBaseUser,password=dataBasePassword,host=dataBaseHost,port=dataBasePort)

    # Open a cursor to perform database operations
    cur = conn.cursor()

    #query to get all the queues and exchange present
    postgreSQL_select_query="Select queue_name,entity from subscriptions where expiry < timestamp %s"
    # Execute a query
    time=dt.now()
    cur.execute(postgreSQL_select_query,(time,))
    records = cur.fetchall()

    #to unbind the queue and exchange for a routing key
    def unbind_queue_exchange(dataBrokerHost, dataBrokerPort, dataBrokerUser, dataBrokerPassword,dataBrokerVhost, exchange, queue, routing_key):
      url = 'https://%s:%s/api/bindings/%s/e/%s/q/%s/%s' % (dataBrokerHost, dataBrokerPort,dataBrokerVhost,exchange,queue,routing_key)
      r=requests.delete(url,auth=(dataBrokerUser,dataBrokerPassword))
      return

    #to get list of binding between a queue and an exchange
    def call_rabbitmq_api(dataBrokerHost, dataBrokerPort, dataBrokerUser, dataBrokerPassword,dataBrokerVhost, exchange, queue):
      url = 'https://%s:%s/api/bindings/%s/e/%s/q/%s' % (dataBrokerHost, dataBrokerPort,dataBrokerVhost,exchange,queue)
      r = requests.get(url, auth=(dataBrokerUser,dataBrokerPassword))
      response=json.loads(r.text)
      for prop in response:
        routing_key=prop['properties_key']
        unbind_queue_exchange(dataBrokerHost,dataBrokerPort,dataBrokerUser,dataBrokerPassword,dataBrokerVhost,exchange,queue,routing_key)
      return

    queue_name=[]
    #getting queue and exchange name from database response one at a time
    for row in records:
      queue=row[0] 
      exchange=row[1]
      queue_name.append(queue)
      res = call_rabbitmq_api(dataBrokerHost, dataBrokerPort, dataBrokerUser, dataBrokerPassword,dataBrokerVhost,quote(exchange,safe=''),quote(queue,safe=''))

    postgreSQL_delete_query="DELETE from subscriptions where queue_name IN {} and expiry < timestamp %s;"
    # Retrieve query results
    records = cur.fetchall()
            
    all_queues="('"+"','".join(queue_name)+"')"
    temp_query=cur.mogrify(postgreSQL_delete_query.format(all_queues))
    cur.execute(temp_query,(time,))
    conn.commit()

    return

schedule.every(scheduleTime).minutes.do(unbind)

# Loop so that the scheduling task
# keeps on running all time.
while True:
  
    # Checks whether a scheduled task 
    # is pending to run or not
    schedule.run_pending()

    
