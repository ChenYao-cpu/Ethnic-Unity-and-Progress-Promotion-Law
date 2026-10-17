const api = require('../../utils/api.js');

Page({
  data: {
    score: 0,
    notes: []
  },

  onLoad() {
    this.getScore();
  },

  onShow() {
    this.getScore();
    this.loadNotes();
  },

  getScore() {
    const userInfo = wx.getStorageSync('userInfo');

    if (userInfo) {
      this.setData({
        score: userInfo.totalScore || 0
      });
    }
  },

  // 读取笔记
  async loadNotes() {
    const user=wx.getStorageSync('userInfo');if(!user?.id){this.setData({notes:[]});if(!this._redirectingLogin){this._redirectingLogin=true;wx.redirectTo({url:'/pages/login/login'})}return}
    try{const res=await api.getNotes(user.id);if(res.code!==200)throw Error(res.msg||'笔记读取失败');this.setData({notes:res.data||[]})}catch(error){this.setData({'_listStates.notes.error':error.message||'笔记读取失败，请重试。'})}
  },
  // 新建笔记
  createNote() {
    wx.navigateTo({
      url: '/pages/note-edit/note-edit'
    });
  },

  // 查看/编辑笔记
  openNote(e) {
    const id = e.currentTarget.dataset.id;

    wx.navigateTo({
      url: `/pages/note-edit/note-edit?id=${id}`
    });
  },

  // 删除笔记
  deleteNote(e) {
    const id = e.currentTarget.dataset.id;

    wx.showModal({
      title: '提示',
      content: '确定删除这篇笔记吗？',

      success: (res) => {
        if (res.confirm) {

          api.deleteNote(id).then(res=>{if(res.code!==200)throw Error(res.msg||'删除失败');this.loadNotes();wx.showToast({title:'删除成功',icon:'success'})}).catch(error=>wx.showToast({title:error.message||'删除失败',icon:'none'}));
        }
      }
    });
  },

  goBack() {
    wx.reLaunch({
      url: '/pages/index/index'
    });
  }
});