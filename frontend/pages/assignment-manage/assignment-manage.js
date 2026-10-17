const api = require('../../utils/api.js')

Page({
    data: {
        userInfo: null,
        assignments: [],
        filteredAssignments: [],
        activeTab: 0,
        filterStatus: 'all',
        tabs: [
            { label: '全部', value: 'all' },
            { label: '未截止', value: 'active' },
            { label: '已截止', value: 'expired' },
            { label: '草稿', value: 'draft' },
            { label: '审核', value: 'review' },
            { label: '已发布', value: 'published' }
        ],
        approvedCount: 0,
        pendingReviewCount: 0,
        expandedId: null,
        cardQuestions: []
    },

    onLoad() {
        const userInfo = wx.getStorageSync('userInfo')
        if (!userInfo || (userInfo.role !== 'teacher' && userInfo.role !== 'admin')) {
            wx.redirectTo({ url: '/pages/login/login' })
            return
        }
        this.setData({ userInfo })
        this.loadAssignments()
    },

    goBack() {
        wx.navigateBack();
    },

    onShow() {
        this.loadAssignments()
    },

    switchTab(e) {
        const index = parseInt(e.currentTarget.dataset.index)
        const tab = this.data.tabs[index]
        this.setData({
            activeTab: index,
            filterStatus: tab.value
        })
        this.filterAssignments()
    },

    async loadAssignments() {
        wx.showLoading({ title: '加载中...' })
        try {
            const res = await api.getTeacherAssignments({ teacherId: this.data.userInfo.id })
            if (res.code === 200) {
                const now = new Date().getTime()
                const assignments = (res.data || []).map(item => {
                    const deadline = new Date(item.deadline).getTime()
                    const reviewStatus = item.reviewStatus || 'draft'
                    return {
                        ...item,
                        isExpired: deadline <= now && deadline > 0,
                        isActive: deadline > now,
                        reviewStatus: reviewStatus,
                        deadlineDate: item.deadline ? this.formatDate(item.deadline) : ''
                    }
                })
                const approvedCount = assignments.filter(a => a.reviewStatus === 'approved').length
                const pendingReviewCount = assignments.filter(a => a.reviewStatus === 'pending_review' || a.reviewStatus === 'approved' || a.reviewStatus === 'rejected').length
                this.setData({ assignments, approvedCount, pendingReviewCount })
                this.filterAssignments()
            }
        } catch (err) {
            console.error('加载作业列表失败', err)
            this.setData({ assignments: [] })
        } finally {
            wx.hideLoading()
        }
    },

    formatDate(dateStr) {
        if (!dateStr) return ''
        const d = new Date(dateStr)
        const y = d.getFullYear()
        const m = String(d.getMonth() + 1).padStart(2, '0')
        const day = String(d.getDate()).padStart(2, '0')
        return `${y}-${m}-${day}`
    },

    filterAssignments() {
        let filtered = this.data.assignments

        switch (this.data.filterStatus) {
            case 'active':
                filtered = filtered.filter(a => a.isActive && a.reviewStatus === 'published')
                break
            case 'expired':
                filtered = filtered.filter(a => a.isExpired && a.reviewStatus === 'published')
                break
            case 'draft':
                filtered = filtered.filter(a => !a.reviewStatus || a.reviewStatus === 'draft')
                break
            case 'review':
                filtered = filtered.filter(a => a.reviewStatus === 'pending_review' || a.reviewStatus === 'approved' || a.reviewStatus === 'rejected')
                break
            case 'published':
                filtered = filtered.filter(a => a.reviewStatus === 'published')
                break
            case 'all':
            default:
                break
        }

        this.setData({ filteredAssignments: filtered })
    },

    // 获取审核状态的颜色和文字
    getStatusInfo(status) {
        switch (status) {
            case 'draft': return { color: '#999', text: '草稿' }
            case 'pending_review': return { color: '#faad14', text: '审核中' }
            case 'approved': return { color: '#52c41a', text: '已通过' }
            case 'rejected': return { color: '#ff4d4f', text: '已驳回' }
            case 'published': return { color: '#1890ff', text: '已发布' }
            default: return { color: '#999', text: '草稿' }
        }
    },

    goPublish() {
        wx.navigateTo({ url: '/pages/assignment-publish/assignment-publish' })
    },

    viewSubmissions(e) {
        const id = e.currentTarget.dataset.id
        wx.navigateTo({ url: `/pages/assignment-submissions/assignment-submissions?id=${id}` })
    },

    editAssignment(e) {
        const id = e.currentTarget.dataset.id
        wx.navigateTo({ url: `/pages/assignment-edit/assignment-edit?id=${id}` })
    },

    // 展开/收起题目
    async toggleExpand(e) {
        const id = e.currentTarget.dataset.id
        if (this.data.expandedId === id) {
            this.setData({ expandedId: null, cardQuestions: [] })
            return
        }
        this.setData({ expandedId: id })
        await this.loadCardQuestions(id)
    },

    // 加载作业题目
    async loadCardQuestions(assignmentId) {
        try {
            const res = await api.getAssignmentQuestions(assignmentId)
            const questions = (res.data || []).map((q, i) => ({
                questionId: q.questionId || q.id,
                questionText: q.questionText || q.title || '',
                _index: i
            }))
            this.setData({ cardQuestions: questions })
        } catch (e) {
            this.setData({ cardQuestions: [] })
        }
    },

    // 删除展开中的题目（不需要重新审核）
    async deleteCardQuestion(e) {
        const index = e.currentTarget.dataset.index
        const qid = e.currentTarget.dataset.qid
        const aid = e.currentTarget.dataset.id
        if (qid) {
            try { await api.removeQuestionFromAssignment(aid, qid) } catch (e) {}
        }
        const questions = [...this.data.cardQuestions]
        questions.splice(index, 1)
        this.setData({ cardQuestions: questions })
        wx.showToast({ title: '已删除', icon: 'success' })
    },

    // 添加题目弹窗
    goAddQuestion(e) {
        const id = e.currentTarget.dataset.id
        const that = this
        wx.showActionSheet({
            itemList: ['从题库随机选取', '手动创建题目'],
            success(res) {
                if (res.tapIndex === 0) {
                    that.bankPickQuestions(id)
                } else {
                    wx.navigateTo({ url: `/pages/question-edit/question-edit?assignmentId=${id}&index=new` })
                }
            }
        })
    },

    // 从题库随机选题
    bankPickQuestions(assignmentId) {
        wx.showModal({
            title: '从题库选题',
            editable: true,
            placeholderText: '输入要抽取的题目数量',
            success: async (res) => {
                if (res.confirm && res.content) {
                    const count = parseInt(res.content)
                    if (isNaN(count) || count <= 0) {
                        wx.showToast({ title: '请输入有效数量', icon: 'none' })
                        return
                    }
                    wx.showLoading({ title: '抽取中...' })
                    try {
                        // 调题库随机抽取
                        await api.addQuestionToAssignment(assignmentId, { type: 'bank_random', count })
                        wx.hideLoading()
                        wx.showToast({ title: `已添加${count}题`, icon: 'success' })
                        // 添加题目后回到草稿状态
                        await this.resetToDraft(assignmentId)
                        this.loadCardQuestions(assignmentId)
                        this.loadAssignments()
                    } catch (e) {
                        wx.hideLoading()
                        wx.showToast({ title: '抽取失败', icon: 'none' })
                    }
                }
            }
        })
    },

    // 添加题目后重置为草稿
    async resetToDraft(id) {
        try {
            await api.updateAssignment(id, { reviewStatus: 'draft' })
        } catch (e) {}
    },

    // 保存题目变更（添加新题后需要重新审核）
    async saveCardQuestions(e) {
        const id = e.currentTarget.dataset.id
        wx.showLoading({ title: '保存中...' })
        try {
            const questions = this.data.cardQuestions.map((q, i) => ({
                questionId: q.questionId || (Date.now() + i),
                questionText: q.questionText,
                sortOrder: i + 1
            }))
            await api.updateAssignment(id, { questions, reviewStatus: 'draft' })
            wx.hideLoading()
            wx.showToast({ title: '已保存，需重新送审', icon: 'success' })
            this.loadAssignments()
            this.setData({ expandedId: null })
        } catch (e) {
            wx.hideLoading()
            wx.showToast({ title: '保存失败', icon: 'none' })
        }
    },

    // 查看审核结果
    viewReviewResult(e) {
        const id = e.currentTarget.dataset.id
        wx.navigateTo({ url: `/pages/assignment-review-result/assignment-review-result?id=${id}` })
    },

    // 提交审核
    submitForReview(e) {
        const id = e.currentTarget.dataset.id
        wx.showModal({
            title: '提交审核',
            content: '确定要将此作业提交给法务人员审核吗？',
            success: async (res) => {
                if (res.confirm) {
                    wx.showLoading({ title: '提交中...' })
                    try {
                        const result = await api.submitForReview(id)
                        wx.hideLoading()
                        if (result.code === 200) {
                            wx.showToast({ title: '已提交审核', icon: 'success' })
                            this.loadAssignments()
                        } else {
                            wx.showToast({ title: result.msg || '提交失败', icon: 'none' })
                        }
                    } catch (err) {
                        wx.hideLoading()
                        console.error('提交审核失败:', err)
                        wx.showToast({ title: '网络错误，请确认后端已启动', icon: 'none' })
                    }
                }
            }
        })
    },

    // 发布作业（审核通过后）
    publishAssignment(e) {
        const id = e.currentTarget.dataset.id
        wx.showModal({
            title: '发布作业',
            content: '确定要发布此作业吗？发布后党员可进行作答。',
            success: async (res) => {
                if (res.confirm) {
                    wx.showLoading({ title: '发布中...' })
                    try {
                        const result = await api.publishAssignment(id)
                        wx.hideLoading()
                        if (result.code === 200) {
                            wx.showToast({ title: '发布成功', icon: 'success' })
                            // 自动切到已发布标签
                            this.setData({ activeTab: 5, filterStatus: 'published' })
                            this.loadAssignments()
                        } else {
                            wx.showToast({ title: result.msg || '发布失败', icon: 'none' })
                        }
                    } catch (err) {
                        wx.hideLoading()
                        console.error('发布失败:', err)
                        wx.showToast({ title: '网络错误，请确认后端已启动', icon: 'none' })
                    }
                }
            }
        })
    },

    deleteAssignment(e) {
        const id = e.currentTarget.dataset.id
        wx.showModal({
            title: '确认删除',
            content: '删除后无法恢复，确定要删除吗？',
            success: async (res) => {
                if (res.confirm) {
                    try {
                        await api.deleteAssignment(id)
                        wx.showToast({ title: '删除成功', icon: 'success' })
                        this.loadAssignments()
                    } catch (err) {
                        wx.showToast({ title: '删除失败', icon: 'none' })
                    }
                }
            }
        })
    }
})
