const api = require('../../utils/api.js')

Page({
    data: {
        assignmentId: null,
        assignmentInfo: {},
        submissions: [],
        statistics: null,
        filteredSubmissions: [],
        searchKeyword: '',
        loading: false,
        currentTab: 'detail'
    },

    onLoad(options) {
        this.setData({ assignmentId: options.id })
        this.loadData()
    },

    async loadData() {
        if (this.data.loading) return

        this.setData({ loading: true })
        wx.showLoading({ title: '加载中...' })

        try {
            const [assignmentRes, submissionsRes, statisticsRes] = await Promise.all([
                api.getAssignmentDetail(this.data.assignmentId),
                api.getAssignmentSubmissionsByAssignmentId(this.data.assignmentId),
                api.getAssignmentStatistics(this.data.assignmentId)
            ])

            if (assignmentRes.code === 200) {
                this.setData({ assignmentInfo: assignmentRes.data })
            }

            if (submissionsRes.code === 200) {
                const submissions = submissionsRes.data || []
                this.setData({
                    submissions,
                    filteredSubmissions: submissions
                })
            }

            if (statisticsRes.code === 200) {
                this.setData({ statistics: statisticsRes.data })
            }
        } catch (err) {
            console.error('加载失败', err)
            wx.showToast({ title: '加载失败', icon: 'none' })
        } finally {
            this.setData({ loading: false })
            wx.hideLoading()
        }
    },

    onSearch(e) {
        const keyword = e.detail.value.toLowerCase()
        this.setData({ searchKeyword: keyword })

        if (!keyword) {
            this.setData({ filteredSubmissions: this.data.submissions })
            return
        }

        const filtered = this.data.submissions.filter(item => {
            const studentName = (item.studentName || '').toLowerCase()
            const studentId = String(item.studentId || '')
            return studentName.includes(keyword) || studentId.includes(keyword)
        })

        this.setData({ filteredSubmissions: filtered })
    },

    switchTab(e) {
        const tab = e.currentTarget.dataset.tab
        this.setData({ currentTab: tab })
    },

    viewDetail(e) {
        const studentId = e.currentTarget.dataset.studentid
        wx.navigateTo({
            url: `/pages/student-answer-detail/student-answer-detail?assignmentId=${this.data.assignmentId}&studentId=${studentId}`
        })
    },

    goBack() {
        wx.navigateBack()
    }
})
