const api = require('../../utils/api.js');

Page({
  data: {
    score: 0
  },

  onLoad() {
    this.getScore();
  },

  onShow() {
    this.getScore();
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
    });
  },

  startGame(e) {
    const type = e.currentTarget.dataset.type;
    const urlMap = {
      match: '/pages/game-match/game-match',
      quick: '/pages/game-quick/game-quick',
      scenario: '/pages/game-scenario/game-scenario'
    };

    if (urlMap[type]) {
      wx.navigateTo({
        url: urlMap[type]
      });
    } else {
      wx.showToast({
        title: '游戏暂未开放',
        icon: 'none'
      });
    }
  },

  goToAchievements() {
    const userInfo = wx.getStorageSync('userInfo')
    if (!userInfo || !userInfo.id) {
      wx.showModal({
        title: '提示',
        content: '请先登录后再查看勋章墙',
        success: (res) => {
          if (res.confirm) {
            wx.navigateTo({ url: '/pages/login/login' })
          }
        }
      })
      return
    }
    wx.navigateTo({
      url: '/pages/achievements/achievements'
    })
  },

  goToLeaderboard() {
    const userInfo = wx.getStorageSync('userInfo')
    if (!userInfo || !userInfo.id) {
      wx.showModal({
        title: '提示',
        content: '请先登录后再查看排行榜',
        success: (res) => {
          if (res.confirm) {
            wx.navigateTo({ url: '/pages/login/login' })
          }
        }
      })
      return
    }
    wx.navigateTo({
      url: '/pages/leaderboard/leaderboard'
    })
  }
});
