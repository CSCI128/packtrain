FROM node AS builder

WORKDIR /app

COPY . .

WORKDIR /app/grading-admin-frontend

RUN npm install

RUN npx openapi-typescript ../openapi/openapi.yml -o ./apps/grading-admin-web/src/lib/api/v1.d.ts

RUN npm run build

FROM nginx:alpine AS serve

COPY --from=builder /app/grading-admin-frontend/apps/instructor/nginx.conf /etc/nginx/conf.d/default.conf

COPY --from=builder /app/grading-admin-frontend/apps/instructor/dist/ /usr/share/nginx/html/instructor/

COPY --from=builder /app/grading-admin-frontend/apps/instructor/docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]
