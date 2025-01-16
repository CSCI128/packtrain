FROM node:lts-slim AS builder

ADD . /openapi

RUN npm i -g @redocly/cli@latest

RUN redocly build-docs /openapi/openapi.yml -o /openapi/api.html

FROM nginx:alpine AS serve

COPY --from=builder /openapi/api.html /usr/share/nginx/html/index.html