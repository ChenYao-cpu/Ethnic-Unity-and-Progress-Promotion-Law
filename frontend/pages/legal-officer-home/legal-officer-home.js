const { BASE_URL } = require('../../utils/config.js')
Page({
 data:{webUrl:BASE_URL.replace(/\/api\/?$/, '')+'/console/'},
 copyUrl(){wx.setClipboardData({data:this.data.webUrl})},
 goStudy(){wx.switchTab({url:'/pages/index/index'})}
})
