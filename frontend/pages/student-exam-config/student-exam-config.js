const api = require('../../utils/api.js');

Page({
  data: {
    activeTab: 0,
    assignments: [],
    completedAssignments: [],
    userInfo: null,
    score: 0
  },

  onLoad() {
    this.userInfo = wx.getStorageSync('userInfo');
    this.getScore();
    this.loadAssignments();
  },

  onShow() {
    this.getScore();
    this.loadAssignments();
  },

  getScore() {
    const userInfo = wx.getStorageSync('userInfo');
    if (userInfo) {
      this.setData({
        score: userInfo.totalScore || 0
      });
    }
  },

    async loadAssignments() {
        this.userInfo=wx.getStorageSync('userInfo');if(!this.userInfo?.id){if(!this._redirectingLogin){this._redirectingLogin=true;wx.redirectTo({url:'/pages/login/login'})}return}

        try {
            wx.showLoading({ title: '加载中' });

            const pendingRes = await api.getStudentAssignments({ studentId: this.userInfo.id, status: 1 });
            const completedRes = await api.getStudentAssignments({ studentId: this.userInfo.id, status: 2 });

            console.log(' 未完成作业响应:', pendingRes);
            console.log(' 已完成作业响应:', completedRes);

            let assignmentsData = [];
            let completedData = [];

            if (pendingRes.code === 200 && pendingRes.data) {
                assignmentsData = pendingRes.data;
                console.log(' 未完成作业数量:', assignmentsData.length);
            }

            if (completedRes.code === 200 && completedRes.data) {
                completedData = completedRes.data;
                console.log(' 已完成作业数量:', completedData.length);
                console.log(' 已完成作业详细数据:', JSON.stringify(completedData[0], null, 2));
            }

            this.setData({
                assignments: assignmentsData,
                completedAssignments: completedData
            }, () => {
                console.log(' setData完成 - assignments:', this.data.assignments.length, 'completedAssignments:', this.data.completedAssignments.length);
                console.log(' 当前activeTab:', this.data.activeTab);

                wx.hideLoading();
            });

        } catch (err) {
            console.error(' 加载作业失败:', err);
            wx.hideLoading();
        }
    },



    switchTab(e) {
        const index = parseInt(e.currentTarget.dataset.index);
        console.log(' 切换标签 - 目标索引:', index);
        console.log(' 切换前activeTab:', this.data.activeTab);

        this.setData({
            activeTab: index
        }, () => {
            console.log(' 切换后activeTab:', this.data.activeTab);
            console.log(' 当前completedAssignments长度:', this.data.completedAssignments.length);

            setTimeout(() => {
                this.setData({
                    forceUpdate: Date.now()
                });
            }, 50);
        });
    },



    startAssignment(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/assignment-detail/assignment-detail?id=${id}`
    });
  },

  goBack() {
    wx.navigateBack({
      delta: 1,
      fail: () => {
        wx.reLaunch({
          url: '/pages/index/index'
        });
      }
    });
  }
});
