const api = require('../../utils/api.js')

Page({
    data: {
        reviewId: null,
        assignment: null,
        questions: [],
        overallComment: '',
        userInfo: null,
        loading: true,
        questionReviews: {},
        approvedCount: 0,
        rejectedCount: 0,
        totalCount: 0,
        showCommentPanel: false,
        currentQuestionId: null,
        currentComment: ''
    },

    onLoad(options) {
        const userInfo = wx.getStorageSync('userInfo')
        if (!userInfo || userInfo.role !== 'legal_officer') {
            wx.redirectTo({ url: '/pages/login/login' })
            return
        }

        const reviewId = options.id
        if (!reviewId) {
            wx.showToast({ title: '参数错误', icon: 'none' })
            setTimeout(() => wx.navigateBack(), 1500)
            return
        }

        this.setData({ userInfo, reviewId })
        this.loadReviewDetail()
    },

    async loadReviewDetail() {
        wx.showLoading({ title: '加载中...' })
        try {
            const res = await api.getReviewDetail(this.data.reviewId)
            wx.hideLoading()

            if (res.code === 200 && res.data) {
                const data = res.data
                let questions = []

                if (data.questions && Array.isArray(data.questions)) {
                    questions = data.questions.map((q, i) => {
                        let options = q.options
                        if (typeof options === 'string') {
                            try { options = JSON.parse(options) } catch (e) { options = [] }
                        }
                        return {
                            ...q,
                            options: Array.isArray(options) ? options : [],
                            questionText: q.title || q.questionText || '',
                            correctAnswer: q.answer || q.correctAnswer || '',
                            _index: i
                        }
                    })
                }

                let questionReviews = {}
                if (data.questionReview && Array.isArray(data.questionReview)) {
                    data.questionReview.forEach(qr => {
                        const qid = qr.questionId
                        questionReviews[qid] = {
                            status: qr.status,
                            comment: qr.comment || ''
                        }
                    })
                }

                let approvedCount = 0, rejectedCount = 0
                Object.values(questionReviews).forEach(r => {
                    if (r.status === 'approved') approvedCount++
                    else if (r.status === 'rejected') rejectedCount++
                })

                this.setData({
                    assignment: data,
                    questions: questions,
                    questionReviews,
                    totalCount: questions.length,
                    approvedCount,
                    rejectedCount,
                    overallComment: data.reviewComment || '',
                    loading: false
                })
            } else {
                wx.showToast({ title: res.msg || '加载失败', icon: 'none' })
                this.setData({ loading: false })
            }
        } catch (err) {
            wx.hideLoading()
            console.error('加载审核详情失败:', err)
            wx.showToast({ title: '网络错误，请检查后端', icon: 'none' })
            this.setData({ loading: false })
        }
    },

    goBack() {
        wx.navigateBack()
    },

    // 逐题通过
    async approveQuestion(e) {
        const questionId = e.currentTarget.dataset.qid
        const questionReviews = { ...this.data.questionReviews }
        questionReviews[questionId] = { status: 'approved', comment: '' }
        this.updateCounts(questionReviews)

        try {
            await api.approveReviewQuestion(this.data.reviewId, questionId, '')
        } catch (err) {
            console.error('审核通过请求失败:', err)
        }
    },

    // 逐题驳回
    showRejectPanel(e) {
        const questionId = e.currentTarget.dataset.qid
        const questionReviews = this.data.questionReviews
        const currentComment = (questionReviews[questionId] && questionReviews[questionId].comment) || ''
        this.setData({
            showCommentPanel: true,
            currentQuestionId: questionId,
            currentComment: currentComment
        })
    },

    onCommentInput(e) {
        this.setData({ currentComment: e.detail.value })
    },

    async confirmReject() {
        const { currentQuestionId, currentComment } = this.data
        if (!currentComment || !currentComment.trim()) {
            wx.showToast({ title: '请填写驳回理由', icon: 'none' })
            return
        }

        const questionReviews = { ...this.data.questionReviews }
        questionReviews[currentQuestionId] = {
            status: 'rejected',
            comment: currentComment.trim()
        }
        this.updateCounts(questionReviews)

        try {
            await api.rejectReviewQuestion(this.data.reviewId, currentQuestionId, currentComment.trim())
        } catch (err) {
            console.error('驳回请求失败:', err)
        }

        this.setData({ showCommentPanel: false, currentQuestionId: null, currentComment: '' })
    },

    cancelReject() {
        this.setData({ showCommentPanel: false, currentQuestionId: null, currentComment: '' })
    },

    resetQuestion(e) {
        const questionId = e.currentTarget.dataset.qid
        const questionReviews = { ...this.data.questionReviews }
        delete questionReviews[questionId]
        this.updateCounts(questionReviews)
    },

    updateCounts(reviews) {
        let approved = 0, rejected = 0
        Object.values(reviews).forEach(r => {
            if (r.status === 'approved') approved++
            else if (r.status === 'rejected') rejected++
        })
        this.setData({ questionReviews: reviews, approvedCount: approved, rejectedCount: rejected })
    },

    // 提交整体审核
    async submitReview() {
        const { totalCount, approvedCount, rejectedCount } = this.data
        const reviewedCount = approvedCount + rejectedCount

        if (reviewedCount < totalCount) {
            wx.showModal({
                title: '提示',
                content: `还有${totalCount - reviewedCount}道题未审核，未审核的题目视为通过，确定提交吗？`,
                success: async (res) => {
                    if (res.confirm) await this.doSubmitReview()
                }
            })
        } else {
            wx.showModal({
                title: '确认提交',
                content: `通过${approvedCount}题，驳回${rejectedCount}题，确定提交审核结果吗？`,
                success: async (res) => {
                    if (res.confirm) await this.doSubmitReview()
                }
            })
        }
    },

    async doSubmitReview() {
        wx.showLoading({ title: '提交中...' })
        try {
            const result = await api.submitReviewResult(this.data.reviewId, {
                comment: this.data.overallComment
            })
            wx.hideLoading()

            if (result.code === 200 && result.data) {
                const data = result.data
                const msg = data.status === 'approved'
                    ? '审核通过！作业已返回给管理员发布'
                    : `已驳回！共${data.rejectedCount || 0}题需修改，已打回给管理员`
                wx.showModal({
                    title: data.status === 'approved' ? '审核通过' : '已驳回',
                    content: msg,
                    showCancel: false,
                    success: () => wx.navigateBack()
                })
            } else {
                wx.showToast({ title: result.msg || '提交失败', icon: 'none' })
            }
        } catch (err) {
            wx.hideLoading()
            console.error('提交审核失败:', err)
            wx.showToast({ title: '网络错误，请重试', icon: 'none' })
        }
    },

    onOverallCommentInput(e) {
        this.setData({ overallComment: e.detail.value })
    }
})
