const { askAI, getChatHistory, interpretLaw } = require('../../utils/api.js')

Page({
    data: {
        score: 0,
        messages: [],
        inputText: '',
        loading: false,
        userId: null,
        mode: 'qa',        // 'qa': 智能问答, 'interpret': 法条解读
        scrollIntoView: ''
    },

    onLoad() {
        this.loadUserInfo()
        this.loadHistory()
    },

    onShow() {
        this.loadUserInfo()
    },

    goBack() {
        wx.reLaunch({
            url: '/pages/index/index'
        })
    },

    loadHistory() {
        if(!this.data.userId)return;
        getChatHistory(this.data.userId).then(res => {
            if (res.code === 200 && res.data) {
                const messages = res.data.flatMap(item => [
                    { role: 'user', content: item.question, mode: item.mode || 'qa' },
                    { role: 'ai', content: item.answer }
                ])
                this.setData({ messages })
                this.scrollToBottom()
            }
        }).catch(() => {})
    },

    loadUserInfo() {
        const userInfo = wx.getStorageSync('userInfo')
        if (userInfo) {
            this.setData({
                score: userInfo.totalScore || 0,
                userId: userInfo.id
            })
        }
        // 从后端拉最新积分，同步到本地缓存
        this.refreshScoreFromServer()
    },

    // 从后端刷新积分（后端是唯一正确数据源）
    async refreshScoreFromServer() {
        const userInfo = wx.getStorageSync('userInfo')
        if (!userInfo || !userInfo.id) return
        const { getUserInfo } = require('../../utils/api.js')
        try {
            const res = await getUserInfo(userInfo.id)
            if (res.code === 200 && res.data) {
                const fresh = { ...userInfo, totalScore: res.data.totalScore || 0 }
                wx.setStorageSync('userInfo', fresh)
                this.setData({ score: fresh.totalScore || 0 })
            }
        } catch (e) {
            // 后端不可用时用缓存值
        }
    },

    onInput(e) {
        this.setData({ inputText: e.detail.value })
    },

    switchMode(e) {
        const mode = e.currentTarget.dataset.mode  // 'qa' 或 'interpret'
        this.setData({ mode: mode })
        // 可选：显示模式切换提示
        wx.showToast({
            title: mode === 'qa' ? '智能问答' : '法条解读',
            icon: 'none',
            duration: 1000
        })
    },

    askQuestion(e) {
        const question = e.currentTarget.dataset.question
        this.setData({ inputText: question })
        this.sendMessage()
    },

    async sendMessage() {
        const { inputText, userId, mode } = this.data
        if (!inputText.trim()) return

        // 添加用户消息
        this.setData({
            messages: [...this.data.messages, {
                role: 'user',
                content: inputText,
                mode: mode
            }],
            inputText: '',
            loading: true
        })
        this.scrollToBottom()

        try {
            let res
            let answerContent = ''

            if (mode === 'interpret') {
                // 法条解读模式
                res = await interpretLaw(userId, inputText)

                if (res.code === 200 && res.data) {
                    // 后台返回结构化对象 { articleNumber, content, interpretation }
                    if (typeof res.data === 'object' && res.data !== null) {
                        // AI 已生成完整解读，直接展示
                        if (res.data.interpretation) {
                            answerContent = res.data.interpretation
                        } else {
                            answerContent = (res.data.content || '未找到法条内容')
                        }
                    } else if (typeof res.data === 'string') {
                        answerContent = res.data
                    } else {
                        answerContent = '未找到相关法条解读'
                    }
                } else {
                    answerContent = (res && res.msg) || '法条解读失败，请稍后重试'
                }
            } else {
                // 智能问答模式
                res = await askAI(userId, inputText)

                if (res.code === 200) {
                    answerContent = res.data
                } else {
                    answerContent = res.msg || 'AI回答失败，请稍后重试'
                }
            }

            // 添加AI回复消息
            this.setData({
                messages: [...this.data.messages, {
                    role: 'ai',
                    content: answerContent
                }],
                loading: false
            })
            this.scrollToBottom()

        } catch (err) {
            console.error('请求错误:', err)
            wx.showToast({
                title: '网络请求失败',
                icon: 'none'
            })
            this.setData({
                loading: false,
                messages: [...this.data.messages, {
                    role: 'ai',
                    content: '网络请求失败，请检查网络连接。'
                }]
            })
            this.scrollToBottom()
        }
    },

    scrollToBottom() {
        const messages = this.data.messages
        if (messages.length > 0) {
            this.setData({
                scrollIntoView: `msg-${messages.length - 1}`
            })
        }
    }
})