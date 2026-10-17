const api = require('../../utils/api.js')

Component({
  properties: {
    messages: {
      type: Array,
      value: []
    },
    loading: {
      type: Boolean,
      value: false
    }
  },

  data: {},

  methods: {
    onScrollToLower() {
      this.triggerEvent('scrolltolower')
    }
  }
})
