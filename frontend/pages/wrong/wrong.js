const api = require('../../utils/api.js')


Page({
  data: {
    wrongList: [],
    loading: false,
    score: 0,

    // 答题中
    inPractice: false,
    currentQuestion: null,
    currentOptions: [],
    currentIndex: 0,
    totalCount: 0,
    optionLabels: ['A', 'B', 'C', 'D', 'E'],
    showAnswer: false,
    isCurrentCorrect: false,

    // 练习完成
    done: false,
    doneCorrect: 0,
    doneWrong: 0
  },

  onLoad() {
    this.loadUserInfo()
  },

  onShow() {
    this.loadUserInfo()
    this.loadWrongList()
  },

  loadUserInfo() {
    const userInfo = wx.getStorageSync('userInfo')
    if (userInfo) {
      this.setData({ score: userInfo.totalScore || 0 })
    }
  },

  goBack() {
    if (this.data.inPractice) {
      // 返回错题列表
      this.setData({ inPractice: false, done: false })
      this.loadWrongList()
    } else {
      wx.navigateBack({ delta: 1 })
    }
  },

  // ========== 加载错题列表 ==========
  async loadWrongList(){
    const user=wx.getStorageSync('userInfo');if(!user?.id){this.setData({loading:false,wrongList:[]});if(!this._redirectingLogin){this._redirectingLogin=true;wx.redirectTo({url:'/pages/login/login'})}return}this.setData({loading:true});
    try{const res=await api.getWrongQuestions(user.id);if(res.code!==200)throw Error(res.msg||'错题读取失败');const records=res.data||[];if(!records.length){this.setData({wrongList:[]});return}const bank=await api.getQuestions({limit:10000});if(bank.code!==200)throw Error(bank.msg||'题目读取失败');const questions=new Map((bank.data||[]).map(q=>[String(q.id),q]));const wrongList=records.map(w=>{const q=questions.get(String(w.questionId));if(!q)throw Error('部分题目暂时无法读取，请稍后重试');let options=q.options;if(typeof options==='string'){try{options=JSON.parse(options)}catch{throw Error('题目选项格式异常')}}return {...w,id:q.id,wrongId:w.id,questionId:q.id,question:q.title,title:q.title,type:q.type===2?'multi':q.type===3?'judge':'single',options:options||[],answer:q.answer,analysis:q.analysis}});this.setData({wrongList})}catch(error){this.setData({'_listStates.wrong.error':error.message||'错题读取失败'})}finally{this.setData({loading:false})}
  },
  async removeWrong(questionId){const item=this.data.wrongList.find(w=>String(w.questionId)===String(questionId));if(!item)return;const res=await api.removeWrongRecord(item.wrongId);if(res.code!==200)throw Error(res.msg||'错题移除失败');this.setData({wrongList:this.data.wrongList.filter(w=>w!==item)})},
  async addScore(){const user=wx.getStorageSync('userInfo');if(!user?.id)return;try{const res=await api.getUserInfo(user.id);if(res.code===200&&res.data){const fresh={...user,totalScore:res.data.totalScore};wx.setStorageSync('userInfo',fresh);this.setData({score:fresh.totalScore})}}catch(error){}},
  // ========== 开始逐题练习 ==========
  startPractice() {
    const list = this.data.wrongList
    if (!list.length) {
      wx.showToast({ title: '暂无错题', icon: 'none' })
      return
    }

    // 打乱顺序
    const shuffled = [...list].sort(() => Math.random() - 0.5)

    const first = shuffled[0]
    const q = this.buildQuestion(first)
    this.setData({
      inPractice: true,
      currentQuestion: first,
      currentOptions: this.buildOptions(q),
      currentIndex: 0,
      totalCount: shuffled.length,
      showAnswer: false,
      isCurrentCorrect: false,
      done: false,
      doneCorrect: 0,
      doneWrong: 0
    })
  },

  // 从错题数据构建题目显示对象（缺失字段从题库补全）
  buildQuestion(wrong) {
    const qid = wrong.questionId || wrong.id
    return {id:qid,question:wrong.question||wrong.title||'',type:wrong.type,answer:wrong.answer,options:wrong.options||[],analysis:wrong.analysis||''}
  },

  buildOptions(q) {
    const opts = q.options || []
    return opts.map((opt, i) => ({
      label: this.data.optionLabels[i],
      text: opt,
      isActive: false,
      isCorrectAnswer: q.answer ? q.answer.indexOf(this.data.optionLabels[i]) > -1 : false
    }))
  },

  // ========== 点击选项 ==========
  onOptionTap(e) {
    if (this.data.showAnswer || this._checkingAnswer) return

    const label = e.currentTarget.dataset.label
    if (!label) return

    const q = this.data.currentQuestion
    const isMulti = (q.type === 'multi')

    // 更新选中状态
    const opts = [...this.data.currentOptions]
    if (isMulti) {
      const opt = opts.find(o => o.label === label)
      if (opt) opt.isActive = !opt.isActive
    } else {
      opts.forEach(o => { o.isActive = (o.label === label) })
    }

    this.setData({ currentOptions: opts }, () => {
      if(!isMulti)this.checkAnswer()
    })
  },

  // ========== 判断对错 ==========
  async checkAnswer() {
    if(this._checkingAnswer||this.data.showAnswer)return;this._checkingAnswer=true;try{
    const q = this.data.currentQuestion
    const opts = this.data.currentOptions
    const userSelected = opts.filter(o => o.isActive).map(o => o.label).sort().join('')
    if(!userSelected)throw Error('请先选择答案');
    const correctAns = (q.answer || '').toUpperCase().replace(/\s/g, '')

    const user=wx.getStorageSync('userInfo');if(!user?.id)throw Error('请先登录');const result=await api.checkAnswer(user.id,q.questionId||q.id,userSelected);if(result.code!==200)throw Error(result.msg||'答案提交失败');const isCorrect=!!result.data.correct;

    // 高亮正确选项
    const updatedOpts = opts.map(o => {
      o.isCorrectAnswer = correctAns.indexOf(o.label) > -1 ||
        ((q.type === 'judge' || q.type === 'judgment') && correctAns === '正确' && o.label === 'A') ||
        ((q.type === 'judge' || q.type === 'judgment') && correctAns === '错误' && o.label === 'B')
      return o
    })

    this.setData({
      showAnswer: true,
      isCurrentCorrect: isCorrect,
      currentOptions: updatedOpts
    })

    if (isCorrect) {
      // 正确：立即从错题本删除 + 加1积分
      await this.removeWrong(q.id)
      await this.addScore()
    }
    }catch(error){wx.showToast({title:error.message||'答案提交失败，请重试',icon:'none'})}finally{this._checkingAnswer=false}
  },

  // ========== 下一题 ==========
  nextQuestion() {
    const userInfo = wx.getStorageSync('userInfo')
    const remaining = this.data.wrongList

    if (this.data.isCurrentCorrect) {
      // 答对了：从当前session的错题中也移除
      const qid = this.data.currentQuestion.id
      const correctCount = this.data.doneCorrect + 1
      const filtered = remaining.filter(w => (w.questionId || w.id) != qid)

      if (filtered.length === 0) {
        // 全部做完
        const total = this.data.totalCount
        this.setData({
          inPractice: false,
          done: true,
          doneCorrect: correctCount,
          doneWrong: this.data.doneWrong,
          wrongList: []
        })
        return
      }

      // 还有剩余，随机取下一道
      const next = filtered[Math.floor(Math.random() * filtered.length)]
      const q = this.buildQuestion(next)
      this.setData({
        currentQuestion: next,
        currentOptions: this.buildOptions(q),
        currentIndex: this.data.currentIndex + 1,
        totalCount: filtered.length,
        showAnswer: false,
        isCurrentCorrect: false,
        doneCorrect: correctCount
      })
    } else {
      // 答错了：保留，从剩余中取下一道
      const wrongCount = this.data.doneWrong + 1

      if (remaining.length === 0) {
        const total = this.data.totalCount
        this.setData({
          inPractice: false,
          done: true,
          doneCorrect: this.data.doneCorrect,
          doneWrong: wrongCount,
          wrongList: []
        })
        return
      }

      const next = remaining[Math.floor(Math.random() * remaining.length)]
      const q = this.buildQuestion(next)
      this.setData({
        currentQuestion: next,
        currentOptions: this.buildOptions(q),
        currentIndex: this.data.currentIndex + 1,
        totalCount: remaining.length,
        showAnswer: false,
        isCurrentCorrect: false,
        doneWrong: wrongCount
      })
    }
  },

  // ========== 重新开始 ==========
  restartPractice() {
    this.setData({ done: false, inPractice: false })
    this.loadWrongList()
  },

  backToHome() {
    wx.navigateBack({ delta: 1 })
  }
})
