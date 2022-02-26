FROM node:erbium-alpine
# WORKDIR /usr/src/app

# Override the base log level (info).
ENV NPM_CONFIG_LOGLEVEL warn

WORKDIR /usr/src/app/packages/api

# Install npm dependencies first (so they may be cached if dependencies don't change)
COPY package.json .
RUN yarn install --production

COPY build build

# RUN

EXPOSE 4545

CMD yarn start:prod
