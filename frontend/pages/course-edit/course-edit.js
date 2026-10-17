const api = require('../../utils/api.js')
const { BASE_URL } = require('../../utils/config.js')

Page({
    data: {
        mode: 'create',   // create | edit
        courseId: null,
        form: {
            title: '',
            description: '',
            videoUrl: '',
            cover: '',
            duration: 30,
            category: '必修',
            points: 10
        },
        categoryOptions: ['必修', '民族', '法规', '政策'],
        categoryIndex: 0,
        submitting: false,
        coverTempPath: '',    // 本地选中的封面图临时路径
        coverPreview: ''     // 封面图预览（本地路径或已有URL）
    },

    goBack() { wx.navigateBack() },

    onLoad(options) {
        if (options.id) {
            this.setData({ mode: 'edit', courseId: options.id })
            this.loadCourse(options.id)
        }
    },

    async loadCourse(id) {
        try {
            const res = await api.getCourseDetail(id)
            if (res && res.data) {
                const c = res.data
                const catIdx = this.data.categoryOptions.indexOf(c.category)
                this.setData({
                    form: {
                        title: c.title || '',
                        description: c.description || '',
                        videoUrl: c.videoUrl || '',
                        cover: c.cover || '',
                        duration: c.duration || 30,
                        category: c.category || '必修',
                        points: c.points || 10
                    },
                    categoryIndex: catIdx >= 0 ? catIdx : 0,
                    coverPreview: c.cover || ''
                })
            }
        } catch (e) {
            wx.showToast({ title: '加载失败', icon: 'none' })
        }
    },

    onCategoryChange(e) {
        const idx = parseInt(e.detail.value)
        this.setData({
            categoryIndex: idx,
            'form.category': this.data.categoryOptions[idx]
        })
    },

    // 表单各字段输入处理
    onTitleInput(e) {
        this.setData({ 'form.title': e.detail.value })
    },
    onVideoUrlInput(e) {
        this.setData({ 'form.videoUrl': e.detail.value })
    },
    onDurationInput(e) {
        this.setData({ 'form.duration': e.detail.value })
    },
    onPointsInput(e) {
        this.setData({ 'form.points': e.detail.value })
    },
    onDescInput(e) {
        this.setData({ 'form.description': e.detail.value })
    },

    // 选择封面图
    chooseCover() {
        wx.chooseImage({
            count: 1,
            sizeType: ['compressed'],
            sourceType: ['album', 'camera'],
            success: (res) => {
                const tempPath = res.tempFilePaths[0]
                this.setData({
                    coverTempPath: tempPath,
                    coverPreview: tempPath
                })
            }
        })
    },

    // 上传封面图到后端
    uploadCover() {
        return new Promise((resolve, reject) => {
            const tempPath = this.data.coverTempPath
            if (!tempPath) {
                // 没有新选的图片，使用已有的URL
                resolve(this.data.form.cover || '')
                return
            }

            // BASE_URL 格式是 http://localhost:8080/api，需要去掉末尾 /api 得到服务器根地址
            const serverRoot = BASE_URL.replace(/\/api\/?$/, '')
            const uploadUrl = serverRoot + '/api/upload/image'

            console.log('上传封面图:', tempPath, '→', uploadUrl)

            wx.uploadFile({
                url: uploadUrl,
                filePath: tempPath,
                name: 'file',
                timeout: 30000,
                success: (res) => {
                    console.log('上传响应 statusCode:', res.statusCode)
                    console.log('上传响应 data:', res.data)
                    try {
                        const data = JSON.parse(res.data)
                        if (data.code === 200 && data.data) {
                            resolve(data.data)
                        } else {
                            reject(new Error(data.msg || '上传失败: ' + (res.data || '')))
                        }
                    } catch (e) {
                        reject(new Error('服务器返回异常: ' + (res.data || '').substring(0, 100)))
                    }
                },
                fail: (err) => {
                    console.error('上传失败详情:', JSON.stringify(err))
                    reject(new Error(err.errMsg || '网络请求失败'))
                }
            })
        })
    },

    async onSubmit() {
        const f = this.data.form
        if (!f.title.trim()) {
            wx.showToast({ title: '请输入课程标题', icon: 'none' })
            return
        }
        if (!f.videoUrl.trim()) {
            wx.showToast({ title: '请输入视频链接', icon: 'none' })
            return
        }

        this.setData({ submitting: true })

        try {
            // 1. 先上传封面图（如果有新选的本地图片）
            let coverUrl = f.cover || ''
            if (this.data.coverTempPath) {
                try {
                    wx.showLoading({ title: '上传封面图...' })
                    coverUrl = await this.uploadCover()
                    wx.hideLoading()
                } catch (uploadErr) {
                    wx.hideLoading()
                    const errMsg = uploadErr.message || uploadErr.errMsg || '未知错误'
                    wx.showToast({ title: '封面图上传失败: ' + errMsg, icon: 'none', duration: 3000 })
                    console.error('封面图上传错误:', uploadErr)
                    this.setData({ submitting: false })
                    return
                }
            }

            const data = {
                ...f,
                title: f.title.trim(),
                videoUrl: f.videoUrl.trim(),
                cover: coverUrl || '',
                duration: parseInt(f.duration) || 30,
                points: parseInt(f.points) || 10
            }

            // 2. 保存课程
            console.log('提交课程数据:', JSON.stringify(data))
            let courseId = this.data.courseId

            if (this.data.mode === 'edit') {
                const updateRes = await api.updateCourse(courseId, data)
                console.log('更新课程结果:', JSON.stringify(updateRes))
            } else {
                const userInfo = wx.getStorageSync('userInfo')
                data.teacherId = userInfo ? userInfo.id : 1
                const createRes = await api.createCourse(data)
                console.log('创建课程结果:', JSON.stringify(createRes))
                if (createRes && createRes.code === 200 && createRes.data) {
                    courseId = createRes.data.id
                    this.setData({ mode: 'edit', courseId: courseId })
                }
            }

            // 3. 自动提交审核
            if (courseId) {
                try {
                    await api.submitCourseForReview(courseId)
                    wx.showToast({ title: '已保存并提交审核', icon: 'success' })
                } catch (reviewErr) {
                    console.error('提交审核失败:', reviewErr)
                    wx.showToast({ title: '已保存，但提交审核失败', icon: 'none' })
                }
            } else {
                wx.showToast({ title: '已保存', icon: 'success' })
            }

            setTimeout(() => wx.navigateBack(), 1500)
        } catch (e) {
            console.error('保存失败详情:', JSON.stringify(e))
            const msg = (e && e.data && e.data.msg) || (e && e.message) || '未知错误'
            wx.showToast({ title: '保存失败: ' + msg, icon: 'none', duration: 3000 })
        } finally {
            this.setData({ submitting: false })
        }
    },

    async deleteCourse() {
        if (this.data.mode !== 'edit') return
        wx.showModal({
            title: '删除课程',
            content: '删除后不可恢复，确定吗？',
            success: async (res) => {
                if (res.confirm) {
                    try {
                        await api.deleteCourse(this.data.courseId)
                        wx.showToast({ title: '已删除', icon: 'success' })
                        setTimeout(() => wx.navigateBack(), 1500)
                    } catch (e) {
                        wx.showToast({ title: '删除失败', icon: 'none' })
                    }
                }
            }
        })
    }
})
