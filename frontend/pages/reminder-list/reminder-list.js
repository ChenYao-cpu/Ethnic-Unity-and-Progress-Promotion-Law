const api = require('../../utils/api.js')

Page({
    data: {
        reminders: [],
        loading: false,
        userInfo: null,
        score: 0
    },

    onLoad() {
        const userInfo = wx.getStorageSync('userInfo')
        if (!userInfo) {
            wx.redirectTo({ url: '/pages/login/login' })
            return
        }
        this.setData({ userInfo })
        this.loadScore()
        this.loadReminders()
    },

    async loadScore() {
        try {
            const res = await api.getUserInfo(this.data.userInfo.id)
            if (res.code === 200 && res.data) {
                this.setData({ score: res.data.totalScore || 0 })
            }
        } catch (err) {
            console.error('加载积分失败', err)
        }
    },

    async loadReminders() {
        this.setData({ loading: true })
        console.log('=== reminder-list: 开始加载提醒 ===', 'userId:', this.data.userInfo.id)

        try {
            const res = await api.getMyReminders(this.data.userInfo.id)
            console.log('=== reminder-list: 后端返回 ===', JSON.stringify(res))
            console.log('=== reminder-list: 数据条数 ===', res.data ? res.data.length : 0)

            if (res.code === 200 && res.data) {
                console.log('=== reminder-list: 完整数据 ===', JSON.stringify(res.data))
                this.setData({ reminders: res.data })
            } else {
                console.log('=== reminder-list: 返回数据异常 ===', res)
            }
        } catch (err) {
            console.error('=== reminder-list: 加载失败 ===', err)
            wx.showToast({ title: '网络错误，请确认后端已启动', icon: 'none' })
        } finally {
            this.setData({ loading: false })
        }
    },

    async markRead(e) {
        const id = e.currentTarget.dataset.id
        try {
            await api.markReminderRead(id)
            const reminders = this.data.reminders.map(r => {
                if (r.id === id) return { ...r, isRead: 1 }
                return r
            })
            this.setData({ reminders })
        } catch (err) {
            const reminders = this.data.reminders.map(r => {
                if (r.id === id) return { ...r, isRead: 1 }
                return r
            })
            this.setData({ reminders })
        }
    },

    async markAllRead() {
        try {
            await api.markAllRemindersRead(this.data.userInfo.id)
        } catch (err) {}
        const reminders = this.data.reminders.map(r => ({ ...r, isRead: 1 }))
        this.setData({ reminders })
        wx.showToast({ title: '全部已读', icon: 'success' })
    },

    goBack() {
        wx.navigateBack()
    }
})
