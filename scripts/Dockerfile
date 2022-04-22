FROM python:3.8-slim
WORKDIR /usr/share/app
COPY requirements.txt .

RUN apt-get update \
    && apt-get -y install libpq-dev gcc \
    && pip install -r requirements.txt

COPY delete-subs.py .
RUN useradd -r -u 1001 -g root rs-refresh-user
USER rs-refresh-user
CMD ["python","delete-subs.py"] 
