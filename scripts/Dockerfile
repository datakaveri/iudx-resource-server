FROM python:3.8-slim

COPY requirements.txt /home/

RUN apt-get update \
    && apt-get -y install libpq-dev gcc \
    && pip install -r /home/requirements.txt

COPY delete-subs.py /home/

CMD ["python","/home/delete-subs.py"] 
