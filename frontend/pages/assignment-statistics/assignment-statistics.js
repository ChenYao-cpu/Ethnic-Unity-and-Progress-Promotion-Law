const api = require('../../utils/api.js')

Page({
    data: {
        assignments: [],
        selectedAssignmentId: null,
        selectedIndex: 0,
        statistics: null,
        chartType: 'bar',
        loading: false,
        pieGrad: "" // 预计算饼图渐变字符串，解决WXML表达式报错
    },

    onLoad() {
        this.loadAssignments()
    },

    async loadAssignments() {
        wx.showLoading({ title: '加载中...' })
        try {
            const userInfo = wx.getStorageSync('userInfo')
            if (!userInfo || !userInfo.id) {
                wx.hideLoading()
                return
            }
            const res = await api.getTeacherAssignments({ teacherId: userInfo.id })
            if (res.code === 200) {
                const published = (res.data || []).filter(a => a.reviewStatus === 'published' || a.status === 1)
                this.setData({ assignments: published })
                if (published.length > 0) {
                    this.setData({ selectedAssignmentId: published[0].id, selectedIndex: 0 })
                    this.loadStatistics(published[0].id)
                } else {
                    // 无测验时清空统计与饼图渐变
                    this.setData({ statistics: null, pieGrad: "#eee 0% 100%" })
                }
            }
        } catch (err) {
            console.error('加载作业列表失败', err)
            wx.showToast({ title: '网络错误，请确认后端已启动', icon: 'none' })
        } finally {
            wx.hideLoading()
        }
    },

    async loadStatistics(assignmentId) {
        if (this.data.loading) return
        this.setData({ loading: true })
        wx.showLoading({ title: '加载中...' })

        try {
            const res = await api.getAssignmentStatistics(assignmentId)
            if (res.code === 200 && res.data && res.data.totalCount > 0) {
                const dist = res.data.scoreDistribution || []
                res.data.maxCount = Math.max(...dist.map(d => d.count || 0), 1)
                this.setData({ statistics: res.data })
                // 拿到统计数据后立刻计算饼图渐变
                this.calcPieGrad()
            } else {
                wx.showToast({ title: '暂无提交数据', icon: 'none' })
                this.setData({ statistics: null, pieGrad: "#eee 0% 100%" })
            }
        } catch (err) {
            console.error('加载统计数据失败', err)
            wx.showToast({ title: '网络错误，请确认后端已启动', icon: 'none' })
        } finally {
            this.setData({ loading: false })
            wx.hideLoading()
        }
    },

    onAssignmentChange(e) {
        const index = parseInt(e.detail.value)
        if (this.data.assignments[index]) {
            this.setData({
                selectedAssignmentId: this.data.assignments[index].id,
                selectedIndex: index
            })
            this.loadStatistics(this.data.assignments[index].id)
        }
    },

    switchChartType(e) {
        const type = e.currentTarget.dataset.type
        if (type) this.setData({ chartType: type })
    },

    // 预计算饼图 conic-gradient 完整字符串，移走WXML复杂运算
    calcPieGrad() {
        const s = this.data.statistics
        if (!s || !s.scoreDistribution || s.scoreDistribution.length === 0) {
            this.setData({ pieGrad: "#eee 0% 100%" })
            return
        }
        let startPercent = 0
        const gradParts = []
        s.scoreDistribution.forEach(item => {
            const pct = Number(item.percentage) || 0
            const endPercent = startPercent + pct
            gradParts.push(`${item.levelColor} ${startPercent}% ${endPercent}%`)
            startPercent = endPercent
        })
        // 不足100%补灰色兜底
        if (startPercent < 100) {
            gradParts.push(`#f0f0f0 ${startPercent}% 100%`)
        }
        this.setData({ pieGrad: gradParts.join(",") })
    },

    // 获取柱状图最大高度（用于CSS计算）
    getMaxCount(dist) {
        if (!dist || dist.length === 0) return 1
        return Math.max(...dist.map(d => d.count), 1)
    },

    goBack() {
        wx.navigateBack()
    }
})