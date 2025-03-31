FROM python:3.11-slim AS builder

WORKDIR /docs

RUN apt-get update && apt-get install -y make gcc libssl-dev

COPY requirements.txt .

RUN pip install --no-cache-dir -r requirements.txt

COPY . .

RUN make html

FROM nginx:alpine AS serve

COPY --from=builder /docs/_build/html /usr/share/nginx/html
