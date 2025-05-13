FROM node:22-alpine AS builder

WORKDIR /app
ADD package*.json /app
ADD tsconfig.json /app

RUN npm i --include=dev

ADD src /app/src

RUN npm run bundle

FROM node:22-alpine AS dist

WORKDIR /app
COPY --from=builder /app/package*.json /app
RUN npm i

COPY --from=builder /app/dist/index.js /app/dist/index.js
COPY --from=builder /app/dist/sourcemap-register.js /app/dist/sourcemap-register.js

ENTRYPOINT ["npm", "start"]
