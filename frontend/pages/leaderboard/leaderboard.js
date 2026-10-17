const api = require('../../utils/api.js')

Page({
    data: {
        userInfo: null,
        leaderboard: [],
        loading: true
    },

    onLoad() {
        const userInfo = wx.getStorageSync('userInfo')
        // 同时检查 userInfo 存在且有有效的 id
        if (!userInfo || !userInfo.id) {
            wx.redirectTo({ url: '/pages/login/login' })
            return
        }
        this.setData({ userInfo: userInfo })
        this.loadLeaderboard()
    },

    onShow() {
        const userInfo = wx.getStorageSync('userInfo')
        if (userInfo && userInfo.id) {
            this.setData({ userInfo: userInfo })
        }
    },

    onPullDownRefresh() {
        this.loadLeaderboard().finally(() => wx.stopPullDownRefresh())
    },

    goBack() {
        wx.navigateBack({
            delta: 1,
            fail: () => wx.switchTab({ url: '/pages/index/index' })
        })
    },

    async loadLeaderboard() {
        this.setData({ loading: true })
        try {
            const res = await api.getLeaderboard(10)
            if (res.code === 200 && res.data) {
                const list = Array.isArray(res.data) ? res.data : []
                this.setData({ leaderboard: list })
                const currentId = this.data.userInfo && this.data.userInfo.id
                if (currentId && list.length > 0) {
                    const me = list.find(item => item.userId === currentId)
                    if (me && me.totalScore != null) {
                        const updated = { ...this.data.userInfo, totalScore: me.totalScore }
                        wx.setStorageSync('userInfo', updated)
                        this.setData({ userInfo: updated })
                    }
                }
            } else {
                this.setData({ leaderboard: [] })
            }
        } catch (err) {
            console.error('排行榜加载失败:', err)
            this.setData({ leaderboard: [] })
        } finally {
            this.setData({ loading: false })
        }
    }
})
