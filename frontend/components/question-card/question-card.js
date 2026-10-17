// D:\idea_code\law-study-system\frontend\components\question-card\question-card.js
Component({
  properties: {
    question: {
      type: Object,
      value: {}
    },
    index: {
      type: Number,
      value: 0
    },
    selectedAnswer: {
      type: String,
      value: ''
    }
  },

  data: {
    options: []
  },

  lifetimes: {
    attached() {
      this.initOptions()
    }
  },

  methods: {
    initOptions() {
      const q = this.data.question
      if (q.options) {
        this.setData({ options: q.options })
      } else if (q.optionA) {
        this.setData({
          options: [
            { key: 'A', value: q.optionA },
            { key: 'B', value: q.optionB },
            { key: 'C', value: q.optionC },
            { key: 'D', value: q.optionD }
          ]
        })
      }
    },

    handleSelect(e) {
      const answer = e.currentTarget.dataset.answer
      this.triggerEvent('select', {
        questionId: this.data.question.id,
        answer: answer,
        index: this.data.index
      })
    }
  }
})
