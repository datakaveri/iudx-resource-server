# install python3 in your system before proceeding further.
# before runnig script install all dependency by using requirements.txt file
#       $> pip install -r requirements.txt
#       $> python3 delete-subs.py
#

import requests
import json
import time
import schedule
import logging
import ssl
import urllib3
from psycopg2 import connect
from urllib.parse import quote
from datetime import timezone,datetime


class DeleteSubscription:

    #function to detch all records from DB to be deleted [expiry less than current time]
    def fetchSubsFromDB(self,pgConnection):
        logging.debug("fetching records from DB")
        cur = pgConnection.cursor()
        #query to get all the queues and exchange present
        postgreSQL_select_query="Select queue_name,entity from subscriptions where expiry < timestamp %s"
        # Execute a query
        time=datetime.now(tz=timezone.utc)
        selectSql=cur.mogrify(postgreSQL_select_query,(time,))
        logging.debug(selectSql)
        cur.execute(selectSql)
        records = cur.fetchall()
        logging.debug(records)
        logging.debug("fetched records from DB")
        return records
    
    #function to get list of binding between a queue and an exchange
    def call_rabbitmq_api(self,dataBrokerHost, dataBrokerPort, dataBrokerUser, dataBrokerPassword,dataBrokerVhost, exchange, queue):
        url = 'https://%s:%s/api/bindings/%s/e/%s/q/%s' % (dataBrokerHost, dataBrokerPort,dataBrokerVhost,exchange,queue)
        logging.debug("getting binding details")
        r = requests.get(url, auth=(dataBrokerUser,dataBrokerPassword))
        response=json.loads(r.text)
        logging.debug("received binding details : "+str(response))
        for prop in response:
            routing_key=prop['properties_key']
            logging.debug("routing key for unbinding : "+str(routing_key))
            self.unbind_queue_exchange(dataBrokerHost,dataBrokerPort,dataBrokerUser,dataBrokerPassword,dataBrokerVhost,exchange,queue,routing_key)
        logging.debug("All unbindings done")
        return

    #function to unbind the queue and exchange for a routing key
    def unbind_queue_exchange(self,dataBrokerHost, dataBrokerPort, dataBrokerUser, dataBrokerPassword,dataBrokerVhost, exchange, queue, routing_key):
        logging.debug("unbinding queue - %s from exchange - %s with rouging key - %s : ",queue,exchange,routing_key)
        url = 'https://%s:%s/api/bindings/%s/e/%s/q/%s/%s' % (dataBrokerHost, dataBrokerPort,dataBrokerVhost,exchange,queue,routing_key)
        r=requests.delete(url,auth=(dataBrokerUser,dataBrokerPassword))
        logging.debug("unbinding done for queue - %s from exchange - %s with routing key - %s",queue,exchange,routing_key)
        return

    #function to extract exchange name from entity column of DB
    def getExchangeFromName(self,entity):
        exchange=entity
        idComponetsList=entity.split("/")
        if(len(idComponetsList)==5):
            exchange=entity.rsplit('/', 1)[0]
        return exchange

    #main function executed every [scheduledTime] minutes.
    def run(self):
        logging.info("starting delete script")
        #connection with the database
        pgConnection = connect(
            database=dataBaseName,
            user=dataBaseUser,
            password=dataBasePassword,
            host=dataBaseHost,
            port=dataBasePort,
            connect_timeout=3,
            keepalives=1,
            keepalives_idle=5,
            keepalives_interval=2,
            keepalives_count=2)
        logging.debug("connected to database.")
        records=self.fetchSubsFromDB(pgConnection)
        queue_name=[]

        #getting queue and exchange name from database response one at a time
        logging.debug("records to delete : "+str(records))
        if not records:
            logging.debug("nothing to delete")
        else:
            for row in records:
                queue=row[0] 
                exchange=self.getExchangeFromName(row[1])
                queue_name.append(queue)
                logging.debug("deleting queue : %s to exchange : %s binding",queue,exchange)
                self.call_rabbitmq_api(dataBrokerHost, dataBrokerPort, dataBrokerUser, dataBrokerPassword,dataBrokerVhost,quote(exchange,safe=''),quote(queue,safe=''))
                logging.debug("deleted queue : %s to exchange : %s binding",queue,exchange)

            logging.debug("unbindings done")
            logging.debug("deleting records from DB")
            cur = pgConnection.cursor()
            postgreSQL_delete_query="DELETE from subscriptions where queue_name IN {} and expiry < timestamp %s;"
            # Retrieve query results
            #records = cur.fetchall()
                
            all_queues="('"+"','".join(queue_name)+"')"
            temp_query=cur.mogrify(postgreSQL_delete_query.format(all_queues))
            time=datetime.now(tz=timezone.utc)
            cur.execute(temp_query,(time,))
            pgConnection.commit()
            logging.debug("deleting records from DB done")
            pgConnection.close()
        logging.info("script execution completed")
        return




#setting logging configs
logging.basicConfig(format='%(asctime)s %(levelname)-8s %(message)s',
        level=logging.DEBUG,
        datefmt='%Y-%m-%d %H:%M:%S')

#reading configuration file from system
with open("/home/script-config.json") as file:
  config=json.load(file)


urllib3.PoolManager(
    ssl_minimum_version=ssl.TLSVersion.TLSv1_2
)

#init local variables
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

scheduleTime = config["schedule_time"]

#create a scheduler to run every <scheduleTime> minutes
schedule.every(scheduleTime).minutes.do(lambda : DeleteSubscription().run())

while True:
    # Checks whether a scheduled task
    # is pending to run or not
    schedule.run_pending()
    time.sleep(5)
    
    
    
