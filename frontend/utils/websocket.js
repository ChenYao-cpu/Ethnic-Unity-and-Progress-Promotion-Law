const { BASE_URL } = require('./config.js')

// WebSocket管理器
class WebSocketManager {
    constructor() {
        this.socketTask = null
        this.isConnected = false
        this.userId = null
        this.messageHandlers = {}
        this.reconnectTimer = null
        this.heartbeatTimer = null
        this.reconnectAttempts = 0
        this.maxReconnectAttempts = 5
    }

    // 连接WebSocket
    connect(userId) {
        return new Promise((resolve, reject) => {
            if (this.socketTask && this.isConnected) {
                console.log('WebSocket已连接')
                resolve()
                return
            }

            this.userId = userId

            // 从 HTTP 地址推导 WebSocket 地址
            const wsUrl = BASE_URL
                .replace(/^http/, 'ws')
                .replace(/\/api$/, '') + '/ws/battle/' + userId

            console.log('正在连接WebSocket:', wsUrl)

            this.socketTask = wx.connectSocket({
                url: wsUrl,
                success: () => {
                    console.log('WebSocket连接请求已发送')
                },
                fail: (err) => {
                    console.error('WebSocket连接失败:', err)
                    reject(err)
                }
            })

            // 监听连接打开
            this.socketTask.onOpen(() => {
                console.log('WebSocket连接已建立')
                this.isConnected = true
                this.reconnectAttempts = 0

                // 发送用户信息
                this.send({
                    type: 'LOGIN',
                    userId: userId
                })

                // 启动心跳
                this.startHeartbeat()
                resolve()
            })

            // 监听消息
            this.socketTask.onMessage((res) => {
                try {
                    const data = JSON.parse(res.data)
                    console.log('收到WebSocket消息:', data)
                    this.handleMessage(data)
                } catch (e) {
                    console.error('解析WebSocket消息失败:', e)
                }
            })

            // 监听错误
            this.socketTask.onError((err) => {
                console.error('WebSocket错误:', err)
                this.isConnected = false
                this.reconnect()
            })

            // 监听关闭
            this.socketTask.onClose(() => {
                console.log('WebSocket连接已关闭')
                this.isConnected = false
                this.stopHeartbeat()
                this.reconnect()
            })
        })
    }

    // 发送消息
    send(message) {
        if (!this.socketTask || !this.isConnected) {
            console.error('WebSocket未连接，无法发送消息')
            return
        }

        const messageStr = JSON.stringify(message)
        this.socketTask.send({
            data: messageStr,
            success: () => {
                console.log('消息发送成功:', message.type)
            },
            fail: (err) => {
                console.error('消息发送失败:', err)
            }
        })
    }

    // 注册消息处理器
    on(type, handler) {
        if (!this.messageHandlers[type]) {
            this.messageHandlers[type] = []
        }
        this.messageHandlers[type].push(handler)
    }

    // 处理消息
    handleMessage(data) {
        const handlers = this.messageHandlers[data.type]
        if (handlers) {
            handlers.forEach(handler => handler(data))
        }
    }

    // 开始匹配
    startMatching() {
        this.send({
            type: 'MATCH_START'
        })
    }

    // 取消匹配
    cancelMatching() {
        this.send({
            type: 'MATCH_CANCEL'
        })
    }

    // 提交答案
    submitAnswer(questionIndex, answerIndex) {
        this.send({
            type: 'ANSWER_SUBMIT',
            questionIndex: questionIndex,
            answerIndex: answerIndex
        })
    }

    // 心跳
    startHeartbeat() {
        this.heartbeatTimer = setInterval(() => {
            this.send({
                type: 'HEARTBEAT'
            })
        }, 30000) // 30秒一次心跳
    }

    stopHeartbeat() {
        if (this.heartbeatTimer) {
            clearInterval(this.heartbeatTimer)
            this.heartbeatTimer = null
        }
    }

    // 重连
    reconnect() {
        if (this.reconnectAttempts >= this.maxReconnectAttempts) {
            console.log('重连次数已达上限')
            return
        }

        if (this.reconnectTimer) {
            clearTimeout(this.reconnectTimer)
        }

        this.reconnectAttempts++
        const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000)

        console.log(`将在 ${delay}ms 后尝试第 ${this.reconnectAttempts} 次重连`)

        this.reconnectTimer = setTimeout(() => {
            if (this.userId) {
                this.connect(this.userId)
            }
        }, delay)
    }

    // 断开连接
    disconnect() {
        this.stopHeartbeat()

        if (this.reconnectTimer) {
            clearTimeout(this.reconnectTimer)
            this.reconnectTimer = null
        }

        if (this.socketTask) {
            this.socketTask.close({
                success: () => {
                    console.log('WebSocket连接已关闭')
                }
            })
            this.socketTask = null
        }

        this.isConnected = false
        this.messageHandlers = {}
    }
}

// 创建全局单例
const wsManager = new WebSocketManager()

module.exports = {
    WebSocketManager,
    wsManager
}
