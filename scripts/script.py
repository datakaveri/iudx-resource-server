import requests
import json
import time
import psycopg2
import schedule
from urllib.parse import quote

from datetime import datetime as dt

host = ''
port = 0
user = ''
passwd = ''
vhost=''



def unbind():  

    #connection with the database
    conn = psycopg2.connect(dbname="",user="",password="",host="",port="")

    # Open a cursor to perform database operations
    cur = conn.cursor()

    #query to get all the queues and exchange present
    postgreSQL_select_query="Select queue_name,entity from subscriptions where expiry < timestamp %s"
    # Execute a query
    time=dt.now()
    cur.execute(postgreSQL_select_query,(time,))
    records = cur.fetchall()

    #to unbind the queue and exchange for a routing key
    def unbind_queue_exchange(host, port, user, passwd,vhost, exchange, queue, prop):
      url = 'https://%s:%s/api/bindings/%s/e/%s/q/%s/%s' % (host, port,vhost,exchange,queue,prop)
      r=requests.delete(url,auth=(user,passwd))
      #if we have to delete the queue as well uncomment below code
      #url = 'https://%s:%s/api/queues/%s/%s' % (host, port,vhost,queue)
      #print(url)
      #r=requests.delete(url,auth=(user,passwd))
      return

    #to get list of binding between a queue and an exchange
    def call_rabbitmq_api(host, port, user, passwd,vhost, exchange, queue):
      url = 'https://%s:%s/api/bindings/%s/e/%s/q/%s' % (host, port,vhost,exchange,queue)
      r = requests.get(url, auth=(user,passwd))
      response=json.loads(r.text)
      for prop in response:
        temp_prop=prop['properties_key']
        unbind_queue_exchange(host,port,user,passwd,vhost,exchange,queue,temp_prop)
      return

    queue_name=[]
    for row in records:
      queue_name.append(row[0])
      res = call_rabbitmq_api(host, port, user, passwd,vhost,quote(row[1],safe=''),quote(row[0],safe=''))

    postgreSQL_delete_query="DELETE from subscriptions where queue_name IN {} and expiry < timestamp %s;"
    # Retrieve query results
    records = cur.fetchall()
            
    all_queues="('"+"','".join(queue_name)+"')"
    temp_query=cur.mogrify(postgreSQL_delete_query.format(all_queues))
    cur.execute(temp_query,(time,))
    conn.commit()

    return

schedule.every(60).minutes.do(unbind)

# Loop so that the scheduling task
# keeps on running all time.
while True:
  
    # Checks whether a scheduled task 
    # is pending to run or not
    schedule.run_pending()

    
