const app = getApp()

Page({
  data: {
    notice: {}
  },

  onLoad(options) {
    if (options.id) {
      this.loadNoticeDetail(options.id)
    }
  },

  loadNoticeDetail(id) {
    wx.showLoading({ title: '加载中' })
    wx.request({
      url: `${app.globalData.baseUrl}/notice/${id}`,
      method: 'GET',
      success: (res) => {
        if (res.data.code === 200) {
          this.setData({ notice: res.data.data })
        }
      },
      fail: () => {
        wx.showToast({ title: '加载失败', icon: 'none' })
      },
      complete: () => {
        wx.hideLoading()
      }
    })
  }
})
