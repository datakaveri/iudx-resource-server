import json
import logging
from immudb import ImmudbClient
from datetime import datetime
from time import strftime, localtime
import pytz
import psycopg2


class MigrateAuditingData:
    def insertData(self, postgresClient, postgresTable, result):
        for item in result:
            id, api, userid, epochtime, isotime, resourceid, providerid, size = item
            formatted_isotime = isotime.replace("[Asia/Kolkata]", "")
            try:
                timestamp = datetime.fromisoformat(formatted_isotime)
            except:
                timestamp = strftime("%Y-%m-%d %H:%M:%S", localtime(epochtime / 1000))
                timestamp = datetime.fromisoformat(timestamp)
            timestamp = timestamp.astimezone(pytz.utc)
            timestamp = timestamp.replace(tzinfo=None)
            timestamp = timestamp.strftime("%Y-%m-%d %H:%M:%S")
            params = ( id, api, userid, epochtime, isotime, resourceid, providerid, size, timestamp,)

            cur = postgresClient.cursor()
            resp = cur.execute(
                "INSERT INTO {} (id, api, userid, epochtime, isotime, resourceid, providerid,size,time) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s) ON CONFLICT DO NOTHING;".format(
                    postgresTable
                ),
                params,
            )

        postgresClient.commit()
        logging.info("batch inserted")
        return

    def nextBatch( self, immuClient, postgresClient, immudbTable, postgresTable, starttime, endtime, lastId,):
        sqlQuery = "select id, api, userid, epochtime, isotime, resourceid, providerid, size from {} where epochtime<={} and epochtime>{} and id>'{}' order by id limit 999;".format(
            immudbTable, starttime, endtime, lastId
        )
        try:
            result = immuClient.sqlQuery(sqlQuery)
            if len(result) == 0:
                return
            else:
                self.insertData(postgresClient, postgresTable, result)
                lastId, _, _, _, _, _, _, _ = result[len(result) - 1]
                self.nextBatch( immuClient, postgresClient, immudbTable, postgresTable, starttime, endtime, lastId,)
        except Exception as e:
            with open("time_config.json", "r+") as config:
                json.dump({"starttime": starttime, "endtime": endtime}, config)
            logging.error(e)
            logging.info("rerun the script")
            return

    def run( self, immuClient, postgresClient, immudbTable, postgresTable, starttime, endtime):

        # first batch of the day
        firstQuery = "select id, api, userid, epochtime, isotime, resourceid, providerid, size from {} where epochtime<={} and epochtime>{} order by id limit 999;".format(
            immudbTable, starttime, endtime
        )

        try:
            result = immuClient.sqlQuery(firstQuery)
        except Exception as e:
            logging.error(e)
            with open("time_config.json", "r+") as config:
                json.dump({"starttime": starttime, "endtime": endtime}, config)
            logging.info("rerun the script")
            return
        else:

            if len(result) == 0:
                return

            # get the last id from the batch result of current select query
            lastId, _, _, _, _, _, _, _ = result[len(result) - 1]

            # insert the data from old DB into the new DB version Table
            self.insertData(postgresClient, postgresTable, result)

            # get the next batch of upto 1000 records
            self.nextBatch( immuClient, postgresClient, immudbTable, postgresTable, starttime, endtime, lastId,)

        return

    def connectImmuDB(self, config):
        # Client connecting older DB version
        immuClient = ImmudbClient(config["immudbHost"])
        DBOld = config["immudbDatabase"]
        immuClient.login(
            config["immudbUserName"], config["immudbPassword"], database=DBOld
        )
        logging.info("immudb login success")
        return immuClient

    def connectPostgres(self, config):
        conn = psycopg2.connect(
            host=config["postgresHost"],
            database=config["postgresDatabase"],
            user=config["postgresUserName"],
            password=config["postgresPassword"],
            port=config["postgresPort"],
        )
        logging.info("postgres login success")
        return conn


def main():

    logging.basicConfig(
        format="%(asctime)s %(levelname)-8s %(message)s",
        level=logging.DEBUG,
        datefmt="%Y-%m-%d %H:%M:%S",
    )

    with open("immudb_mig_config.json", "r+") as f:
        config = json.load(f)
        starttime = config["starttime"]
        et = starttime - 86400000
        endtime = config["endtime"]
        immudbTable = config["immudbTableName"]
        postgresTable = config["postgresTableName"]

    immuClient = MigrateAuditingData().connectImmuDB(config)
    postgresClient = MigrateAuditingData().connectPostgres(config)

    # Iterate for each day till 13th Sept'21 (no data before that)
    while et >= endtime:
        MigrateAuditingData().run(
            immuClient, postgresClient, immudbTable, postgresTable, starttime, et
        )
        starttime = et
        et = starttime - 86400000


if __name__ == "__main__":
    main()

