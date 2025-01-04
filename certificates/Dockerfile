FROM debian:bookworm-slim

RUN apt update && \
    apt install -y openssl && \
    apt install -y python3 python3-pip python3-venv libaugeas0

RUN python3 -m venv /opt/certbot/ && \
    /opt/certbot/bin/pip install certbot
ENV PATH=/opt/certbot/bin/:$PATH

ADD --chmod=777 create_certificates.sh /usr/bin/create_certificates.sh

ENTRYPOINT [ "create_certificates.sh" ]