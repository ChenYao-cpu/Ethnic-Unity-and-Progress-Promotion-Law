const api = require('../../utils/api.js')

Page({
  data: {
    isLogin: false,
    userInfo: {},
    identityRoles: [],
    isAdmin: false,
    score: 0,
    treeImg: '/images/tree/tree1.png',
    treeLevel: 1,
    treeExp: 0,
    treeMaxExp: 100,
    isGrowing: false,
    watering: false
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 2 })
    }
    this.checkLoginStatus()
    this.loadTreeData()
  },

  async loadMyData(){this.setData({profileLoading:true,profileError:''});try{await this.checkLoginStatus();if(this.data.isLogin)await this.loadTreeData()}catch(error){this.setData({profileError:error.message||'个人学习信息读取失败，请重试。'})}finally{this.setData({profileLoading:false})}},
  async checkLoginStatus() {
    const userInfo = wx.getStorageSync('userInfo')
    if (userInfo && userInfo.id) {
      // 解析 identity JSON 字段
      let identityRoles = []
      if (userInfo.identity) {
        try {
          const identityObj = typeof userInfo.identity === 'string'
            ? JSON.parse(userInfo.identity)
            : userInfo.identity
          if (identityObj.party_member) identityRoles.push({ key: 'party_member', label: '党员' })
          if (identityObj.admin) identityRoles.push({ key: 'admin', label: '管理员' })
          if (identityObj.legal_officer) identityRoles.push({ key: 'legal_officer', label: '法务人员' })
        } catch (e) {
          // identity 解析失败时用 role 兜底
          if (userInfo.role === 'admin' || userInfo.role === 'teacher') {
            identityRoles.push({ key: 'admin', label: '管理员' })
          } else if (userInfo.role === 'legal_officer') {
            identityRoles.push({ key: 'legal_officer', label: '法务人员' })
          } else {
            identityRoles.push({ key: 'party_member', label: '党员' })
          }
        }
      } else {
        // 没有 identity 字段时用 role 兜底
        if (userInfo.role === 'admin' || userInfo.role === 'teacher') {
          identityRoles.push({ key: 'admin', label: '管理员' })
          identityRoles.push({ key: 'party_member', label: '党员' })
        } else if (userInfo.role === 'legal_officer') {
          identityRoles.push({ key: 'legal_officer', label: '法务人员' })
        } else {
          identityRoles.push({ key: 'party_member', label: '党员' })
        }
      }

      // 是否拥有管理员身份
      const isAdmin = identityRoles.some(r => r.key === 'admin')

      this.setData({
        isLogin: true,
        userInfo: userInfo,
        identityRoles: identityRoles,
        isAdmin: isAdmin,
        score: userInfo.totalScore || 0
      })
    } else {
      this.setData({ isLogin: false, identityRoles: [] })
    }
  },

  async loadTreeData(){const user=wx.getStorageSync('userInfo');if(!user?.id)return;const res=await api.getKnowledgeTree(user.id);if(res.code!==200||!res.data)throw Error(res.msg||'学习成长信息读取失败');this.setData({treeLevel:res.data.level,treeExp:Number(res.data.growthValue)%100,treeMaxExp:100})},

  goToLogin() {
    wx.navigateTo({ url: '/pages/login/login' })
  },

  switchToAdmin() {
    wx.reLaunch({ url: '/pages/teacher-home/teacher-home' })
  },

  navTo(e) {
    const url = e.currentTarget.dataset.url
    if (url) {
      wx.navigateTo({ url })
    }
  },

  async feedTree(){if(this.data.watering)return;const user=wx.getStorageSync('userInfo');if(!user?.id)return;this.setData({watering:true});try{const res=await api.growTree({userId:user.id,points:10});if(res.code!==200)throw Error(res.msg||'浇灌失败');this.setData({treeLevel:res.data.level,treeExp:Number(res.data.growthValue)%100});await this.checkLoginStatus();wx.showToast({title:'浇灌已记录',icon:'success'})}catch(error){wx.showToast({title:error.message||'浇灌失败，请重试',icon:'none'})}finally{this.setData({watering:false})}},

  logout() {
    wx.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          wx.removeStorageSync('userInfo')
          wx.removeStorageSync('treeData')
          this.setData({
            isLogin: false,
            userInfo: {},
            score: 0,
            treeLevel: 1,
            treeExp: 0,
            treeMaxExp: 100
          })
          wx.showToast({ title: '已退出登录', icon: 'success' })
        }
      }
    })
  }
})
