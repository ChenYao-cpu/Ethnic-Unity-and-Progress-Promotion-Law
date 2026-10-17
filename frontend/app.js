const { BASE_URL } = require('./utils/config.js')

App({
    onLaunch() {
        const userInfo = wx.getStorageSync('userInfo')
        if (userInfo) {
            this.globalData.userInfo = userInfo
        }
    },
    globalData: {
        userInfo: null,
        baseUrl: BASE_URL
    }
})
