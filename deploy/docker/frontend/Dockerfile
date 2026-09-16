ARG APP_DIR

FROM node:20-alpine AS build
ARG APP_DIR

WORKDIR /workspace/${APP_DIR}
COPY ${APP_DIR}/package.json ${APP_DIR}/package-lock.json ./
RUN npm ci
COPY ${APP_DIR}/ ./
RUN npm run build

FROM nginx:1.27-alpine
ARG APP_DIR

COPY deploy/docker/frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/${APP_DIR}/dist /usr/share/nginx/html

EXPOSE 80
