FROM node:16-bullseye AS build

WORKDIR /app
COPY ruoyi-ui/package.json ./
RUN npm config set registry https://registry.npmmirror.com && npm install --legacy-peer-deps

COPY ruoyi-ui/ ./
ENV VUE_APP_BASE_API=/prod-api \
    VUE_APP_WS_API=ws://localhost:9666 \
    VUE_APP_MONITRO_ADMIN=/admin/login \
    VUE_APP_XXL_JOB_ADMIN=/xxl-job-admin

RUN npm run build:prod

FROM nginx:1.22.1-alpine

COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
