const api = require('../../utils/api.js')

Page({
    data: {
        userInfo: null,
        levelId: 3,
        scenarios: [],
        currentIndex: 0,
        answers: {},
        showResult: false,
        result: null,
        optionLabels: ['A', 'B', 'C', 'D']
    },

    onLoad(options) {
        const userInfo = wx.getStorageSync('userInfo')
        if (!userInfo || !userInfo.id) {
            wx.redirectTo({ url: '/pages/login/login' })
            return
        }

        this.setData({
            userInfo,
            levelId: options.levelId || 3
        })

        // 从后端同步最新积分
        this.refreshScoreFromServer()
        this.startLevel()
    },

    async refreshScoreFromServer() {
        const userInfo = wx.getStorageSync('userInfo')
        if (!userInfo || !userInfo.id) return
        try {
            const res = await api.getUserInfo(userInfo.id)
            if (res.code === 200 && res.data) {
                const fresh = { ...userInfo, totalScore: res.data.totalScore || 0 }
                wx.setStorageSync('userInfo', fresh)
                this.setData({ userInfo: fresh })
            }
        } catch (e) {}
    },

    goBack() {
        wx.navigateBack()
    },


    async startLevel() {
        wx.showLoading({ title: '加载中' })

        try {
            const res = await api.startScenarioLevel(this.data.userInfo.id, this.data.levelId)
            console.log('=== startScenarioLevel 响应 ===', JSON.stringify(res))
            console.log('=== 响应data ===', JSON.stringify(res.data))

            if (res.code === 200 && res.data && res.data.scenarios) {
                console.log('=== scenarios数组 ===', JSON.stringify(res.data.scenarios))

                if (res.data.scenarios.length > 0) {
                    console.log('=== 第一道题完整数据 ===', JSON.stringify(res.data.scenarios[0]))
                    console.log('=== 第一道题的image字段值 ===', res.data.scenarios[0].image)
                    console.log('=== 第一道题是否有image字段 ===', 'image' in res.data.scenarios[0])
                }

                const scenarios = res.data.scenarios.map((s, idx) => {
                    console.log('=== 题目' + idx + '原始数据 ===', JSON.stringify(s))
                    console.log('=== 题目' + idx + '的image字段 ===', s.image)
                    console.log('=== 题目' + idx + 'image字段类型 ===', typeof s.image)

                    let options = []
                    try {
                        if (s.options === null || s.options === undefined) {
                            console.warn('第' + idx + '题options字段为null/undefined')
                            options = []
                        } else if (typeof s.options === 'string') {
                            if (s.options === '' || s.options === 'null' || s.options === '[]') {
                                console.warn('第' + idx + '题options为空字符串或无效值')
                                options = []
                            } else {
                                options = JSON.parse(s.options)
                            }
                        } else if (Array.isArray(s.options)) {
                            options = s.options
                        } else if (s.options) {
                            options = [String(s.options)]
                        }
                    } catch (e) {
                        console.error('第' + idx + '题选项解析失败:', s.options, e)
                        options = ['选项解析失败']
                    }

                    if (!Array.isArray(options) || options.length === 0) {
                        console.error('❌ 第' + idx + '题（ID:' + s.id + '）选项数据完全缺失！')
                        options = ['该题目选项数据缺失，请联系管理员']
                    } else {
                        console.log('✅ 第' + idx + '题选项解析成功，共' + options.length + '个选项')
                    }

                    return {
                        id: s.id,
                        scenario: s.scenario || s.title || '情景描述加载中...',
                        options: options,
                        image: s.image || '',
                        knowledgePoint: s.knowledgePoint || '',
                        answer: s.answer,
                        analysis: s.analysis || ''
                    }
                })

                console.log('=== 解析后scenarios[0] ===', JSON.stringify(scenarios[0]))
                console.log('=== 解析后scenarios[0].image ===', scenarios[0].image)

                this.setData({
                    scenarios: scenarios,
                    currentIndex: 0,
                    answers: {}
                }, () => {
                    console.log('=== setData后的currentScenario ===', JSON.stringify(this.data.scenarios[this.data.currentIndex]))
                    console.log('=== currentScenario.image ===', this.data.scenarios[this.data.currentIndex].image)
                })
            } else {
                wx.showToast({ title: (res && res.msg) || '加载失败', icon: 'none' })
            }
        } catch (err) {
            console.error('加载失败', err)
            wx.showToast({ title: '加载失败，请检查网络', icon: 'none' })
        } finally {
            wx.hideLoading()
        }
    },


    selectAnswer(e) {
        const { scenarioIndex, answerIndex } = e.currentTarget.dataset
        console.log('=== selectAnswer ===', { scenarioIndex, answerIndex, currentIndex: this.data.currentIndex, scenariosLen: this.data.scenarios.length })

        const scenario = this.data.scenarios[scenarioIndex]

        if (!scenario) {
            console.warn('selectAnswer: 未找到 scenario, scenarioIndex=', scenarioIndex)
            return
        }
        if (!scenario.options || scenario.options.length === 0) {
            console.warn('selectAnswer: scenario.options 为空')
            return
        }

        const answers = { ...this.data.answers }
        answers[scenario.id] = this.data.optionLabels[answerIndex]

        this.setData({ answers })

        if (scenarioIndex < this.data.scenarios.length - 1) {
            setTimeout(() => {
                this.setData({ currentIndex: scenarioIndex + 1 })
            }, 500)
        } else {
            setTimeout(() => {
                this.submitAnswer()
            }, 500)
        }
    },

    async submitAnswer() {
        wx.showLoading({ title: '提交中' })

        try {
            const res = await api.submitScenarioLevel(
                this.data.userInfo.id,
                this.data.levelId,
                JSON.stringify(this.data.answers)
            )

            if (res.code === 200) {
                // 积分全部由后端计算和累加，前端直接用后端返回的最新 totalScore
                const newTotalScore = (res.data && res.data.totalScore != null) ? res.data.totalScore
                    : ((this.data.userInfo.totalScore || 0) + ((res.data && res.data.score) || 0))
                const userInfo = { ...this.data.userInfo, totalScore: newTotalScore }
                wx.setStorageSync('userInfo', userInfo)

                this.setData({
                    showResult: true,
                    result: res.data,
                    userInfo: userInfo
                })
            } else {
                wx.showToast({ title: res.msg || '提交失败', icon: 'none' })
            }
        } catch (err) {
            console.error('提交失败', err)
            wx.showToast({ title: '提交失败', icon: 'none' })
        } finally {
            wx.hideLoading()
        }
    },

    backToHome() {
        wx.navigateBack()
    },

    restartLevel() {
        this.setData({
            showResult: false,
            result: null
        })
        this.startLevel()
    }
})
