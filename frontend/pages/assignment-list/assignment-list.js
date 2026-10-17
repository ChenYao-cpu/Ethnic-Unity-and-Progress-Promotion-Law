const app = getApp()
const api = require('../../utils/api.js')

Page({
    data: {
        allAssignments: [],      // 所有作业
        pendingList: [],         // 未完成列表
        completedList: [],       // 已完成列表
        activeTab: 'pending',
        loading: true
    },

    async onLoad() {
        await this.loadData()
    },

    async loadData() {
        this.setData({ loading: true })

        try {
            const userInfo = wx.getStorageSync('userInfo')
            if(!userInfo?.id){this.setData({loading:false});wx.redirectTo({url:'/pages/login/login'});return}
            const studentId = userInfo.id

            // 1. 获取所有作业
            const res = await api.getStudentAssignments({ studentId: studentId, status: 1 })
            console.log('所有作业:', res)

            if (res && res.code === 200 && res.data) {
                const all = res.data

                // 2. 获取已提交的作业ID
                const submitRes = await api.getAssignmentSubmissionList(studentId)
                if(submitRes.code!==200)throw Error(submitRes.msg||'提交记录读取失败')

                const submittedIds = (submitRes?.data || []).map(s => s.assignmentId)
                console.log('已提交的作业ID:', submittedIds)

                // 3. 分类
                const pending = all.filter(a => !submittedIds.includes(a.id))
                const completed = all.filter(a => submittedIds.includes(a.id))

                console.log('未完成数量:', pending.length)
                console.log('已完成数量:', completed.length)

                this.setData({
                    allAssignments: all,
                    pendingList: pending,
                    completedList: completed,
                    loading: false
                })
            } else {
                this.setData({ loading: false })
            }
        } catch (err) {
            console.error('加载失败:', err)
            this.setData({ loading: false })
        }
    },

    switchTab(e) {
        const tab = e.currentTarget.dataset.tab
        console.log('切换到:', tab)
        this.setData({ activeTab: tab })
    },

    startAssignment(e) {
        const id = e.currentTarget.dataset.id
        const isCompleted = this.data.activeTab === 'completed'
        wx.navigateTo({
            url: `/pages/assignment-detail/assignment-detail?id=${id}&completed=${isCompleted}`
        })
    },

    goBack() {
        wx.navigateBack()
    }
})