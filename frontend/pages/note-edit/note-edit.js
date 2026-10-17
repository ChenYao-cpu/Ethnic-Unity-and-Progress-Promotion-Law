const api = require('../../utils/api.js');

Page({

  data: {
    score: 0,
    noteId: null,
    noteTitle: '',
    noteContent: '',
    images: [],
    selectedFont: '微软雅黑',
    selectedFontFamily: '"Microsoft YaHei", sans-serif',
    selectedColor: '#000000',
    showToolbar: false,
    fontList: [
      { name: '宋体', fontFamily: '"SimSun", serif' },
      { name: '黑体', fontFamily: '"SimHei", sans-serif' },
      { name: '楷体', fontFamily: '"KaiTi", serif' },
      { name: '仿宋', fontFamily: '"FangSong", serif' },
      { name: '微软雅黑', fontFamily: '"Microsoft YaHei", sans-serif' }
    ],
    colorList: [
      '#000000',
      '#FF0000',
      '#0000FF',
      '#00AA00',
      '#FF8800',
      '#800080'
    ]
  },

  onLoad(options) {
    this.getScore();

    if(options.id){this.setData({noteId:options.id});this.loadNote();}
  },
  async loadNote(){const user=wx.getStorageSync('userInfo');if(!user?.id)return;try{const res=await api.getNotes(user.id);if(res.code!==200)throw Error(res.msg);const note=(res.data||[]).find(n=>String(n.id)===String(this.data.noteId));if(!note)throw Error('这篇笔记不存在或已删除');this.setData({noteTitle:note.title,noteContent:note.content})}catch(error){this.setData({'_listStates.notes.error':error.message||'笔记读取失败'})}},
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

  // 标题输入
  onTitleInput(e) {
    this.setData({
      noteTitle: e.detail.value
    });
  },

  // 内容输入
  onContentInput(e) {
    this.setData({
      noteContent: e.detail.value
    });
  },

  // 返回
  goBack() {
    wx.navigateBack({
      delta: 1
    });
  },

  // 工具栏
  toggleToolbar() {
    this.setData({
      showToolbar: !this.data.showToolbar
    });
  },
  // 显示字体选择器
  showFontPicker() {
    this.setData({
      showFontModal: true
    });
  },
  // 关闭字体选择器
  closeFontModal() {
    this.setData({
      showFontModal: false
    });
  },

  // 显示颜色选择器
  showColorPicker() {
    this.setData({
      showColorModal: true
    });
  },

  // 关闭颜色选择器
  closeColorModal() {
    this.setData({
      showColorModal: false
    });
  },
  stopPropagation() {
    // 空方法，用于阻止弹窗关闭
  },
  selectFont(e) {
    const fontName = e.currentTarget.dataset.font;
    const fontItem = this.data.fontList.find(item => item.name === fontName);

    if (fontItem) {
      this.setData({
        selectedFont: fontItem.name,
        selectedFontFamily: fontItem.fontFamily
      });
      wx.showToast({
        title: '字体已切换',
        icon: 'success'
      });
    }
  },


  // 选择颜色
  selectColor(e) {
    const color = e.currentTarget.dataset.color;
    this.setData({
      selectedColor: color
    });
    wx.showToast({
      title: '颜色已切换',
      icon: 'success'
    });
  },

  // 添加图片
  addImage() {
    wx.chooseMedia({
      count: 9,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const tempFiles = res.tempFiles.map(item => item.tempFilePath);
        this.setData({
          images: this.data.images.concat(tempFiles)
        });
      }
    });
  },

  // 删除图片
  deleteImage(e) {
    const index = e.currentTarget.dataset.index;
    let images = this.data.images;
    images.splice(index, 1);
    this.setData({
      images
    });
  },

  // 保存笔记
  async saveNote(){
    if(this.data.saving)return;const user=wx.getStorageSync('userInfo'),title=this.data.noteTitle.trim(),content=this.data.noteContent.trim();
    if(!user?.id){wx.showToast({title:'请先登录',icon:'none'});return}if(!title||!content){wx.showToast({title:'请填写标题和内容',icon:'none'});return}
    this.setData({saving:true});try{const data={userId:user.id,title,content};const res=await (this.data.noteId?api.updateNote({...data,id:this.data.noteId}):api.createNote(data));if(res.code!==200)throw Error(res.msg||'保存失败');wx.showToast({title:'已保存到学习笔记',icon:'success'});wx.navigateBack()}catch(error){wx.showToast({title:error.message||'保存失败',icon:'none'})}finally{this.setData({saving:false})}
  }
});