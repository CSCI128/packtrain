FROM node:lts-slim AS builder

ADD . /openapi

RUN npm i -g @redocly/cli@latest

RUN redocly bundle /openapi/openapi.yml -o /openapi/bundled.yml

RUN redocly build-docs /openapi/bundled.yml -o /openapi/api.html

FROM nginx:alpine AS serve

COPY --from=builder /openapi/api.html /usr/share/nginx/html/index.htmlred