const api = require('../../utils/api.js')

Page({
    data: {
        assignmentId: null,
        title: '',
        description: '',
        deadline: '',
        questions: [],
        userInfo: null,
        loading: true,
        showAddPanel: false,
        // 添加题目相关
        questionMode: 'bank',
        bankQuestionCount: 10,
        bankCategoryIndex: 0,
        bankTypeIndex: 0,
        categories: [
            { value: null, label: '全部' },
            { value: 1, label: '第一章' },
            { value: 2, label: '第二章' },
            { value: 3, label: '第三章' }
        ],
        questionTypes: [
            { value: 1, label: '单选题' },
            { value: 2, label: '多选题' }
        ],
        pendingQuestions: [],
        loadingBank: false,
        selectedPendingCount: 0
    },

    onLoad(options) {
        const userInfo = wx.getStorageSync('userInfo')
        if (!userInfo || (userInfo.role !== 'teacher' && userInfo.role !== 'admin')) {
            wx.redirectTo({ url: '/pages/login/login' })
            return
        }

        const assignmentId = options.id
        if (!assignmentId) {
            wx.showToast({ title: '参数错误', icon: 'none' })
            setTimeout(() => wx.navigateBack(), 1500)
            return
        }

        this.setData({ userInfo, assignmentId })
        this.loadAssignmentDetail()
    },

    async loadAssignmentDetail() {
        wx.showLoading({ title: '加载中...' })
        try {
            const res = await api.getAssignmentDetail(this.data.assignmentId)
            if (res.code === 200 && res.data) {
                const data = res.data
                // 解析题目
                let questions = []
                if (data.questions) {
                    if (typeof data.questions === 'string') {
                        try { questions = JSON.parse(data.questions) } catch (e) { questions = [] }
                    } else if (Array.isArray(data.questions)) {
                        questions = data.questions
                    }
                }

                questions = questions.map((q, index) => {
                    let options = q.options
                    if (typeof options === 'string') {
                        try { options = JSON.parse(options) } catch (e) { options = [] }
                    }
                    return {
                        ...q,
                        options: options,
                        questionText: q.questionText || q.content || q.title || '',
                        correctAnswer: q.correctAnswer || q.answer || '',
                        _index: index
                    }
                })

                this.setData({
                    title: data.title || '',
                    description: data.description || '',
                    deadline: data.deadline || '',
                    questions: questions,
                    loading: false
                })
            }
        } catch (err) {
            console.error('加载作业详情失败', err)
            wx.showToast({ title: '加载失败', icon: 'none' })
        } finally {
            wx.hideLoading()
        }
    },

    goBack() {
        wx.navigateBack()
    },

    onTitleInput(e) {
        this.setData({ title: e.detail.value })
    },

    onDescInput(e) {
        this.setData({ description: e.detail.value })
    },

    onDeadlineChange(e) {
        this.setData({ deadline: e.detail.value })
    },

    // 编辑题目
    editQuestion(e) {
        const index = e.currentTarget.dataset.index
        const question = this.data.questions[index]
        // 将题目数据编码传到编辑页
        const questionStr = encodeURIComponent(JSON.stringify(question))
        wx.navigateTo({
            url: `/pages/question-edit/question-edit?assignmentId=${this.data.assignmentId}&index=${index}&question=${questionStr}`
        })
    },

    // 删除题目
    deleteQuestion(e) {
        const index = e.currentTarget.dataset.index
        wx.showModal({
            title: '确认删除',
            content: '确定要删除这道题目吗？',
            success: (res) => {
                if (res.confirm) {
                    const questions = [...this.data.questions]
                    questions.splice(index, 1)
                    this.setData({ questions })
                    wx.showToast({ title: '已删除', icon: 'success' })
                }
            }
        })
    },

    // 显示添加题目面板
    showAddQuestion() {
        this.setData({ showAddPanel: true })
    },

    hideAddPanel() {
        this.setData({
            showAddPanel: false,
            pendingQuestions: [],
            selectedPendingCount: 0
        })
    },

    switchMode(e) {
        this.setData({ questionMode: e.currentTarget.dataset.mode })
    },

    onBankCategoryChange(e) {
        this.setData({ bankCategoryIndex: parseInt(e.detail.value) })
    },

    onBankTypeChange(e) {
        this.setData({ bankTypeIndex: parseInt(e.detail.value) })
    },

    onBankCountInput(e) {
        this.setData({ bankQuestionCount: parseInt(e.detail.value) || 10 })
    },

    async loadBankQuestions() {
        const { bankQuestionCount, bankCategoryIndex, bankTypeIndex, categories, questionTypes } = this.data

        if (bankQuestionCount < 1 || bankQuestionCount > 50) {
            wx.showToast({ title: '题目数量应在 1-50 之间', icon: 'none' })
            return
        }

        this.setData({ loadingBank: true })

        try {
            const category = categories[bankCategoryIndex].value
            const type = questionTypes[bankTypeIndex].value

            const params = {}
            if (category !== null && category !== undefined) params.category = category
            if (type !== null && type !== undefined) params.type = type
            params.limit = 200

            const res = await api.getQuestions(params)
            this.setData({ loadingBank: false })

            if (res.code === 200 && res.data && res.data.length > 0) {
                const filtered = res.data.filter(q => {
                    if (type !== null && type !== undefined) return q.type === type
                    return true
                })

                const shuffled = this.shuffleArray(filtered)
                const selected = shuffled.slice(0, Math.min(bankQuestionCount, shuffled.length))

                const questions = selected.map(q => {
                    let options = []
                    try {
                        if (q.options) {
                            options = typeof q.options === 'string' ? JSON.parse(q.options) : q.options
                        }
                    } catch (e) { options = [] }

                    return {
                        questionId: q.id,
                        questionText: q.title || q.content || q.questionText || '',
                        options: options,
                        correctAnswer: q.answer || q.correctAnswer || '',
                        analysis: q.analysis || '',
                        type: q.type,
                        difficulty: q.difficulty || 2,
                        selected: false
                    }
                })

                this.setData({ pendingQuestions: questions })
                this.updatePendingCount()
            } else {
                wx.showToast({ title: '题库中没有符合条件的题目', icon: 'none' })
            }
        } catch (err) {
            this.setData({ loadingBank: false })
            console.error('加载题库失败:', err)
            wx.showToast({ title: '加载失败', icon: 'none' })
        }
    },

    shuffleArray(array) {
        const arr = [...array]
        for (let i = arr.length - 1; i > 0; i--) {
            const j = Math.floor(Math.random() * (i + 1));
            [arr[i], arr[j]] = [arr[j], arr[i]]
        }
        return arr
    },

    togglePendingQuestion(e) {
        const index = e.currentTarget.dataset.index
        const pendingQuestions = [...this.data.pendingQuestions]
        pendingQuestions[index].selected = !pendingQuestions[index].selected
        this.setData({ pendingQuestions })
        this.updatePendingCount()
    },

    updatePendingCount() {
        const count = this.data.pendingQuestions.filter(q => q.selected).length
        this.setData({ selectedPendingCount: count })
    },

    // 确认添加选中题目
    confirmAddQuestions() {
        const selectedQuestions = this.data.pendingQuestions.filter(q => q.selected)
        if (selectedQuestions.length === 0) {
            wx.showToast({ title: '请选择题目', icon: 'none' })
            return
        }

        const questions = [...this.data.questions, ...selectedQuestions.map(q => ({
            questionId: q.questionId,
            questionText: q.questionText,
            options: q.options,
            correctAnswer: q.correctAnswer,
            analysis: q.analysis,
            type: q.type,
            difficulty: q.difficulty
        }))]

        this.setData({
            questions,
            showAddPanel: false,
            pendingQuestions: [],
            selectedPendingCount: 0
        })

        wx.showToast({ title: `已添加${selectedQuestions.length}道题目`, icon: 'success' })
    },

    // 手动添加空白题目
    addEmptyQuestion() {
        wx.navigateTo({
            url: `/pages/question-edit/question-edit?assignmentId=${this.data.assignmentId}&index=new`
        })
    },

    // 保存修改
    async saveAssignment() {
        const { assignmentId, title, description, deadline, questions } = this.data

        if (!title) {
            wx.showToast({ title: '请输入作业标题', icon: 'none' })
            return
        }

        if (questions.length === 0) {
            wx.showToast({ title: '请至少保留一道题目', icon: 'none' })
            return
        }

        wx.showLoading({ title: '保存中...' })

        try {
            const res = await api.updateAssignment(assignmentId, {
                title,
                description,
                deadline,
                questions: questions.map((q, index) => ({
                    questionId: q.questionId || Date.now() + index,
                    questionText: q.questionText,
                    options: JSON.stringify(q.options || []),
                    answer: q.correctAnswer,
                    analysis: q.analysis || '',
                    type: q.type || 1,
                    difficulty: q.difficulty || 2
                }))
            })

            wx.hideLoading()

            if (res.code === 200) {
                wx.showToast({ title: '保存成功', icon: 'success' })
                setTimeout(() => wx.navigateBack(), 1500)
            } else {
                wx.showToast({ title: res.msg || '保存失败', icon: 'none' })
            }
        } catch (err) {
            wx.hideLoading()
            console.error('保存失败:', err)
            wx.showToast({ title: '保存失败', icon: 'none' })
        }
    }
})
