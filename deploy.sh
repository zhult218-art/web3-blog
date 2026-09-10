#!/bin/bash
set -e

echo "Starting infrastructure..."
docker compose up -d mysql redis rabbitmq nacos

echo "Waiting for nacos..."
for i in {1..60}; do
  if curl -s http://127.0.0.1:8848/nacos/v1/console/health/readiness >/dev/null; then
    echo "Nacos is ready"
    break
  fi
  sleep 2
done

echo "Starting backend services..."
docker compose up -d gateway user-service blog-service forum-service shop-service media-service quant-service tool-service software-service

echo "Starting frontend dev server..."
cd frontend
npm install
npm run dev
