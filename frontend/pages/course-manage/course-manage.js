const api = require('../../utils/api.js')
const { BASE_URL } = require('../../utils/config.js')

// 将相对路径（如 /uploads/images/...）转为完整URL
function fixCoverUrl(cover) {
    if (!cover) return ''
    if (cover.startsWith('http://') || cover.startsWith('https://')) return cover
    // 去掉 BASE_URL 末尾的 /api
    const serverRoot = BASE_URL.replace(/\/api\/?$/, '')
    return serverRoot + cover
}

Page({
    data: {
        courses: [],
        loading: true,
        statusMap: {
            'draft': '草稿',
            'pending_review': '审核中',
            'approved': '已通过',
            'rejected': '已驳回',
            'published': '已发布'
        }
    },

    onShow() { this.loadData() },

    goBack() { wx.navigateBack() },

    async loadData() {
        this.setData({ loading: true })
        try {
            const userInfo = wx.getStorageSync('userInfo')
            const res = await api.getTeacherCourses(userInfo ? userInfo.id : 1)
            if (res.code === 200) {
                const courses = (res.data || []).map(c => {
                    const fixedCover = fixCoverUrl(c.cover)
                    console.log('课程封面:', c.cover, '→', fixedCover)
                    return { ...c, cover: fixedCover }
                })
                console.log('最终课程列表:', JSON.stringify(courses.map(c => ({ id: c.id, title: c.title, cover: c.cover }))))
                this.setData({ courses })
            } else {
                wx.showToast({ title: res.msg || '加载失败', icon: 'none' })
                this.setData({ courses: [] })
            }
        } catch (e) {
            wx.showToast({ title: '加载失败', icon: 'none' })
        } finally {
            this.setData({ loading: false })
        }
    },

    // 封面图加载失败时显示占位
    onCoverError(e) {
        const index = e.currentTarget.dataset.index
        this.setData({ [`courses[${index}]._coverFailed`]: true })
    },

    goCreate() {
        wx.navigateTo({ url: '/pages/course-edit/course-edit' })
    },

    goEdit(e) {
        const id = e.currentTarget.dataset.id
        wx.navigateTo({ url: '/pages/course-edit/course-edit?id=' + id })
    },

    async submitForReview(e) {
        const id = e.currentTarget.dataset.id
        wx.showModal({
            title: '提交审核',
            content: '提交后法务人员将审核此课程，确定吗？',
            success: async (res) => {
                if (res.confirm) {
                    try {
                        await api.submitCourseForReview(id)
                        wx.showToast({ title: '已提交审核', icon: 'success' })
                        this.loadData()
                    } catch (e) {
                        wx.showToast({ title: '提交失败', icon: 'none' })
                    }
                }
            }
        })
    },

    async unpublishCourse(e) {
        const id = e.currentTarget.dataset.id
        wx.showModal({
            title: '下架视频',
            content: '下架后学生端将不可见此课程，确定吗？',
            success: async (res) => {
                if (res.confirm) {
                    try {
                        await api.unpublishCourse(id)
                        wx.showToast({ title: '已下架', icon: 'success' })
                        this.loadData()
                    } catch (e) {
                        wx.showToast({ title: '下架失败', icon: 'none' })
                    }
                }
            }
        })
    },

    async deleteCourse(e) {
        const id = e.currentTarget.dataset.id
        wx.showModal({
            title: '删除课程',
            content: '删除后不可恢复，确定吗？',
            success: async (res) => {
                if (res.confirm) {
                    try {
                        await api.deleteCourse(id)
                        wx.showToast({ title: '已删除', icon: 'success' })
                        this.loadData()
                    } catch (e) {
                        wx.showToast({ title: '删除失败', icon: 'none' })
                    }
                }
            }
        })
    }
})
