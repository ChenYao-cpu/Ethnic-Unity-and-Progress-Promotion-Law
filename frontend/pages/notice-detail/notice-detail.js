const api=require('../../utils/api.js');
Page({
    data: {
        score: 0,
        id: '',
        title: '',
        date: '',
        content: ''
    },

    onLoad(options) { this.setData({id:options.id||''});this.getScore();this.loadNotice(); },
    async loadNotice(){try{const res=await api.getNoticeDetail(this.data.id);if(res.code!==200||!res.data)throw Error(res.msg||'公告不存在或已移除');this.setData({title:res.data.title,date:res.data.createTime||res.data.publishTime||'',content:res.data.content})}catch(error){this.setData({'_listStates.noticeDetail.error':error.message||'公告读取失败'})}},
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

    goBack() {
        wx.navigateBack();
    }
});
