// D:\idea_code\law-study-system\frontend\components\notice-item\notice-item.js
Component({
  properties: {
    notice: {
      type: Object,
      value: {}
    }
  },

  methods: {
    handleClick() {
      this.triggerEvent('click', {
        noticeId: this.data.notice.id
      })
    }
  }
})
