const api = require('../../utils/api.js')

Page({
  data: {
    score: 0,
    notices: [],
    userInfo: null,
    isLoading: false,
    assignments: [],
    unreadReminderCount: 0,
    latestReminders: []
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 0 })
    }
    this.loadData()
  },


  // 加载待完成测验
  async loadAssignments() {
    try {
      const userInfo = wx.getStorageSync('userInfo')
      if (!userInfo || !userInfo.id) {
        this.setData({ assignments: [] })
        return
      }

      const examRes = await api.getStudentAssignments({ studentId: userInfo.id })
      console.log('测验数据:', examRes)

      if (examRes.code === 200 && examRes.data) {
        const assignments = examRes.data.filter(exam => !exam.completed)
        this.setData({ assignments })
      } else {
        this.setData({ assignments: [] })
      }
    } catch (err) {
      console.error('加载测验失败:', err)
      this.setData({ assignments: [] })
    }
  },

  // 加载未读提醒
    async loadData() {
        this.setData({ isLoading: true })
        console.log('=== loadData: 开始加载数据 ===')

        try {
            const userInfo = wx.getStorageSync('userInfo')
            console.log('=== loadData: 用户信息 ===', userInfo ? '存在' : '不存在', userInfo ? userInfo.id : '')

            if (userInfo) {
                this.setData({
                    score: userInfo.totalScore || 0,
                    userInfo: userInfo
                })
                try {
                    const res = await api.getUserInfo(userInfo.id)
                    if (res.code === 200 && res.data) {
                        const fresh = { ...userInfo, totalScore: res.data.totalScore || 0 }
                        wx.setStorageSync('userInfo', fresh)
                        this.setData({ score: fresh.totalScore || 0, userInfo: fresh })
                    }
                } catch (e) {}
            }

            const noticeRes = await api.getNotices()
            this.setData({
                notices: noticeRes.data || []
            })

            await this.loadAssignments()

            console.log('=== loadData: 开始加载未读提醒 ===')
            await this.loadUnreadReminders()
            console.log('=== loadData: 加载完成 ===', {
                score: this.data.score,
                notices: this.data.notices.length,
                assignments: this.data.assignments.length,
                unreadReminders: this.data.unreadReminderCount,
                latestReminders: this.data.latestReminders.length
            })
        } catch (err) {
            console.error('=== loadData: 加载失败 ===', err)
        } finally {
            this.setData({ isLoading: false })
        }
    },



    async loadUnreadReminders() {
        try {
            const userInfo = wx.getStorageSync('userInfo')
            if (!userInfo || !userInfo.id) {
                console.log('=== loadUnreadReminders: 用户信息不存在 ===')
                return
            }

            console.log('=== loadUnreadReminders: 开始加载 ===', 'userId:', userInfo.id)
            const res = await api.getMyReminders(userInfo.id)
            console.log('=== loadUnreadReminders: 后端返回 ===', JSON.stringify(res))

            if (res.code === 200 && res.data) {
                console.log('=== loadUnreadReminders: 提醒数据 ===', JSON.stringify(res.data))
                console.log('=== loadUnreadReminders: 数据条数 ===', res.data.length)

                const unread = res.data.filter(r => {
                    console.log('=== 检查提醒 ===', r.id, 'isRead:', r.isRead, 'message:', r.message)
                    return !r.isRead
                })

                console.log('=== loadUnreadReminders: 未读数量 ===', unread.length)
                console.log('=== loadUnreadReminders: 最新3条 ===', JSON.stringify(res.data.slice(0, 3)))

                this.setData({
                    unreadReminderCount: unread.length,
                    latestReminders: (res.data || []).slice(0, 3)
                }, () => {
                    console.log('=== loadUnreadReminders: setData完成 ===', {
                        count: this.data.unreadReminderCount,
                        reminders: this.data.latestReminders.length
                    })
                })
            } else {
                console.log('=== loadUnreadReminders: 返回数据异常 ===', res)
            }
        } catch (err) {
            console.error('=== loadUnreadReminders: 加载失败 ===', err)
        }
    },


    // 跳转到提醒列表
  goToReminders() {
    wx.navigateTo({ url: '/pages/reminder-list/reminder-list' })
  },

  // 跳转到最新测验列表
  goToLatestExams() {
    wx.navigateTo({
      url: '/pages/student-exam-config/student-exam-config'
    })
  },

  // 跳转到测验详情
  goToExamDetail(e) {
    const examId = e.currentTarget.dataset.id
    const userInfo = wx.getStorageSync('userInfo')
    if (!userInfo) {
      wx.showModal({
        title: '提示',
        content: '请先登录后再使用测验功能',
        success: (res) => {
          if (res.confirm) {
            wx.navigateTo({ url: '/pages/login/login' })
          }
        }
      })
      return
    }
    wx.navigateTo({
      url: `/pages/student-exam-config/student-exam-config?examId=${examId}`
    })
  },

  // ====================== 【我新加的方法：跳转到综合测验】 ======================
  goToExam() {
    const userInfo = wx.getStorageSync('userInfo')
    if (!userInfo) {
      wx.showModal({
        title: '提示',
        content: '请先登录后再使用测验功能',
        success: (res) => {
          if (res.confirm) {
            wx.navigateTo({ url: '/pages/login/login' })
          }
        }
      })
      return
    }

    wx.navigateTo({
      url: '/pages/exam/exam'
    })
  },

  // 页面导航跳转
  navigateTo(e) {
    const url = e.currentTarget.dataset.url
    if (url) {
      const userInfo = wx.getStorageSync('userInfo')
      if (!userInfo && this.needLogin(url)) {
        wx.showModal({
          title: '提示',
          content: '请先登录后再使用此功能',
          success: (res) => {
            if (res.confirm) {
              wx.navigateTo({ url: '/pages/login/login' })
            }
          }
        })
        return
      }
      wx.navigateTo({ url })
    }
  },

  // 检查是否需要登录
  needLogin(url) {
    const loginRequiredPages = [
      '/pages/exam/exam',
      '/pages/game-index/game-index',
      '/pages/leaderboard/leaderboard',
      '/pages/achievements/achievements'
    ]
    return loginRequiredPages.some(page => url.includes(page))
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.loadData().then(() => {
      wx.stopPullDownRefresh()
    })
  },

  // 处理分享
  onShareAppMessage() {
    return {
      title: '民族团结促进法学习',
      path: '/pages/index/index',
      imageUrl: '/images/share-cover.png'
    }
  },

  // 页面加载
  onLoad(options) {
    if (options.from) {
      console.log('来自:', options.from)
    }
  },

  // 页面隐藏
  onHide() {},

  // 页面卸载
  onUnload() {}
})