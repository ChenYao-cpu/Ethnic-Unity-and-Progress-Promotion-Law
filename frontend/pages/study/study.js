const api = require('../../utils/api.js')

Page({
    data: {
        overview: {},
        recentRecords: [],
        studyStats: [],
        profile: {},
        checkinData: {},
        weeklyReport: {},
        isLogin: false,
        activeTab: 'overview',
        isLoading: true,
        todayCheckedIn: false,
        score: 0
    },

    onShow() {
        if (typeof this.getTabBar === 'function' && this.getTabBar()) {
            this.getTabBar().setData({ selected: 1 })
        }
        this.loadData()
    },

    switchTab(e) {
        this.setData({
            activeTab: e.currentTarget.dataset.tab
        })
    },

    async loadData() {
        const userInfo = wx.getStorageSync('userInfo')
        if (!userInfo || !userInfo.id) {
            this.setData({ isLogin: false, isLoading: false })
            return
        }

        this.setData({
            isLogin: true,
            isLoading: true,
            score: userInfo.totalScore || 0
        })
        const userId = userInfo.id

        try {
            const [overview, recentRecords, studyStats, profile, checkinCalendar, weeklyReport] = await Promise.all([
                api.getStudyOverview(userId),
                api.getStudyRecent(userId, 10),
                api.getStudyStats(userId, 7),
                api.getStudyProfile(userId),
                api.getCheckinCalendar(userId),
                api.getWeeklyReport(userId)
            ])

            console.log('学习概览数据:', overview.data)
            console.log('最近学习记录:', recentRecords.data)
            console.log('7日学习统计:', studyStats.data)

            // 处理学习概览数据
            const overviewData = overview.data || {}
            const processedOverview = {
                totalMinutes: overviewData.totalMinutes || 0,
                courseCount: overviewData.courseCount || 0,
                totalHours: overviewData.totalHours || '0.00'
            }


            console.log('学习概览 - 总时长:', processedOverview.totalMinutes, '分钟')
            console.log('学习概览 - 课程数:', processedOverview.courseCount)
            console.log('学习概览 - 总小时:', processedOverview.totalHours)
            console.log('学习概览 - 原始数据:', overviewData)

            // 处理打卡数据
            const checkinDataProcessed = {
                activeDays: checkinCalendar.data ? checkinCalendar.data.totalDays || 0 : 0,
                maxContinuousDays: 0,
                checkinDates: checkinCalendar.data ? checkinCalendar.data.checkinDates || [] : []
            }

            // 计算最长连续打卡天数
            if (checkinDataProcessed.checkinDates.length > 0) {
                checkinDataProcessed.maxContinuousDays = this.calculateMaxContinuousDays(checkinDataProcessed.checkinDates)
            }

            // 处理周报数据
            const weeklyReportData = weeklyReport.data || {}
            const processedWeeklyReport = {
                totalStudyTime: weeklyReportData.totalStudyTime || 0,
                courseCount: weeklyReportData.courseCount || 0,
                averageDailyTime: weeklyReportData.averageDailyTime || 0,
                growthValue: weeklyReportData.growthValue || 0
            }

            this.setData({
                overview: processedOverview,
                recentRecords: recentRecords.data || [],
                studyStats: studyStats.data || [],
                profile: profile.data || {},
                checkinData: checkinDataProcessed,
                weeklyReport: processedWeeklyReport,
                todayCheckedIn: this.isTodayChecked(checkinDataProcessed.checkinDates),
                isLoading: false
            })
        } catch (err) {
            console.error('加载数据失败:', err)
            this.setData({ isLoading: false })
            wx.showToast({ title: '加载失败', icon: 'none' })
        }
    },

    // 计算最长连续打卡天数
    calculateMaxContinuousDays(dates) {
        if (!dates || dates.length === 0) return 0

        const sortedDates = dates.sort((a, b) => new Date(a) - new Date(b))
        let maxDays = 1
        let currentDays = 1

        for (let i = 1; i < sortedDates.length; i++) {
            const prevDate = new Date(sortedDates[i - 1])
            const currDate = new Date(sortedDates[i])
            const diffTime = currDate - prevDate
            const diffDays = Math.round(diffTime / (1000 * 60 * 60 * 24))

            if (diffDays === 1) {
                currentDays++
                maxDays = Math.max(maxDays, currentDays)
            } else {
                currentDays = 1
            }
        }

        return maxDays
    },

    // 判断今天是否已打卡
    isTodayChecked(dates) {
        if (!dates || dates.length === 0) return false

        const today = new Date()
        const todayStr = today.toISOString().split('T')[0]

        return dates.some(date => {
            const dateStr = new Date(date).toISOString().split('T')[0]
            return dateStr === todayStr
        })
    },

    async handleCheckin() {
        const userInfo = wx.getStorageSync('userInfo')
        if (!userInfo || !userInfo.id) return

        wx.showLoading({ title: '打卡中...' })
        try {
            const res = await api.checkin(userInfo.id)
            wx.hideLoading()

            if (res.code === 200 && res.data && res.data.success) {
                wx.showToast({ title: '打卡成功!', icon: 'success' })

                // 重新加载数据以更新统计信息
                this.loadData()
            } else {
                wx.showToast({ title: res.msg || '今日已打卡', icon: 'none' })
            }
        } catch (err) {
            wx.hideLoading()
            wx.showToast({ title: '网络错误', icon: 'none' })
        }
    },

    viewDetail(e) {
        const { recordId, courseId } = e.currentTarget.dataset
        if (courseId) {
            wx.navigateTo({ url: `/pages/course-detail/course-detail?id=${courseId}` })
        }
    },

    goToDetail() {
        wx.navigateTo({
            url: '/pages/study-detail/study-detail'
        })
    },

    goLogin() {
        wx.navigateTo({ url: '/pages/login/login' })
    },

    formatDuration(minutes) {
        if (minutes >= 60) {
            const hours = Math.floor(minutes / 60)
            const mins = minutes % 60
            return mins > 0 ? `${hours}小时${mins}分钟` : `${hours}小时`
        }
        return `${minutes}分钟`
    }
})
