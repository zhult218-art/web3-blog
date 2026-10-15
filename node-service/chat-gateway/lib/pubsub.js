'use strict'
// ============================================================
// 广播总线抽象：为多实例横向扩容预留 Redis PubSub 通道。
//  - 未配置 Redis（REDIS_URL/REDIS_HOST 缺失）：进程内直投，单实例完全可用；
//  - 配置后：本机 socket 照常直投（零延迟），同时把事件 publish 到
//    Redis 频道，其它节点订阅后投递给各自的本地连接，实现集群广播。
// 订阅端按 nodeId 过滤，避免本节点消息二次投递造成重复。
// ============================================================
const os = require('os')
const { randomUUID } = require('crypto')

const NODE_ID = `${os.hostname()}:${process.pid}:${randomUUID().slice(0, 8)}`
const CHANNEL = 'web3:chat:broadcast'

/**
 * @param {(channel: string, payload: object) => void} onRemote
 *        收到其它节点广播时的本地投递回调
 */
function createBus(onRemote) {
  const redisHost = process.env.REDIS_HOST
  const redisUrl = process.env.REDIS_URL
  if (!redisUrl && !redisHost) {
    console.log('[bus] 未配置 Redis，使用进程内广播（单实例模式）')
    return {
      mode: 'memory',
      async publish() { /* 单实例无需跨节点转发，本地直投已由调用方完成 */ },
      async close() {},
    }
  }

  const Redis = require('ioredis')
  const opts = redisUrl
    ? redisUrl
    : { host: redisHost, port: Number(process.env.REDIS_PORT || 6379), password: process.env.REDIS_PASSWORD || undefined }
  const pub = new Redis(opts)
  const sub = new Redis(opts)

  sub.subscribe(CHANNEL).then(
    () => console.log(`[bus] Redis PubSub 已启用（channel=${CHANNEL}）`),
    err => console.error('[bus] Redis 订阅失败，降级为单实例广播:', err.message)
  )
  sub.on('message', (_channel, raw) => {
    try {
      const envelope = JSON.parse(raw)
      if (envelope.nodeId === NODE_ID) return   // 本节点已直投，忽略回声
      onRemote(envelope.channel, envelope.payload)
    } catch { /* 非法消息忽略 */ }
  })
  pub.on('error', e => console.error('[bus] Redis 连接错误:', e.message))
  sub.on('error', e => console.error('[bus] Redis 连接错误:', e.message))

  return {
    mode: 'redis',
    nodeId: NODE_ID,
    async publish(channel, payload) {
      try {
        await pub.publish(CHANNEL, JSON.stringify({ nodeId: NODE_ID, channel, payload }))
      } catch (e) {
        console.error('[bus] publish 失败:', e.message)
      }
    },
    async close() { await Promise.allSettled([pub.quit(), sub.quit()]) },
  }
}

module.exports = { createBus, NODE_ID }
