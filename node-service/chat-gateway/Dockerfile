FROM docker.m.daocloud.io/library/node:22-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci --omit=dev || npm install --omit=dev
COPY server.js ./
COPY lib ./lib
EXPOSE 3010
CMD ["node", "server.js"]
