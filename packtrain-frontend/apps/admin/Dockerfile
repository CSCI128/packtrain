FROM node AS builder

WORKDIR /app

COPY . .

WORKDIR /app/packtrain-frontend

RUN npm install

RUN npm run build

FROM nginx:alpine AS serve

COPY --from=builder /app/packtrain-frontend/apps/admin/nginx.conf /etc/nginx/conf.d/default.conf

COPY --from=builder /app/packtrain-frontend/apps/admin/dist/ /usr/share/nginx/html/

COPY --from=builder /app/packtrain-frontend/apps/admin/docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]
