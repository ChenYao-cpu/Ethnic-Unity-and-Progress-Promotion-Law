const api = require('../../utils/api.js')

Page({
  openTutor() { const q=this.data.questions[this.data.currentIndex]; if(q) wx.navigateTo({url:'/pages/ai-tutor/ai-tutor?id='+q.id+'&answer='+encodeURIComponent(this.data.userAnswer||'')}) },

  data: {
    questions: [],
    currentIndex: 0,
    userAnswer: '',
    showResult: false,
    isCorrect: false,
    score: 0
  },

  onLoad() {
    this.getScore()
    this.loadQuestions()
  },

  onShow() {
    this.getScore()
  },

  getScore() {
    const userInfo = wx.getStorageSync('userInfo');
    if (userInfo) {
      this.setData({
        score: userInfo.totalScore || 0
      });
    }
  },

  goBack() {
    wx.reLaunch({
      url: '/pages/index/index'
    })
  },

  async loadQuestions() {
    wx.showLoading({ title: '加载中' })
    try {
      const res = await api.getQuestions(1)
      const questions = (res.data || []).map(q => {
        let options = []
        try {
          const opts = JSON.parse(q.options)
          if (Array.isArray(opts)) {
            options = opts.map((opt, index) => ({
              key: String.fromCharCode(65 + index),
              value: opt
            }))
          }
        } catch (e) {
          console.error('解析选项失败', e)
        }
        return {
          ...q,
          options: options
        }
      })

      this.setData({
        questions: questions,
        currentIndex: 0,
        userAnswer: '',
        showResult: false
      })
    } catch (err) {
      wx.showToast({ title: '加载失败', icon: 'none' })
    }
    wx.hideLoading()
  },

  selectAnswer(e) {
    if (this.data.showResult) return

    const answer = e.currentTarget.dataset.answer
    this.setData({ userAnswer: answer })
  },

  async submitAnswer() {
    if (!this.data.userAnswer) {
      wx.showToast({ title: '请选择答案', icon: 'none' })
      return
    }

    const { questions, currentIndex, userAnswer } = this.data
    const question = questions[currentIndex]
    const userInfo = wx.getStorageSync('userInfo')

    try {
      const res = await api.checkAnswer(userInfo.id, question.id, userAnswer)

      this.setData({
        showResult: true,
        isCorrect: res.data.correct
      })

      if (res.data.correct) {
        this.setData({ score: this.data.score + 10 })
      }
    } catch (err) {
      wx.showToast({ title: '提交失败', icon: 'none' })
    }
  },

  nextQuestion() {
    const { currentIndex, questions } = this.data

    if (currentIndex < questions.length - 1) {
      this.setData({
        currentIndex: currentIndex + 1,
        userAnswer: '',
        showResult: false
      })
    } else {
      wx.showModal({
        title: '练习完成',
        content: `得分：${this.data.score}`,
        showCancel: false,
        success: () => {
          this.loadQuestions()
        }
      })
    }
  }
})
