Page({
  data: { loggedIn: false },
  onLoad() {
    const user = wx.getStorageSync('userInfo');
    const loggedIn = !!(user && user.id);
    this.setData({ loggedIn });
    if (loggedIn) {
      this.timer = setTimeout(() => this.enterHome(), 1700);
    }
  },
  onUnload() {
    if (this.timer) clearTimeout(this.timer);
  },
  enterHome() {
    if (this.entered) return;
    this.entered = true;
    const user = wx.getStorageSync('userInfo') || {};
    const role = user.role || 'party_member';
    if (role === 'admin' || role === 'teacher') {
      wx.reLaunch({ url: '/pages/teacher-home/teacher-home' });
    } else if (role === 'legal_officer') {
      wx.reLaunch({ url: '/pages/legal-officer-home/legal-officer-home' });
    } else {
      wx.switchTab({ url: '/pages/index/index' });
    }
  },
  goLogin() { wx.navigateTo({ url: '/pages/login/login' }); },
  goRegister() { wx.navigateTo({ url: '/pages/register/register' }); }
});
