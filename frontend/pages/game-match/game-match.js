const legalMatchData = require('../../utils/legal-match-data.js')

Page({
    data: {
        levelId: 1,
        score: 0,
        items: [],
        options: [],
        matchedPairs: [],
        selectedId: null,
        startTime: 0,
        timeUsed: 0,
        timer: null,
        showResult: false,
        result: null,
        allMatched: false,
        showAllComplete: false,
        isFullScore: false,
        totalScore: 0,
        displayScore: '',
        totalTimeText: '',
        bestScore: 0,
        completedLevels: [],
        totalTime: 0,
        totalCorrectCount: 0
    },

    onLoad(options) {
        const userInfo = wx.getStorageSync('userInfo')

        if (!userInfo || !userInfo.id) {
            wx.redirectTo({
                url: '/pages/login/login'
            })
            return
        }

        const bestScore = wx.getStorageSync('matchBestScore') || 0

        // ⭐ 读取上一关累积的计时和正确匹配数
        const savedTotalTime = wx.getStorageSync('matchTotalTime') || 0
        const savedCorrectCount = wx.getStorageSync('matchCorrectCount') || 0
        // 读完立即清空，避免残留影响重新挑战
        wx.removeStorageSync('matchTotalTime')
        wx.removeStorageSync('matchCorrectCount')

        this.setData({
            score: userInfo.totalScore || 0,
            levelId: parseInt(options.levelId) || 1,
            bestScore: bestScore,
            totalTime: savedTotalTime,
            totalCorrectCount: savedCorrectCount
        })

        // 从后端同步最新积分
        this.refreshScoreFromServer()

        this.startLevel()
    },

    async refreshScoreFromServer() {
        const userInfo = wx.getStorageSync('userInfo')
        if (!userInfo || !userInfo.id) return
        try {
            const api = require('../../utils/api.js')
            const res = await api.getUserInfo(userInfo.id)
            if (res.code === 200 && res.data) {
                const fresh = { ...userInfo, totalScore: res.data.totalScore || 0 }
                wx.setStorageSync('userInfo', fresh)
                this.setData({ score: fresh.totalScore || 0 })
            }
        } catch (e) {}
    },

    onUnload() {
        if (this.data.timer) {
            clearInterval(this.data.timer)
        }
    },

    startLevel() {

        const levelData = legalMatchData[this.data.levelId]

        if (!levelData) {

            wx.showToast({
                title: '关卡不存在',
                icon: 'none'
            })

            setTimeout(() => {
                wx.navigateBack()
            }, 1500)

            return
        }

        const shuffled = [...levelData.options].sort(() => Math.random() - 0.5)

        this.setData({
            items: levelData.items,
            options: shuffled,
            startTime: Date.now(),
            matchedPairs: [],
            allMatched: false,
            selectedId: null,
            showResult: false,

            // ⭐ 每关重新初始化
            completedLevels: []
        })

        this.startTimer()
    },

    startTimer() {

        if (this.data.timer) {
            clearInterval(this.data.timer)
        }

        this.setData({
            timer: setInterval(() => {

                this.setData({
                    timeUsed: Math.floor(
                        (Date.now() - this.data.startTime) / 1000
                    )
                })

            }, 1000)
        })
    },

    selectItem(e) {

        const id = e.currentTarget.dataset.id

        const isMatched = this.data.matchedPairs.some(
            p => p.itemId == id
        )

        if (isMatched) return

        this.setData({
            selectedId: id
        })
    },

    selectOption(e) {

        const index = e.currentTarget.dataset.index

        if (index === undefined || index === null) {
            return
        }

        const option = this.data.options[index]

        if (!option) {
            return
        }

        const optionId = option.id

        // ======================
        // 已匹配 -> 取消匹配
        // ======================
        let existPair = null

        for (let i = 0; i < this.data.matchedPairs.length; i++) {

            if (this.data.matchedPairs[i].optionId == optionId) {

                existPair = this.data.matchedPairs[i]
                break
            }
        }

        if (existPair) {

            const newPairs = []

            for (let i = 0; i < this.data.matchedPairs.length; i++) {

                if (this.data.matchedPairs[i].optionId != optionId) {

                    newPairs.push(this.data.matchedPairs[i])
                }
            }

            const newItems = []

            for (let i = 0; i < this.data.items.length; i++) {

                let item = this.data.items[i]

                if (item.id == existPair.itemId) {
                    item.colorClass = ''
                }

                newItems.push(item)
            }

            const newOptions = []

            for (let i = 0; i < this.data.options.length; i++) {

                let opt = this.data.options[i]

                if (opt.id == optionId) {
                    opt.colorClass = ''
                }

                newOptions.push(opt)
            }

            this.setData({
                matchedPairs: newPairs,
                items: newItems,
                options: newOptions,
                allMatched: false
            })

            wx.showToast({
                title: '已取消匹配',
                icon: 'none'
            })

            return
        }

        // ======================
        // 必须先选左边
        // ======================
        if (!this.data.selectedId) {

            wx.showToast({
                title: '请先选择左边法条',
                icon: 'none'
            })

            return
        }

        // ======================
        // 左边是否已匹配
        // ======================
        let itemMatched = null

        for (let i = 0; i < this.data.matchedPairs.length; i++) {

            if (this.data.matchedPairs[i].itemId == this.data.selectedId) {

                itemMatched = this.data.matchedPairs[i]
                break
            }
        }

        if (itemMatched) {

            wx.showToast({
                title: '该法条已匹配',
                icon: 'none'
            })

            return
        }

        // ======================
        // 颜色
        // ======================
        let colorIndex = -1

        for (let i = 0; i < this.data.items.length; i++) {

            if (this.data.items[i].id == this.data.selectedId) {

                colorIndex = i
                break
            }
        }

        const colorClass = 'color-' + (colorIndex % 5)

        // ======================
        // 添加匹配
        // ======================
        const newPairs = this.data.matchedPairs.slice()

        newPairs.push({
            itemId: this.data.selectedId,
            optionId: optionId,
            color: colorClass
        })

        const newItems = []

        for (let i = 0; i < this.data.items.length; i++) {

            let item = this.data.items[i]

            if (item.id == this.data.selectedId) {
                item.colorClass = colorClass
            }

            newItems.push(item)
        }

        const newOptions = []

        for (let i = 0; i < this.data.options.length; i++) {

            let opt = this.data.options[i]

            if (opt.id == optionId) {
                opt.colorClass = colorClass
            }

            newOptions.push(opt)
        }

        const allMatched = (
            newPairs.length === this.data.items.length
        )

        this.setData({
            matchedPairs: newPairs,
            items: newItems,
            options: newOptions,
            selectedId: null,
            allMatched: allMatched
        })

        // ======================
        // 自动下一关
        // ======================
        if (allMatched) {

            setTimeout(() => {

                if (this.data.levelId >= 7) {

                    this.showFinalResult()

                } else {

                    // ⭐ 统计本关答对了几对（item.id == option.id 才算对）
                    const levelCorrect = this.data.matchedPairs.filter(
                        p => p.itemId == p.optionId
                    ).length

                    const newTotalTime =
                        this.data.totalTime + this.data.timeUsed
                    const newCorrectCount =
                        this.data.totalCorrectCount + levelCorrect

                    wx.setStorageSync('matchTotalTime', newTotalTime)
                    wx.setStorageSync('matchCorrectCount', newCorrectCount)

                    wx.redirectTo({
                        url:
                            '/pages/game-match/game-match?levelId=' +
                            (this.data.levelId + 1)
                    })
                }

            }, 600)
        }
    },

    // ======================
    // 最终结算
    // ======================
    showFinalResult() {

        if (this.data.timer) {
            clearInterval(this.data.timer)
        }

        const totalTime =
            this.data.totalTime + this.data.timeUsed

        const minutes = Math.floor(totalTime / 60)

        const seconds = totalTime % 60

        const totalTimeText =
            minutes > 0
                ? minutes + '分' + seconds + '秒'
                : seconds + '秒'

        // ⭐ 正确计分：统计最后一关答对数 + 前面累积
        const finalLevelCorrect = this.data.matchedPairs.filter(
            p => p.itemId == p.optionId
        ).length
        const totalCorrectItems = this.data.totalCorrectCount + finalLevelCorrect
        const totalItems = 65

        const scorePerItem = 100 / totalItems

        let displayScore = (
            totalCorrectItems * scorePerItem
        ).toFixed(1)

        if (parseFloat(displayScore) >= 99.9) {
            displayScore = '100'
        }

        const isFullScore = totalCorrectItems >= totalItems

        // ⭐ 游戏完成 +2积分
        let newScore = this.data.score + 2

        const userInfo = wx.getStorageSync('userInfo')

        if (userInfo) {

            userInfo.totalScore = newScore

            wx.setStorageSync('userInfo', userInfo)
        }

        // ⭐ 保存最高分
        if (parseFloat(displayScore) > this.data.bestScore) {

            wx.setStorageSync(
                'matchBestScore',
                parseFloat(displayScore)
            )
        }

        this.setData({
            showAllComplete: true,
            isFullScore: isFullScore,
            displayScore: displayScore,
            totalTimeText: totalTimeText,
            totalTime: totalTime,
            score: newScore
        })

        // ⭐ 通关后清空累计数据
        wx.removeStorageSync('matchTotalTime')
        wx.removeStorageSync('matchCorrectCount')
    },

    getItemColor(id) {

        const pair = this.data.matchedPairs.find(
            p => p.itemId == id
        )

        return pair ? pair.color : ''
    },

    getOptionColor(id) {

        const pair = this.data.matchedPairs.find(
            p => p.optionId == id
        )

        return pair ? pair.color : ''
    },

    goBack() {
        wx.navigateBack()
    },

    restart() {

        this.setData({
            showResult: false,
            matchedPairs: [],
            allMatched: false,
            selectedId: null
        })

        wx.removeStorageSync('matchTotalTime')
        wx.removeStorageSync('matchCorrectCount')

        this.startLevel()
    },

    closeAllComplete() {

        this.setData({
            showAllComplete: false
        })

        wx.navigateBack()
    }
})