FROM python:3.13-alpine

WORKDIR /app
ADD requirements.txt /app/requirements.txt
RUN pip install -r /app/requirements.txt --break-system-packages

ADD src /app

ENTRYPOINT ["python", "/app/main.py"]

