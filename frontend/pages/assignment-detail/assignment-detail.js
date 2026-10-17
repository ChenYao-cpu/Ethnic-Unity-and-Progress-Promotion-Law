const app = getApp()
const api = require('../../utils/api.js')

// 本地错题缓存按账号隔离
const storageKey = (name) => {
    const u = wx.getStorageSync('userInfo') || {};
    return name + '_' + (u.id || 'guest');
};

Page({
    openTutor() { const q=this.data.questions[this.data.currentIndex]; if(q) wx.navigateTo({url:'/pages/ai-tutor/ai-tutor?id='+q.id}) },

    data: {
        assignment: null,
        questions: [],
        currentIndex: 0,
        selectedMap: {},
        timeLeft: 0,
        timeLeftText: '00:00',
        timer: null,
        optionLabels: ['A', 'B', 'C', 'D', 'E'],
        examResult: null,
        loading: true,
        correctCount: 0,
        isCompleted: false,
        userScore: 0
    },

    async onLoad(options) {
        console.log('作业 ID:', options.id)
        const assignmentId = options.id
        const isCompleted = options.completed === 'true'

        this.setData({ isCompleted: isCompleted })

        if (!assignmentId) {
            wx.showToast({ title: '作业 ID 无效', icon: 'none' })
            setTimeout(() => wx.navigateBack(), 1500)
            return
        }
        await this.loadAssignmentDetail(assignmentId)

        if (isCompleted) {
            await this.loadUserScore(assignmentId)
        }
    },

    async loadUserScore(assignmentId) {
        try {
            const userInfo = wx.getStorageSync('userInfo')
            const res = await api.getAssignmentSubmission(assignmentId, userInfo.id)
            if (res.code === 200 && res.data) {
                this.setData({ userScore: res.data.score })
            }
        } catch (err) {
            console.error('加载得分失败:', err)
        }
    },

    async loadAssignmentDetail(assignmentId) {
        this.setData({ loading: true })
        wx.showLoading({ title: '加载中...' })

        try {
            const res = await api.getAssignmentDetail(assignmentId)

            if (res && res.code === 200) {
                const assignment = res.data

                if (!assignment || !assignment.questions || assignment.questions.length === 0) {
                    wx.hideLoading()
                    wx.showModal({
                        title: '提示',
                        content: '该作业暂无题目',
                        showCancel: false,
                        success: () => wx.navigateBack()
                    })
                    return
                }

                const questions = assignment.questions.map((aq, index) => {
                    const question = aq.question

                    let options = []
                    if (question.options) {
                        if (typeof question.options === 'string') {
                            try {
                                options = JSON.parse(question.options)
                            } catch (e) {
                                options = []
                            }
                        } else if (Array.isArray(question.options)) {
                            options = question.options
                        }
                    }

                    return {
                        id: question.id,
                        question: question.title || '题目内容为空',
                        type: question.type === 2 ? 'multi' : 'single',
                        options: options,
                        optionsSelected: new Array(options.length).fill(false),
                        answer: question.answer || '',
                        analysis: question.analysis || '暂无解析',
                        score: 5
                    }
                })

                this.setData({
                    assignment,
                    questions,
                    loading: false
                })

                if (!this.data.isCompleted) {
                    this.startTimer()
                }

                wx.hideLoading()
            } else {
                wx.showModal({
                    title: '加载失败',
                    content: res.msg || '无法获取作业信息',
                    showCancel: false,
                    success: () => wx.navigateBack()
                })
            }
        } catch (err) {
            console.error('请求异常:', err)
            wx.showModal({
                title: '网络错误',
                content: '请检查网络连接',
                showCancel: false,
                success: () => wx.navigateBack()
            })
        } finally {
            this.setData({ loading: false })
            wx.hideLoading()
        }
    },

    startTimer() {
        if (this.data.timer) clearInterval(this.data.timer)
        this.data.timer = setInterval(() => {
            const newTime = this.data.timeLeft + 1
            this.setData({
                timeLeft: newTime,
                timeLeftText: this.formatTime(newTime)
            })
        }, 1000)
    },

    formatTime(seconds) {
        const m = Math.floor(seconds / 60)
        const s = seconds % 60
        return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
    },

    selectAnswer(e) {
        if (this.data.isCompleted) return

        const { idx } = e.currentTarget.dataset
        const currentIndex = this.data.currentIndex
        const question = this.data.questions[currentIndex]

        if (!question) return

        const label = this.data.optionLabels[idx]
        let currentSelection = this.data.selectedMap[question.id]

        if (currentSelection === undefined) {
            currentSelection = question.type === 'multi' ? [] : ''
        }

        let newSelection

        if (question.type === 'multi') {
            const arr = Array.isArray(currentSelection) ? [...currentSelection] : []
            const index = arr.indexOf(label)
            if (index > -1) {
                arr.splice(index, 1)
            } else {
                arr.push(label)
            }
            newSelection = arr.sort()
        } else {
            newSelection = label
        }

        const optionsSelected = question.options.map((_, i) => {
            const optLabel = this.data.optionLabels[i]
            if (question.type === 'multi') {
                return Array.isArray(newSelection) && newSelection.includes(optLabel)
            } else {
                return newSelection === optLabel
            }
        })

        const updatedQuestions = [...this.data.questions]
        updatedQuestions[currentIndex] = {
            ...question,
            optionsSelected
        }

        this.setData({
            [`selectedMap.${question.id}`]: newSelection,
            questions: updatedQuestions
        })
    },

    prevQuestion() {
        if (this.data.currentIndex > 0) {
            this.setData({ currentIndex: this.data.currentIndex - 1 })
        }
    },

    nextQuestion() {
        if (this.data.currentIndex < this.data.questions.length - 1) {
            this.setData({ currentIndex: this.data.currentIndex + 1 })
        }
    },

    submitAssignment() {
        if (this.data.timer) clearInterval(this.data.timer)

        let score = 0
        let correctCount = 0
        let wrongList = []

        this.data.questions.forEach(q => {
            const userAns = this.data.selectedMap[q.id] || (q.type === 'multi' ? [] : '')
            const correct = this.checkAnswer(q, userAns)
            if (correct) {
                score += q.score || 5
                correctCount++
            } else {
                wrongList.push({
                    ...q,
                    userAnswer: Array.isArray(userAns) ? userAns.join('') || '未作答' : userAns || '未作答'
                })
            }
        })

        this.setData({
            examResult: {
                score,
                correctCount,
                wrongCount: this.data.questions.length - correctCount,
                wrongList
            },
            correctCount: correctCount
        })

        if (wrongList.length > 0) {
            this.saveWrongQuestions(wrongList)
        }

        this.saveSubmissionToBackend(score, correctCount)
    },

    saveSubmissionToBackend(score, correctCount) {
        const userInfo = wx.getStorageSync('userInfo')
        if (!userInfo || !userInfo.id) {
            console.error('用户信息不存在')
            return
        }

        const answers = {}
        this.data.questions.forEach(q => {
            const userAns = this.data.selectedMap[q.id] || (q.type === 'multi' ? [] : '')
            answers[q.id] = userAns
        })

        wx.request({
            url: `${app.globalData.baseUrl}/assignment/submit`,
            method: 'POST',
            header: { 'Content-Type': 'application/json' },
            data: {
                assignmentId: this.data.assignment.id,
                studentId: userInfo.id,
                answers: JSON.stringify(answers),
                score: score
            },
            success: (res) => {
                if (res.statusCode === 200 && res.data && res.data.code === 200) {
                    console.log('提交记录保存成功')
                }
            },
            fail: (err) => {
                console.error('提交记录保存请求失败:', err)
            }
        })
    },

    checkAnswer(q, userAns) {
        const correct = q.answer || ''
        return q.type === 'multi' ? userAns.join('') === correct : userAns === correct
    },

    saveWrongQuestions(wrongList) {
        const wrongs = wx.getStorageSync(storageKey('wrongQuestions')) || []
        wrongList.forEach(q => {
            if (!wrongs.find(w => w.question === q.question)) {
                wrongs.push({
                    id: q.id,
                    question: q.question,
                    answer: q.answer,
                    analysis: q.analysis,
                    type: q.type,
                    options: q.options
                })
            }
        })
        wx.setStorageSync(storageKey('wrongQuestions'), wrongs)
    },

    backToHome() {
        if (this.data.timer) clearInterval(this.data.timer)
        wx.navigateBack()
    },

    goBack() {
        if (this.data.timer) clearInterval(this.data.timer)
        wx.navigateBack()
    },

    onUnload() {
        if (this.data.timer) clearInterval(this.data.timer)
    }
})