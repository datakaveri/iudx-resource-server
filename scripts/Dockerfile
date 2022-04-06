FROM python:3.8-slim

RUN apt-get update \
    && apt-get -y install libpq-dev gcc \
    && pip install requests psycopg2 schedule

COPY script.py /home/

CMD ["python","/home/script.py"]