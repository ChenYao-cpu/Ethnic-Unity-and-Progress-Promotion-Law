const api = require('../../utils/api.js')

Page({
    data: {
        assignmentId: null,
        questionIndex: -1,
        isNew: true,

        // 题目字段
        questionText: '',
        optionA: '',
        optionB: '',
        optionC: '',
        optionD: '',
        correctAnswer: 'A',
        analysis: '',
        questionType: 1,
        difficulty: 2,

        questionTypes: ['单选题', '多选题'],
        questionTypeIndex: 0,
        difficulties: ['简单', '中等', '困难'],
        difficultyIndex: 1,
        answerOptions: ['A', 'B', 'C', 'D'],
        answerIndex: 0,

        userInfo: null
    },

    onLoad(options) {
        const userInfo = wx.getStorageSync('userInfo')
        if (!userInfo || (userInfo.role !== 'teacher' && userInfo.role !== 'admin')) {
            wx.redirectTo({ url: '/pages/login/login' })
            return
        }

        this.setData({ userInfo, assignmentId: options.assignmentId })

        // 如果有题目数据，解析
        if (options.question) {
            try {
                const question = JSON.parse(decodeURIComponent(options.question))
                const options_arr = question.options || []

                // 找到正确答案索引
                let answerIdx = 0
                const answerOptions = ['A', 'B', 'C', 'D']
                const correctAns = question.correctAnswer || question.answer || 'A'
                answerIdx = answerOptions.indexOf(correctAns)
                if (answerIdx < 0) answerIdx = 0

                // 找到类型索引
                let typeIdx = (question.type || question.questionType || 1) - 1
                if (typeIdx < 0 || typeIdx > 1) typeIdx = 0

                // 找到难度索引
                let diffIdx = (question.difficulty || 2) - 1
                if (diffIdx < 0 || diffIdx > 2) diffIdx = 1

                this.setData({
                    isNew: options.index === 'new',
                    questionIndex: parseInt(options.index) >= 0 ? parseInt(options.index) : -1,
                    questionText: question.questionText || '',
                    optionA: options_arr[0] || '',
                    optionB: options_arr[1] || '',
                    optionC: options_arr[2] || '',
                    optionD: options_arr[3] || '',
                    correctAnswer: correctAns,
                    analysis: question.analysis || '',
                    questionType: question.type || question.questionType || 1,
                    difficulty: question.difficulty || 2,
                    questionTypeIndex: typeIdx,
                    difficultyIndex: diffIdx,
                    answerIndex: answerIdx
                })
            } catch (e) {
                console.error('解析题目数据失败', e)
                this.setData({ isNew: true })
            }
        }
    },

    goBack() {
        // 如果有修改，返回数据给上一页
        const pages = getCurrentPages()
        const prevPage = pages[pages.length - 2]
        if (prevPage && this.data.questionIndex >= 0) {
            // 构造题目数据
            const question = this.buildQuestionData()
            const questions = prevPage.data.questions || []
            questions[this.data.questionIndex] = question
            prevPage.setData({ questions })
        }
        wx.navigateBack()
    },

    buildQuestionData() {
        const { questionText, optionA, optionB, optionC, optionD, correctAnswer, analysis, questionType, difficulty } = this.data
        return {
            questionText: questionText,
            options: [optionA, optionB, optionC, optionD].filter(o => o),
            correctAnswer: correctAnswer,
            analysis: analysis,
            type: questionType,
            difficulty: difficulty
        }
    },

    onTextInput(e) {
        this.setData({ questionText: e.detail.value })
    },

    onOptionAInput(e) { this.setData({ optionA: e.detail.value }) },
    onOptionBInput(e) { this.setData({ optionB: e.detail.value }) },
    onOptionCInput(e) { this.setData({ optionC: e.detail.value }) },
    onOptionDInput(e) { this.setData({ optionD: e.detail.value }) },

    onAnalysisInput(e) {
        this.setData({ analysis: e.detail.value })
    },

    onTypeChange(e) {
        const idx = parseInt(e.detail.value)
        this.setData({
            questionTypeIndex: idx,
            questionType: idx + 1
        })
    },

    onDifficultyChange(e) {
        const idx = parseInt(e.detail.value)
        this.setData({
            difficultyIndex: idx,
            difficulty: idx + 1
        })
    },

    onAnswerChange(e) {
        const idx = parseInt(e.detail.value)
        this.setData({
            answerIndex: idx,
            correctAnswer: this.data.answerOptions[idx]
        })
    },

    async saveQuestion() {
        const { questionText, optionA, optionB, optionC, optionD, correctAnswer } = this.data

        if (!questionText) {
            wx.showToast({ title: '请输入题目内容', icon: 'none' })
            return
        }

        if (!optionA || !optionB) {
            wx.showToast({ title: '请至少输入选项A和B', icon: 'none' })
            return
        }

        if (!correctAnswer) {
            wx.showToast({ title: '请选择正确答案', icon: 'none' })
            return
        }

        // 返回上一页并传递数据
        const pages = getCurrentPages()
        const prevPage = pages[pages.length - 2]
        if (prevPage) {
            const question = this.buildQuestionData()
            if (this.data.isNew) {
                // 新题目，添加到列表
                const questions = prevPage.data.questions || []
                questions.push(question)
                prevPage.setData({ questions })
                wx.showToast({ title: '题目已添加', icon: 'success' })
            } else {
                // 编辑模式，更新题目
                const questions = prevPage.data.questions || []
                if (this.data.questionIndex >= 0 && this.data.questionIndex < questions.length) {
                    questions[this.data.questionIndex] = { ...questions[this.data.questionIndex], ...question }
                    prevPage.setData({ questions })
                }
                wx.showToast({ title: '题目已更新', icon: 'success' })
            }
        }

        setTimeout(() => wx.navigateBack(), 500)
    }
})
