FROM node

WORKDIR /app

COPY . .

WORKDIR /app/grading-admin-frontend

RUN npm install

RUN npx openapi-typescript ../openapi/openapi.yml -o ./apps/grading-admin-web/src/lib/api/v1.d.ts

CMD ["npm", "run", "build"]
