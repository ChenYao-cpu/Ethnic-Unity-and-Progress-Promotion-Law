const api = require('../../utils/api.js')

Page({
    data: {
        userInfo: null,
        assignments: [],
        selectedAssignmentId: null,
        selectedIndex: 0,
        progress: null,
        loading: false,
        // 催促状态追踪
        remindedStudents: {},
        showRemindPanel: false,
        remindTargetId: null,
        remindTargetName: '',
        remindAll: false,
        remindMessage: ''
    },

    onLoad() {
        const userInfo = wx.getStorageSync('userInfo')
        if (!userInfo || (userInfo.role !== 'teacher' && userInfo.role !== 'admin')) {
            wx.redirectTo({ url: '/pages/login/login' })
            return
        }
        this.setData({ userInfo })
        this.loadAssignments()
    },

    onShow() {
        if (this.data.selectedAssignmentId) {
            this.loadProgress(this.data.selectedAssignmentId)
        }
    },

    async loadAssignments() {
        wx.showLoading({ title: '加载中...' })
        try {
            const res = await api.getTeacherAssignments({ teacherId: this.data.userInfo.id })
            if (res.code === 200) {
                const published = (res.data || []).filter(a => a.status === 1)
                this.setData({ assignments: published })
                if (published.length > 0) {
                    const firstId = published[0].id
                    this.setData({ selectedAssignmentId: firstId, selectedIndex: 0 })
                    this.loadProgress(firstId)
                }
            }
        } catch (err) {
            console.error('加载作业列表失败', err)
            wx.showToast({ title: '网络错误，请确认后端已启动', icon: 'none' })
        } finally {
            wx.hideLoading()
        }
    },

    async loadProgress(assignmentId) {
        if (this.data.loading) return
        this.setData({ loading: true })
        wx.showLoading({ title: '加载进度...' })

        try {
            const res = await api.getAssignmentProgress(assignmentId)
            if (res.code === 200 && res.data) {
                const progress = res.data
                const completed = (progress.studentDetails || []).filter(s => s.status === 'completed')
                const uncompleted = (progress.studentDetails || []).filter(s => s.status === 'uncompleted')
                completed.sort((a, b) => (b.score || 0) - (a.score || 0))

                this.setData({
                    progress: {
                        ...progress,
                        completedList: completed,
                        uncompletedList: uncompleted
                    },
                    remindedStudents: {}
                })
            }
        } catch (err) {
            console.error('加载学习进度失败', err)
            wx.showToast({ title: '网络错误，请确认后端已启动', icon: 'none' })
        } finally {
            this.setData({ loading: false })
            wx.hideLoading()
        }
    },

    onAssignmentChange(e) {
        const index = parseInt(e.detail.value)
        if (this.data.assignments[index]) {
            this.setData({
                selectedAssignmentId: this.data.assignments[index].id,
                selectedIndex: index,
                remindedStudents: {}
            })
            this.loadProgress(this.data.assignments[index].id)
        }
    },

    // 催促单个党员 - 直接发送，不需要弹窗
    remindStudent(e) {
        const studentId = e.currentTarget.dataset.sid
        const studentName = e.currentTarget.dataset.name || '党员'

        // 检查是否已催促
        if (this.data.remindedStudents[studentId]) {
            wx.showToast({ title: '已催促过该党员', icon: 'none' })
            return
        }

        const adminName = (this.data.userInfo && this.data.userInfo.nickname) || '管理员'
        const message = adminName + '管理员提醒：' + studentName + '同志，请尽快完成测验！'
        wx.showLoading({ title: '发送中...' })

        api.sendReminder({
            assignmentId: this.data.selectedAssignmentId,
            fromUserId: this.data.userInfo.id,
            toUserId: studentId,
            message: message
        }).then(() => {
            wx.hideLoading()
            // 标记为已催促
            const remindedStudents = { ...this.data.remindedStudents }
            remindedStudents[studentId] = true
            this.setData({ remindedStudents })
            wx.showToast({ title: '已催促', icon: 'success' })
        }).catch(() => {
            wx.hideLoading()
            wx.showToast({ title: '催促失败，请重试', icon: 'none' })
        })
    },

    // 催促全部未完成
    remindAllUncompleted() {
        const uncompleted = this.data.progress ? this.data.progress.uncompletedList : []
        const unreminded = uncompleted.filter(s => !this.data.remindedStudents[s.studentId])

        if (unreminded.length === 0) {
            wx.showToast({ title: '所有未完成党员均已催促过', icon: 'none' })
            return
        }

        wx.showModal({
            title: '一键催促',
            content: `将催促${unreminded.length}名未完成党员，确定吗？`,
            success: (res) => {
                if (res.confirm) {
                    this.batchRemind(unreminded)
                }
            }
        })
    },

    async batchRemind(students) {
        wx.showLoading({ title: '发送中...' })
        const adminName = (this.data.userInfo && this.data.userInfo.nickname) || '管理员'
        const message = adminName + '管理员提醒：请尽快完成测验！'

        try {
            await api.sendBatchReminder({
                assignmentId: this.data.selectedAssignmentId,
                fromUserId: this.data.userInfo.id,
                toUserIds: students.map(s => s.studentId),
                message: message
            })
            // 全部标记为已催促
            const remindedStudents = { ...this.data.remindedStudents }
            students.forEach(s => { remindedStudents[s.studentId] = true })
            this.setData({ remindedStudents })
            wx.hideLoading()
            wx.showToast({ title: `已催促${students.length}名党员`, icon: 'success' })
        } catch (err) {
            console.error('批量发送失败', err)
            wx.hideLoading()
            wx.showToast({ title: '发送失败，请重试', icon: 'none' })
        }
    },

    goBack() {
        wx.navigateBack()
    }
})
