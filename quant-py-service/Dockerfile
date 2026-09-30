FROM python:3.11-slim
WORKDIR /app

# 系统依赖：numpy/pandas/akshare 编译需要的二进制库
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc g++ libffi-dev libssl-dev \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 9006
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "9006"]
