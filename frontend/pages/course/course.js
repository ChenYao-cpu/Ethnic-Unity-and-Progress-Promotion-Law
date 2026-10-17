const {request}=require('../../utils/request.js');
const api = require('../../utils/api.js');
const { BASE_URL } = require('../../utils/config.js');

function fixCoverUrl(cover) {
    if (!cover) return ''
    if (cover.startsWith('http://') || cover.startsWith('https://')) return cover
    const serverRoot = BASE_URL.replace(/\/api\/?$/, '')
    return serverRoot + cover
}

Page({
    data: {
        categories: [
            { id: -1, name: '全部' },
            { id: -2, name: '最新视频' },
            { id: 0, name: '序言' },
            { id: 1, name: '第一章' },
            { id: 2, name: '第二章' },
            { id: 3, name: '第三章' },
            { id: 4, name: '第四章' },
            { id: 5, name: '第五章' },
            { id: 6, name: '第六章' },
            { id: 7, name: '第七章' }
        ],
        currentCategory: -1,
        courses: [],
        latestCourses: [],
        latestEmpty: false,
        filteredCourses: [],
        currentCourse: null,
        score: 0,
        canDrag: false,
        isFirstWatch: true,
        videoContext: null,
        videoDuration: 0,
        saveTimer: null,
        currentProgress: 0
    },

    onLoad() {
        this.getScore();
        this.loadCourses();
    },

    onShow() {
        this.getScore();
    },

    getScore() {
        const userInfo = wx.getStorageSync('userInfo');
        if (userInfo) {
            this.setData({
                score: userInfo.totalScore || 0
            });
        }
    },

    async loadCourses(){
      try{const res=await api.getCourses();if(res.code!==200)throw Error(res.msg||'课程读取失败');const courses=(res.data||[]).map(c=>({...c,cover:fixCoverUrl(c.cover),chapter:this.courseChapter(c),duration:this.formatDuration(c.duration)}));courses.sort((a,b)=>Number(b.id)-Number(a.id));this.setData({courses,latestCourses:courses,latestEmpty:!courses.length});this.filterCourses();}
      catch(error){this.setData({'_listStates.courses.error':error.message||'课程读取失败，请重试。'})}
    },
    onCourseCoverLoad(e){const id=e.currentTarget.dataset.id;this.setData({filteredCourses:this.data.filteredCourses.map(c=>String(c.id)===String(id)?{...c,_coverReady:true}:c)})},
    onCourseCoverError(e){const id=e.currentTarget.dataset.id;this.setData({filteredCourses:this.data.filteredCourses.map(c=>String(c.id)===String(id)?{...c,_coverError:true}:c)})},
    courseChapter(c){const names=['序言','第一章','第二章','第三章','第四章','第五章','第六章','第七章'];const category=String(c.category??'');const index=names.findIndex(n=>category.includes(n)||String(c.title||'').startsWith(n));return index<0?null:index},
    selectCategory(e){this.setData({currentCategory:Number(e.currentTarget.dataset.id)});this.filterCourses()},
    filterCourses(){const category=this.data.currentCategory;this.setData({filteredCourses:category<0?this.data.courses:this.data.courses.filter(c=>c.chapter===category)})},
    formatDuration(seconds){if(seconds===null||seconds===undefined)return '';const m=Math.floor(seconds/60),r=Math.floor(seconds%60);return String(m).padStart(2,'0')+':'+String(r).padStart(2,'0')},
    async playCourse(e) {
        const course = e.currentTarget.dataset.course;
        const user=wx.getStorageSync('userInfo');if(!user?.id){wx.showToast({title:'请先登录后学习',icon:'none'});return}
        let record;try{const res=await request('/study/course/'+course.id+'?userId='+user.id);if(res.code!==200)throw Error(res.msg);record=res.data}catch(error){wx.showToast({title:'学习进度读取失败，请重试',icon:'none'});return}
        this._progressStates=this._progressStates||{};this._progressStates[course.id]={progress:Number(record?.progress||0),reportedTime:0};
        const savedProgress={progress:Number(record?.progress||0),watched:Number(record?.progress||0)>=100,currentTime:0};

        this.setData({
            currentCourse: {
                ...course,
                progress: savedProgress.watched ? 0 : (savedProgress.progress || 0),
                watched: savedProgress.watched || false
            },
            canDrag: savedProgress.watched || false,
            isFirstWatch: !savedProgress.watched,
            currentProgress: savedProgress.watched ? 0 : (savedProgress.progress || 0)
        });

        // 立即创建 videoContext，确保 videoPlay 回调时可用
        this.videoContext = wx.createVideoContext('courseVideo', this);
        // 已看完的从头开始，未看完的从断点续播
        this._pendingSeekTime = savedProgress.watched ? 0 : (savedProgress.currentTime || 0);
    },

    // 返回上一级
    goBack() {
        if (this.videoContext) {
            this.videoContext.pause();
        }

        if (this.saveTimer) {
            clearTimeout(this.saveTimer);
        }

        if (this.data.currentCourse) {
            this.setData({ currentCourse: null });
        } else {
            wx.reLaunch({
                url: '/pages/index/index'
            });
        }
    },

    // ✅ 新增：点击退出/返回首页的方法
    goHome() {
        if (this.videoContext) {
            this.videoContext.pause();
        }
        if (this.saveTimer) {
            clearTimeout(this.saveTimer);
        }
        // 直接返回首页
        wx.reLaunch({
            url: '/pages/index/index'
        });
    },

    videoPlay() {
        // 恢复到上次播放位置
        const seekTime = this._pendingSeekTime || 0;
        if (seekTime > 0 && this.videoContext) {
            // 延迟一小段时间确保视频已加载，再跳转到断点位置
            setTimeout(() => {
                if (this.videoContext) {
                    this.videoContext.seek(seekTime);
                }
            }, 300);
        }
        this._pendingSeekTime = 0;
    },

    videoPause() {
        if (this.data.currentCourse) {
            this.saveProgress(
                this.data.currentCourse.id,
                this.data.currentProgress / 100 * this.videoDuration,
                this.videoDuration,
                this.data.currentCourse.watched
            );
        }
    },

    videoTimeUpdate(e) {
        const currentTime = e.detail.currentTime;
        const duration = e.detail.duration;

        if (duration > 0) {
            this.videoDuration = duration;
            const progress = Math.floor((currentTime / duration) * 100);
            this.setData({ currentProgress: progress });

            if (duration > 0 && currentTime / duration > 0.5) {
                this.setData({ canDrag: true });
            }

            if (this.saveTimer) {
                clearTimeout(this.saveTimer);
            }

            this.saveTimer = setTimeout(() => {
                this.saveProgress(
                    this.data.currentCourse.id,
                    currentTime,
                    duration,
                    this.data.currentCourse.watched
                );
            }, 1000);
        }
    },

    async videoEndFinish(){if(!this.data.currentCourse)return;const ok=await this.saveProgress(this.data.currentCourse.id,this.videoDuration,this.videoDuration,true);if(ok){this.setData({'currentCourse.watched':true,'currentCourse.progress':100,currentProgress:100,isFirstWatch:false});wx.showToast({title:'学习进度已保存',icon:'success'})}},
    videoError(e) {
        console.error('视频加载失败:', e.detail.errMsg);
        wx.showToast({
            title: '视频加载失败',
            icon: 'none'
        });
    },

    saveProgress(...args){this._progressQueue=(this._progressQueue||Promise.resolve()).then(()=>this.persistProgress(...args));return this._progressQueue;},
    async persistProgress(courseId,currentTime,duration,watched){
      const user=wx.getStorageSync('userInfo');if(!user?.id||!duration)return false;
      const state=(this._progressStates||{})[courseId];if(!state)return false;if(state.progress>=100)return true;
      const progress=Math.min(100,Math.floor(currentTime/duration*100));const elapsed=Math.max(0,Math.floor((currentTime-(state.reportedTime||0))/60));
      if(progress===state.progress&&!elapsed)return true;
      try{const res=await api.updateStudyProgress({userId:user.id,courseId,duration:elapsed,progress});if(res.code!==200)throw Error(res.msg||'保存失败');state.progress=progress;state.reportedTime=(state.reportedTime||0)+elapsed*60;return true}catch(error){wx.showToast({title:'学习进度未保存，请稍后重试',icon:'none'});return false}
    },
    onFullScreenChange(e) {
        this.setData({ isFullScreen: e.detail.fullScreen })
    },

    onUnload() {
        if (this.videoContext) {
            this.videoContext.pause();
        }

        if (this.saveTimer) {
            clearTimeout(this.saveTimer);
        }
    }
});