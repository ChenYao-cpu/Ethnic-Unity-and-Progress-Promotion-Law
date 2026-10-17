const api = require('../../utils/api.js')

Page({
    data: {
        userInfo: null,
        achievements: [],
        loading: true,
        _initialized: false
    },

    onLoad() {
        const userInfo = wx.getStorageSync('userInfo')
        if (!userInfo || !userInfo.id) {
            wx.redirectTo({ url: '/pages/login/login' })
            return
        }

        this.setData({ userInfo })
        this.loadAchievements()
        this.data._initialized = true
    },

    onShow() {
        // 检查登录状态
        const userInfo = wx.getStorageSync('userInfo')
        if (!userInfo || !userInfo.id) {
            wx.redirectTo({ url: '/pages/login/login' })
            return
        }
        // 用户信息有变化时同步
        if (!this.data.userInfo || this.data.userInfo.id !== userInfo.id) {
            this.setData({ userInfo })
        }
        // 避免首次重复加载
        if (this.data._initialized) {
            this.loadAchievements()
        }
    },

    goBack() {
        wx.navigateBack({
            delta: 1,
            fail: () => {
                wx.switchTab({ url: '/pages/index/index' })
            }
        })
    },

    async loadAchievements() {
        this.setData({ loading: true })

        try {
            const res = await api.getAchievements(this.data.userInfo.id)

            if (res.code === 200 && res.data) {
                // 定义所有成就的完整列表
                const allAchievements = [
                    {
                        code: 'perfect_match',
                        name: '法条大神',
                        desc: '法条匹配首次完全正确',
                        iconClass: 'icon-fatiao1'
                    },
                    {
                        code: 'speed_master',
                        name: '闪电侠',
                        desc: '快速答题每题3秒内答对',
                        iconClass: 'icon-shandianxia'
                    },
                    {
                        code: 'first_blood',
                        name: '初出茅庐',
                        desc: '完成人生第一关',
                        iconClass: 'icon-chuchumaolu'
                    },
                    {
                        code: 'rank_first',
                        name: '冠军',
                        desc: '排行榜获得第一名',
                        iconClass: 'icon-jifen1'
                    },
                    {
                        code: 'rank_second',
                        name: '亚军',
                        desc: '排行榜获得第二名',
                        iconClass: 'icon-face_happy'
                    },
                    {
                        code: 'rank_third',
                        name: '季军',
                        desc: '排行榜获得第三名',
                        iconClass: 'icon-face_smile'
                    },
                    {
                        code: 'all_wrong',
                        name: '气氛组担当',
                        desc: '情景模拟全错',
                        iconClass: 'icon-face_frown'
                    },
                    {
                        code: 'ten_levels',
                        name: '闯关达人',
                        desc: '成功通关10个关卡',
                        iconClass: 'icon-darenrenzheng'
                    },
                    {
                        code: 'score_1000',
                        name: '积分收割机',
                        desc: '累计获得1000积分',
                        iconClass: 'icon-star-ai'
                    }
                ]

                // 将后端返回的成就数据与完整列表合并
                const unlockedMap = {}
                res.data.forEach(item => {
                    if (item.unlocked) {
                        unlockedMap[item.achievementCode || item.code] = {
                            unlocked: 1,
                            unlockTime: item.unlockTime
                        }
                    }
                })

                const achievements = allAchievements.map(item => {
                    const unlockedData = unlockedMap[item.code] || { unlocked: 0, unlockTime: null }

                    // 格式化时间
                    let unlockTime = ''
                    if (unlockedData.unlockTime) {
                        const date = new Date(unlockedData.unlockTime)
                        unlockTime = `${date.getMonth() + 1}月${date.getDate()}日`
                    }

                    return {
                        ...item,
                        unlocked: unlockedData.unlocked,
                        unlockTime: unlockTime
                    }
                })

                const unlockedAchievements = achievements.filter(a => a.unlocked)
                this.setData({
                    achievements,
                    unlockedAchievements,
                    unlockedCount: unlockedAchievements.length,
                    totalCount: achievements.length
                })
            } else {
                // 即使没有数据也显示所有成就（未解锁状态）
                const fallbackList = this.getAllAchievementsList()
                this.setData({
                    achievements: fallbackList,
                    unlockedAchievements: [],
                    unlockedCount: 0,
                    totalCount: fallbackList.length
                })
            }
        } catch (err) {
            console.error('加载成就失败', err)
            wx.showToast({ title: '加载失败', icon: 'none' })
            // 出错时也显示所有成就
            const fallbackList = this.getAllAchievementsList()
            this.setData({
                achievements: fallbackList,
                unlockedAchievements: [],
                unlockedCount: 0,
                totalCount: fallbackList.length
            })
        } finally {
            this.setData({ loading: false })
        }
    },

    // 获取所有成就列表（用于降级显示）
    getAllAchievementsList() {
        return [
            {
                code: 'perfect_match',
                name: '法条大神',
                desc: '法条匹配首次完全正确',
                iconClass: 'icon-fatiao1',
                unlocked: 0,
                unlockTime: ''
            },
            {
                code: 'speed_master',
                name: '闪电侠',
                desc: '快速答题每题3秒内答对',
                iconClass: 'icon-shandianxia',
                unlocked: 0,
                unlockTime: ''
            },
            {
                code: 'first_blood',
                name: '初出茅庐',
                desc: '完成人生第一关',
                iconClass: 'icon-chuchumaolu',
                unlocked: 0,
                unlockTime: ''
            },
            {
                code: 'rank_first',
                name: '冠军',
                desc: '排行榜获得第一名',
                iconClass: 'icon-jifen1',
                unlocked: 0,
                unlockTime: ''
            },
            {
                code: 'rank_second',
                name: '亚军',
                desc: '排行榜获得第二名',
                iconClass: 'icon-face_happy',
                unlocked: 0,
                unlockTime: ''
            },
            {
                code: 'rank_third',
                name: '季军',
                desc: '排行榜获得第三名',
                iconClass: 'icon-face_smile',
                unlocked: 0,
                unlockTime: ''
            },
            {
                code: 'all_wrong',
                name: '气氛组担当',
                desc: '情景模拟全错',
                iconClass: 'icon-face_frown',
                unlocked: 0,
                unlockTime: ''
            },
            {
                code: 'ten_levels',
                name: '闯关达人',
                desc: '成功通关10个关卡',
                iconClass: 'icon-darenrenzheng',
                unlocked: 0,
                unlockTime: ''
            },
            {
                code: 'score_1000',
                name: '积分收割机',
                desc: '累计获得1000积分',
                iconClass: 'icon-star-ai',
                unlocked: 0,
                unlockTime: ''
            }
        ]
    }
})
