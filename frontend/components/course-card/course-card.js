// D:\idea_code\law-study-system\frontend\components\course-card\course-card.js
Component({
  properties: {
    course: {
      type: Object,
      value: {}
    }
  },

  methods: {
    handleClick() {
      this.triggerEvent('click', {
        courseId: this.data.course.id
      })
    }
  }
})
