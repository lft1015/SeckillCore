ARG APP_DIR

FROM node:20-alpine AS build
ARG APP_DIR

WORKDIR /workspace/${APP_DIR}

# 复制前端项目所有源文件（node_modules 和 dist 已被 .dockerignore 排除）
COPY ${APP_DIR}/ ./

# 安装依赖并构建
RUN npm install && npm run build

FROM nginx:1.27-alpine
ARG APP_DIR

COPY deploy/docker/frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/${APP_DIR}/dist /usr/share/nginx/html

EXPOSE 80