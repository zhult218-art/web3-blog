FROM docker.m.daocloud.io/library/python:3.11-slim
WORKDIR /app
# 仅复制运行所需文件：避免把本地 .venv / 日志 / __pycache__ 打进镜像与构建上下文
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt
COPY main.py ./
COPY core ./core
COPY modules ./modules
RUN mkdir -p data output
EXPOSE 9006
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "9006"]
