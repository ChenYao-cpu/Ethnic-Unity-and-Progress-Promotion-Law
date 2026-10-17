const api = require('../../utils/api.js')

Page({
    data: {
        username: '',
        password: '',
        showPassword: false,
        danghuiPositions: []
    },

    onLoad() {
        this.generateDanghuiBackground();
    },

    goBack() {
        wx.navigateBack({
            delta: 1,
            fail: () => {
                wx.switchTab({
                    url: '/pages/index/index'
                })
            }
        });
    },

    generateDanghuiBackground() {
        const positions = [];
        const rows = 8;
        const cols = 4;

        for (let i = 0; i < rows; i++) {
            for (let j = 0; j < cols; j++) {
                positions.push({
                    top: (i * 200) + 'rpx',
                    left: (j * 250 + (i % 2 === 0 ? 0 : 100)) + 'rpx'
                });
            }
        }

        this.setData({ danghuiPositions: positions });
    },

    onUsernameInput(e) {
        this.setData({ username: e.detail.value })
    },

    onPasswordInput(e) {
        this.setData({ password: e.detail.value })
    },

    onPasswordTouchStart() {
        this.setData({ showPassword: true })
    },

    onPasswordTouchEnd() {
        this.setData({ showPassword: false })
    },

    async handleLogin() {
        const { username, password } = this.data

        if (!username || !password) {
            wx.showToast({ title: '请输入用户名和密码', icon: 'none' })
            return
        }

        wx.showLoading({ title: '登录中...', mask: true })

        try {
            const res = await api.login({ username, password })

            if (res.code === 200) {
                // 后端返回用户信息
                const userInfo = res.data
                const role = userInfo.role || 'party_member'

                // 先保存基本信息到 storage，立即完成登录
                wx.setStorageSync('userInfo', {
                    ...userInfo,
                    role: role
                })

                // 隐藏 loading，立即跳转
                wx.hideLoading()
                wx.showToast({ title: '登录成功', icon: 'success', duration: 1000 })

                // 异步拉取完整用户信息（不阻塞跳转）
                if (userInfo.id) {
                    api.getUserInfo(userInfo.id).then(infoRes => {
                        if (infoRes.code === 200 && infoRes.data) {
                            const cached = wx.getStorageSync('userInfo') || {}
                            wx.setStorageSync('userInfo', {
                                ...cached,
                                ...infoRes.data,
                                role: role
                            })
                        }
                    }).catch(e => {
                        console.error('获取用户完整信息失败:', e)
                    })
                }

                // 登录成功后立即跳转
                if (role === 'admin' || role === 'teacher') {
                    wx.redirectTo({
                        url: '/pages/teacher-home/teacher-home',
                        fail: (err) => {
                            console.error('跳转教师端失败:', err)
                            wx.switchTab({ url: '/pages/index/index' })
                        }
                    })
                } else if (role === 'legal_officer') {
                    wx.redirectTo({
                        url: '/pages/legal-officer-home/legal-officer-home',
                        fail: (err) => {
                            console.error('跳转法务端失败:', err)
                            wx.switchTab({ url: '/pages/index/index' })
                        }
                    })
                } else {
                    // party_member 或 student
                    wx.switchTab({ url: '/pages/index/index' })
                }
            } else {
                wx.hideLoading()
                wx.showToast({ title: res.msg || '登录失败', icon: 'none' })
            }
        } catch (err) {
            console.error('登录请求失败:', err)
            wx.hideLoading()
            wx.showToast({ title: '网络请求失败，请检查网络', icon: 'none' })
        }
    },

    goRegister() {
        wx.navigateTo({ url: '/pages/register/register' })
    },

    forgotPassword() {
        wx.showToast({ title: '请联系管理员重置密码', icon: 'none' })
    }
})
