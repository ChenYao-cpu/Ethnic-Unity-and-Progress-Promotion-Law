const api = require('../../utils/api.js')

Page({
    data: {
        // 学校/单位选择
        schools: [
            '西南民族大学', '中央民族大学', '中南民族大学', '西北民族大学',
            '北方民族大学', '大连民族大学', '湖北民族大学', '广西民族大学',
            '云南民族大学', '贵州民族大学', '西藏民族大学', '内蒙古民族大学',
            '青海民族大学', '四川民族学院', '其他院校'
        ],
        schoolIndex: -1,
        schoolInput: '',

        // 身份选择
        identity: {
            party_member: false,
            admin: false,
            legal_officer: false
        },

        // 账号信息
        username: '',
        password: '',
        confirmPassword: '',
        showPassword: false,
        showConfirmPassword: false,

        // 邮箱验证
        email: '',
        verifyCode: '',
        sendingCode: false,
        codeCountdown: 0,

        // 背景装饰
        danghuiPositions: []
    },

    onLoad() {
        this.generateDanghuiBackground();
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

    // 学校/单位 下拉选择
    onSchoolChange(e) {
        const idx = parseInt(e.detail.value);
        this.setData({
            schoolIndex: idx,
            schoolInput: this.data.schools[idx]
        });
    },

    // 学校/单位 手动输入
    onSchoolInput(e) {
        this.setData({
            schoolInput: e.detail.value,
            schoolIndex: -1  // 手动输入时重置下拉
        });
    },

    // 身份选择（chip点击）
    onIdentityTap(e) {
        const key = e.currentTarget.dataset.key;
        const identity = { ...this.data.identity };

        if (key === 'admin') {
            if (identity.admin) {
                // 取消管理员 → 只取消管理员，保留党员
                identity.admin = false;
            } else {
                // 选中管理员 → 取消法务人员，自动勾选党员
                identity.admin = true;
                identity.legal_officer = false;
                identity.party_member = true;
            }
        } else if (key === 'legal_officer') {
            if (identity.legal_officer) {
                // 取消法务人员
                identity.legal_officer = false;
            } else {
                // 选中法务人员 → 取消管理员，取消党员
                identity.legal_officer = true;
                identity.admin = false;
                identity.party_member = false;
            }
        } else if (key === 'party_member') {
            if (identity.party_member) {
                // 如果管理员已选中，党员不能取消
                if (identity.admin) {
                    wx.showToast({ title: '管理员必须拥有党员身份', icon: 'none' });
                    return;
                }
                // 取消党员
                identity.party_member = false;
            } else {
                // 选中党员 → 只选党员，不冲突
                identity.party_member = true;
            }
        }

        this.setData({ identity });
    },

    // 通用输入
    onInput(e) {
        const field = e.currentTarget.dataset.field;
        this.setData({ [field]: e.detail.value });
    },

    // 密码可见性切换（长按显示，松开隐藏）
    onPasswordTouchStart(e) {
        const field = e.currentTarget.dataset.field;
        const showField = field === 'confirmPassword' ? 'showConfirmPassword' : 'showPassword';
        this.setData({ [showField]: true });
    },

    onPasswordTouchEnd(e) {
        const field = e.currentTarget.dataset.field;
        const showField = field === 'confirmPassword' ? 'showConfirmPassword' : 'showPassword';
        this.setData({ [showField]: false });
    },

    // 发送验证码
    sendVerifyCode() {
        const { email, sendingCode, codeCountdown } = this.data;

        if (sendingCode || codeCountdown > 0) return;

        if (!email) {
            wx.showToast({ title: '请先输入邮箱', icon: 'none' });
            return;
        }

        const emailReg = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
        if (!emailReg.test(email)) {
            wx.showToast({ title: '邮箱格式不正确', icon: 'none' });
            return;
        }

        this.setData({ sendingCode: true });

        // 调用后端发送验证码
        api.sendVerifyCode(email).then(res => {
            if (res.code === 200) {
                wx.showToast({ title: '验证码已发送', icon: 'success' });
                this.startCountdown();
            } else {
                wx.showToast({ title: res.msg || '发送失败，请检查邮箱地址', icon: 'none' });
                this.setData({ sendingCode: false });
            }
        }).catch(() => {
            wx.showToast({ title: '请求失败，请检查网络', icon: 'none' });
            this.setData({ sendingCode: false });
        });
    },

    startCountdown() {
        this.setData({ sendingCode: false, codeCountdown: 60 });
        const timer = setInterval(() => {
            if (this.data.codeCountdown <= 1) {
                clearInterval(timer);
                this.setData({ codeCountdown: 0 });
            } else {
                this.setData({ codeCountdown: this.data.codeCountdown - 1 });
            }
        }, 1000);
    },

    goBack() {
        wx.navigateBack({
            delta: 1,
            fail: () => {
                wx.switchTab({
                    url: '/pages/index/index'
                });
            }
        });
    },

    async handleRegister() {
        const { username, password, confirmPassword, email, verifyCode, identity, schoolInput } = this.data;

        // 校验学校
        if (!schoolInput.trim()) {
            wx.showToast({ title: '请选择或输入学校/单位', icon: 'none' });
            return;
        }

        // 校验身份选择
        if (!identity.party_member && !identity.admin && !identity.legal_officer) {
            wx.showToast({ title: '请选择身份', icon: 'none' });
            return;
        }

        // 校验用户名和密码
        if (!username || !password) {
            wx.showToast({ title: '请填写用户名和密码', icon: 'none' });
            return;
        }

        if (password.length < 6) {
            wx.showToast({ title: '密码至少6位', icon: 'none' });
            return;
        }

        if (password !== confirmPassword) {
            wx.showToast({ title: '两次密码输入不一致', icon: 'none' });
            return;
        }

        // 校验邮箱
        if (!email) {
            wx.showToast({ title: '请填写邮箱', icon: 'none' });
            return;
        }

        const emailReg = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
        if (!emailReg.test(email)) {
            wx.showToast({ title: '邮箱格式不正确', icon: 'none' });
            return;
        }

        if (!verifyCode) {
            wx.showToast({ title: '请输入验证码', icon: 'none' });
            return;
        }

        // 确定主角色
        let role = 'party_member';
        if (identity.admin) {
            role = 'admin';
        } else if (identity.legal_officer) {
            role = 'legal_officer';
        }

        wx.showLoading({ title: '注册中...' });

        try {
            const res = await api.register({
                username,
                password,
                school: schoolInput.trim(),
                role: role,
                identity: JSON.stringify(identity),
                email: email,
                verifyCode: verifyCode
            });

            if (res.code === 200) {
                const userInfo = {
                    ...res.data,
                    role: role,
                    identity: identity,
                    school: schoolInput.trim()
                };
                wx.setStorageSync('userInfo', userInfo);

                wx.showToast({ title: '注册成功，审核已通过', icon: 'success' });

                setTimeout(() => {
                    wx.hideLoading();
                    if (role === 'admin') {
                        wx.reLaunch({ url: '/pages/teacher-home/teacher-home' });
                    } else if (role === 'legal_officer') {
                        wx.reLaunch({ url: '/pages/legal-officer-home/legal-officer-home' });
                    } else {
                        wx.switchTab({ url: '/pages/index/index' });
                    }
                }, 1500);
            } else {
                wx.showToast({ title: res.msg || '注册失败', icon: 'none' });
            }
        } catch (err) {
            console.error('注册失败详情:', JSON.stringify(err));
            const msg = (err && err.data && err.data.msg) || (err && err.msg) || '网络错误';
            wx.showToast({ title: msg, icon: 'none', duration: 3000 });
        } finally {
            wx.hideLoading();
        }
    }
})
