const app = getApp();
const questionsBank = require('../../utils/questions.js');
const api = require('../../utils/api.js');

// 本地缓存按账号隔离，避免同一台设备切换账号后互相串数据
const storageKey = (name) => {
  const u = wx.getStorageSync('userInfo') || {};
  return name + '_' + (u.id || 'guest');
};

Page({
  openTutor(e) { const d=e.currentTarget.dataset; wx.navigateTo({url:'/pages/ai-tutor/ai-tutor?id='+d.id+'&answer='+encodeURIComponent(d.answer||'')}) },

  data: {
    currentType: null,
    questions: [],
    currentOptions: [],
    currentIndex: 0,
    answers: {},
    timeLeft: 0,
    timer: null,
    examResult: null,
    optionLabels: ['A', 'B', 'C', 'D', 'E'],
    assignments: [],
    completedChapters: [],
    currentChapter: null,
    score: 0
  },

  onLoad() {
    const userInfo = wx.getStorageSync('userInfo');
    if (!userInfo) {
      wx.redirectTo({ url: '/pages/login/login' });
      return;
    }
    this.getScore();
    this.loadAssignments();
    this.loadCompletedChapters();
  },

  onShow() {
    this.getScore();
    this.loadAssignments();
    this.loadCompletedChapters();
  },

  getScore() {
    const userInfo = wx.getStorageSync('userInfo');
    if (userInfo) {
      this.setData({
        score: userInfo.totalScore || 0
      });
    }
  },

  async loadAssignments() {
    try {
      const userInfo = wx.getStorageSync('userInfo');
      if (!userInfo || !userInfo.id) return;
      const res = await api.getStudentAssignments({ studentId: userInfo.id, status: 1 });
      if (res.code === 200 && res.data) this.setData({ assignments: res.data });
    } catch (err) { console.error('加载作业失败:', err); }
  },

  async loadCompletedChapters(callback) {
    const raw = wx.getStorageSync(storageKey('completedChapters'));
    const arr = Array.isArray(raw) ? raw.map(n => Number(n)) : [];
    this.setData({ completedChapters: arr }, () => {
      if (callback) callback();
    });
  },

  selectType(e) {
    const type = e.currentTarget.dataset.type;
    const userInfo = wx.getStorageSync('userInfo');
    const isTeacher = userInfo && userInfo.role === 'teacher';

    if (type === '综合模拟') {
      wx.navigateTo({ url: isTeacher ? '/pages/exam-config/exam-config' : '/pages/student-exam-config/student-exam-config' });
      return;
    }

    this.setData({
      currentType: type,
      examResult: null,
      questions: [],
      currentOptions: [],
      answers: {}
    });

    if (type === '专项训练') {
      // 显示单选题和多选题选择按钮
    } else if (type === '错题重考') {
      this.startWrongExam();
    }
  },

  goToMyQuiz() {
    const userInfo = wx.getStorageSync('userInfo');
    if (userInfo && userInfo.role === 'teacher') {
      wx.navigateTo({ url: '/pages/teacher-home/teacher-home' });
    } else {
      wx.navigateTo({ url: '/pages/student-exam-config/student-exam-config' });
    }
  },

  startSpecialTrain(e) {
    wx.showLoading({ title: '加载中' });

    const type = e.currentTarget.dataset.type;
    console.log('专项训练类型:', type);
    console.log('题库总数:', questionsBank.length);

    let specialQuestions = [];
    if (type === 'single') {
      specialQuestions = questionsBank.filter(q => q.type === 'single');
      console.log('单选题数量:', specialQuestions.length);
    } else if (type === 'multi') {
      specialQuestions = questionsBank.filter(q => q.type === 'multi');
      console.log('多选题数量:', specialQuestions.length);
    }

    if (specialQuestions.length === 0) {
      wx.hideLoading();
      wx.showToast({ title: '暂无题目', icon: 'none' });
      return;
    }

    // 随机抽取10道题
    const selected = this.shuffleArray([...specialQuestions]).slice(0, 10);
    this.initExam(selected);
    wx.hideLoading();
  },

  startWrongExam() {
    wx.showLoading({ title: '加载中' });
    const wrongList = wx.getStorageSync(storageKey('wrongQuestions')) || [];
    if (wrongList.length === 0) { wx.hideLoading(); wx.showToast({ title: '暂无错题', icon: 'none' }); return; }
    this.initExam(wrongList);
    wx.hideLoading();
  },

  startChapterExam(e) {
    const chapter = Number(e.currentTarget.dataset.chapter);
    wx.showLoading({ title: `加载第${chapter}章` });
    const chapterQuestions = questionsBank.filter(q => q.chapter === chapter);
    const judgments = chapterQuestions.filter(q => q.type === 'judge' || q.type === 'judgment');
    const singles = chapterQuestions.filter(q => q.type === 'single');
    const multis = chapterQuestions.filter(q => q.type === 'multi');
    let finalQuestions = [...this.shuffleArray(judgments).slice(0, 2), ...this.shuffleArray(singles).slice(0, 4), ...this.shuffleArray(multis).slice(0, 2)];
    if (finalQuestions.length === 0) { wx.hideLoading(); wx.navigateBack(); return; }
    finalQuestions = this.shuffleArray(finalQuestions);
    this.initExam(finalQuestions, chapter);
    wx.hideLoading();
  },

  initExam(questionList, chapter = null) {
    if (this.data.timer) clearInterval(this.data.timer);
    this.setData({
      questions: questionList,
      answers: {},
      currentOptions: [],
      currentIndex: 0,
      timeLeft: questionList.length * 60,
      examResult: null,
      currentChapter: chapter
    });
    this.refreshCurrentQuestionView();
    this.startTimer();
  },

  shuffleArray(arr) {
    for (let i = arr.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [arr[i], arr[j]] = [arr[j], arr[i]];
    }
    return arr;
  },

  startTimer() {
    this.data.timer = setInterval(() => {
      if (this.data.timeLeft <= 1) { clearInterval(this.data.timer); this.submitExam(); }
      else this.setData({ timeLeft: this.data.timeLeft - 1 });
    }, 1000);
  },

  formatTime(seconds) {
    const min = Math.floor(seconds / 60);
    const sec = seconds % 60;
    return `${String(min).padStart(2, '0')}:${String(sec).padStart(2, '0')}`;
  },

  refreshCurrentQuestionView() {
    const q = this.data.questions[this.data.currentIndex];
    if (!q) return;
    const currentAns = this.data.answers[q.id] || '';
    const opts = q.options.map((opt, i) => {
      const label = this.data.optionLabels[i];
      return {
        label: label,
        text: opt,
        isActive: currentAns.indexOf(label) > -1
      };
    });
    this.setData({ currentOptions: opts });
  },

  onOptionTap(e) {
    const label = e.currentTarget.dataset.label;
    if (!label) return;
    const q = this.data.questions[this.data.currentIndex];
    const isMulti = q.type === 'multi';
    let current = (this.data.answers[q.id] || '').split('').filter(Boolean);

    if (isMulti) {
      const idx = current.indexOf(label);
      if (idx > -1) current.splice(idx, 1);
      else current.push(label);
      current.sort();
    } else {
      current = [label];
    }

    this.setData({ [`answers.${q.id}`]: current.join('') }, () => {
      this.refreshCurrentQuestionView();
    });
  },

  prevQuestion() {
    if (this.data.currentIndex > 0) {
      this.setData({ currentIndex: this.data.currentIndex - 1 }, () => { this.refreshCurrentQuestionView(); });
    }
  },

  nextQuestion() {
    if (this.data.currentIndex < this.data.questions.length - 1) {
      this.setData({ currentIndex: this.data.currentIndex + 1 }, () => { this.refreshCurrentQuestionView(); });
    }
  },

  submitExam() {
    if (this.data.timer) clearInterval(this.data.timer);
    wx.showModal({
      title: '确认交卷',
      content: `已答${Object.keys(this.data.answers).length}/${this.data.questions.length}题，确定提交？`,
      success: (res) => { if (res.confirm) this.doSubmit(); }
    });
  },

  doSubmit() {
    wx.showLoading({ title: '阅卷中' });
    let correctCount = 0, wrongList = [];
    this.data.questions.forEach(q => {
      const userAns = this.data.answers[q.id] || '';
      const correctAns = q.answer || '';
      let isCorrect = false;
      if (q.type === 'judge' || q.type === 'judgment') {
        isCorrect = (userAns === 'A' && correctAns === '正确') || (userAns === 'B' && correctAns === '错误') || userAns === correctAns;
      } else {
        isCorrect = userAns.toUpperCase().split('').sort().join('') === correctAns.toUpperCase().split('').sort().join('');
      }
      isCorrect ? correctCount++ : wrongList.push({ ...q, userAnswer: userAns || '未作答' });
    });

    const total = this.data.questions.length;
    const score = total > 0 ? Math.round((correctCount / total) * 100) : 0;

    const oldWrongs = wx.getStorageSync(storageKey('wrongQuestions')) || [];
    const newWrongs = [...wrongList, ...oldWrongs.filter(o => !wrongList.find(w => w.id === o.id))];
    wx.setStorageSync(storageKey('wrongQuestions'), newWrongs);

    // 计算积分奖励
    let bonusScore = 0;

    // 专项训练完成奖励2积分
    if (this.data.currentType === '专项训练') {
      bonusScore += 2;
    }

    // 错题重考：每完成10个错题奖励2积分
    if (this.data.currentType === '错题重考') {
      const wrongCount = Math.floor(newWrongs.length / 10);
      const oldWrongCount = Math.floor(oldWrongs.length / 10);
      if (wrongCount > oldWrongCount) {
        bonusScore += 2;
      }
    }

    // 更新用户积分
    if (bonusScore > 0) {
      this.addScore(bonusScore);
      wx.showToast({
        title: `完成奖励 +${bonusScore}积分`,
        icon: 'success',
        duration: 2000
      });
    }

    if (this.data.currentChapter !== null && this.data.currentChapter !== undefined) {
      let completed = wx.getStorageSync(storageKey('completedChapters')) || [];
      if (!Array.isArray(completed)) completed = [];
      completed = completed.map(n => Number(n));

      const ch = Number(this.data.currentChapter);
      if (!completed.includes(ch)) {
        completed.push(ch);
        wx.setStorageSync(storageKey('completedChapters'), completed);
        this.setData({ completedChapters: completed });
      }
    }

    this.setData({ examResult: { score, correctCount, wrongCount: total - correctCount, wrongList } });
    wx.hideLoading();
  },

  addScore(points) {
    const userInfo = wx.getStorageSync('userInfo');
    if (userInfo) {
      userInfo.totalScore = (userInfo.totalScore || 0) + points;
      wx.setStorageSync('userInfo', userInfo);
      this.setData({ score: userInfo.totalScore });
    }
  },

  backToHome() {
    if (this.data.timer) clearInterval(this.data.timer);
    this.setData({
      currentType: null,
      examResult: null,
      questions: [],
      answers: {},
      currentOptions: [],
      currentIndex: 0,
      currentChapter: null
    });
  },

  goBack() {
    if (this.data.timer) clearInterval(this.data.timer);

    if (this.data.examResult) {
      // 如果在结果页面，返回到初始状态
      this.backToHome();
    } else if (this.data.currentType) {
      // 如果在答题页面，返回到类型选择
      this.setData({
        currentType: null,
        examResult: null,
        questions: [],
        answers: {},
        currentOptions: [],
        currentIndex: 0,
        currentChapter: null
      });
    } else {
      // 如果在初始页面，返回首页
      wx.reLaunch({
        url: '/pages/index/index'
      });
    }
  },

  onUnload() {
    if (this.data.timer) clearInterval(this.data.timer);
  }
});
