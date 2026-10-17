const api = require('../../utils/api');
const app = getApp();

Page({
    data: {
        title: '',
        description: '',
        deadline: '',

        questionMode: 'ai',

        aiQuestionCount: 5,
        aiQuestionTypeIndex: 0,
        aiDifficultyIndex: 1,
        questionTypes: [
            { value: 1, label: '单选题' },
            { value: 2, label: '多选题' }
        ],
        difficulties: [
            { value: 1, label: '简单' },
            { value: 2, label: '中等' },
            { value: 3, label: '困难' }
        ],

        bankQuestionCount: 10,
        bankCategoryIndex: 0,
        bankTypeIndex: 0,
        categories: [
            { value: null, label: '全部' },
            { value: 1, label: '第一章' },
            { value: 2, label: '第二章' },
            { value: 3, label: '第三章' }
        ],

        generatedQuestions: [],
        singleQuestions: [],
        multiQuestions: [],
        bankQuestions: [],
        pendingQuestions: [],

        generating: false,
        loadingBank: false,
        userInfo: null,
        selectedCount: 0,
        pendingCount: 0
    },

    onLoad() {
        const userInfo = wx.getStorageSync('userInfo');
        if (!userInfo || (userInfo.role !== 'teacher' && userInfo.role !== 'admin')) {
            wx.redirectTo({ url: '/pages/login/login' });
            return;
        }
        this.setData({ userInfo });
    },

    goBack() {
        wx.navigateBack();
    },

    onShow() {
        this.updateSelectedCount();
    },

    onTitleInput(e) {
        this.setData({ title: e.detail.value });
    },

    onDescInput(e) {
        this.setData({ description: e.detail.value });
    },

    onDeadlineChange(e) {
        this.setData({ deadline: e.detail.value });
    },

    switchMode(e) {
        const mode = e.currentTarget.dataset.mode;
        this.setData({ questionMode: mode });
    },

    onAiQuestionCountInput(e) {
        this.setData({ aiQuestionCount: parseInt(e.detail.value) || 5 });
    },

    onAiQuestionTypeChange(e) {
        this.setData({ aiQuestionTypeIndex: parseInt(e.detail.value) });
    },

    onAiDifficultyChange(e) {
        this.setData({ aiDifficultyIndex: parseInt(e.detail.value) });
    },

    onBankQuestionCountInput(e) {
        this.setData({ bankQuestionCount: parseInt(e.detail.value) || 10 });
    },

    onBankCategoryChange(e) {
        this.setData({ bankCategoryIndex: parseInt(e.detail.value) });
    },

    onBankTypeChange(e) {
        this.setData({ bankTypeIndex: parseInt(e.detail.value) });
    },

    updateSelectedCount() {
        const selectedCount = this.data.generatedQuestions.filter(q => q.selected).length;
        const pendingCount = this.data.pendingQuestions.filter(q => q.selected).length;
        this.setData({
            selectedCount,
            pendingCount
        });
    },

    mergeQuestions() {
        const allQuestions = [
            ...this.data.singleQuestions.map((q, idx) => ({ ...q, globalIndex: idx, source: 'ai' })),
            ...this.data.multiQuestions.map((q, idx) => ({ ...q, globalIndex: this.data.singleQuestions.length + idx, source: 'ai' })),
            ...this.data.bankQuestions.map((q, idx) => ({ ...q, globalIndex: this.data.singleQuestions.length + this.data.multiQuestions.length + idx, source: 'bank' }))
        ];
        this.setData({
            generatedQuestions: allQuestions
        });
        this.updateSelectedCount();
    },

    async loadBankQuestions() {
        const { categories, questionTypes, bankQuestionCount, bankCategoryIndex, bankTypeIndex } = this.data;

        if (bankQuestionCount < 1 || bankQuestionCount > 50) {
            wx.showToast({ title: '题目数量应在 1-50 之间', icon: 'none' });
            return;
        }

        this.setData({ loadingBank: true });

        try {
            const category = categories[bankCategoryIndex].value;
            const type = questionTypes[bankTypeIndex].value;

            const params = {};
            if (category !== null && category !== undefined) {
                params.category = category;
            }
            if (type !== null && type !== undefined) {
                params.type = type;
            }
            params.limit = 200;

            const res = await api.getQuestions(params);

            this.setData({ loadingBank: false });

            if (res.code === 200 && res.data && res.data.length > 0) {
                const filteredQuestions = res.data.filter(q => {
                    if (type !== null && type !== undefined) {
                        return q.type === type;
                    }
                    return true;
                });

                if (filteredQuestions.length === 0) {
                    wx.showToast({ title: '没有符合条件的题目', icon: 'none' });
                    return;
                }

                const allQuestions = filteredQuestions;
                const shuffled = this.shuffleArray(allQuestions);
                const selectedQuestions = shuffled.slice(0, Math.min(bankQuestionCount, shuffled.length));

                const questions = selectedQuestions.map(q => {
                    let options = [];
                    try {
                        if (q.options) {
                            options = JSON.parse(q.options);
                        }
                    } catch (e) {
                        console.error('解析选项失败', e);
                    }

                    return {
                        questionId: q.id,
                        questionText: q.title,
                        options: options,
                        correctAnswer: q.answer,
                        analysis: q.analysis,
                        difficulty: q.difficulty || 2,
                        questionType: q.type,
                        selected: false,
                        source: 'bank'
                    };
                });

                this.setData({
                    pendingQuestions: questions
                });

                this.updateSelectedCount();

                wx.showToast({
                    title: '已生成' + questions.length + '道题目，请选择',
                    icon: 'success'
                });
            } else {
                wx.showToast({ title: '题库中没有符合条件的题目', icon: 'none' });
            }
        } catch (err) {
            this.setData({ loadingBank: false });
            console.error('加载题库失败:', err);
            wx.showToast({ title: '加载失败', icon: 'none' });
        }
    },

    shuffleArray(array) {
        const arr = [...array];
        for (let i = arr.length - 1; i > 0; i--) {
            const j = Math.floor(Math.random() * (i + 1));
            [arr[i], arr[j]] = [arr[j], arr[i]];
        }
        return arr;
    },

    async generateQuestions() {
        const { aiQuestionCount, questionTypes, aiQuestionTypeIndex, difficulties, aiDifficultyIndex, userInfo } = this.data;

        if (!userInfo || !userInfo.id) {
            wx.showToast({ title: '请先登录', icon: 'none' });
            return;
        }

        if (aiQuestionCount < 1 || aiQuestionCount > 20) {
            wx.showToast({ title: '题目数量应在 1-20 之间', icon: 'none' });
            return;
        }

        const questionType = questionTypes[aiQuestionTypeIndex].value;
        const questionTypeName = questionTypes[aiQuestionTypeIndex].label;

        this.setData({ generating: true });

        try {
            const res = await api.aiGenerateQuestions(
                userInfo.id,
                aiQuestionCount,
                questionType,
                difficulties[aiDifficultyIndex].value
            );

            console.log('AI返回的原始数据:', JSON.stringify(res, null, 2));

            this.setData({ generating: false });

            if (res.code === 200 && res.data && res.data.length > 0) {
                const questions = res.data.map((q, index) => {
                    let questionText = '';
                    if (q.questionText) questionText = q.questionText;
                    else if (q.content) questionText = q.content;
                    else if (q.title) questionText = q.title;
                    else if (q.question) questionText = q.question;
                    else questionText = '题目内容';

                    let options = [];
                    if (q.options) {
                        if (Array.isArray(q.options)) {
                            options = q.options;
                        } else if (typeof q.options === 'string') {
                            try {
                                options = JSON.parse(q.options);
                            } catch(e) {
                                options = [];
                            }
                        }
                    }

                    if (options.length === 0 && q.optionA) {
                        options = [q.optionA, q.optionB, q.optionC, q.optionD].filter(v => v);
                    }

                    let correctAnswer = q.answer || q.correctAnswer || '';
                    let analysis = q.analysis || q.explanation || '';

                    console.log(`解析第${index+1}题:`, { questionText, options, correctAnswer, analysis });

                    return {
                        questionId: Date.now() + index + Math.random(),
                        questionText: questionText,
                        options: options,
                        correctAnswer: correctAnswer,
                        analysis: analysis,
                        selected: false,
                        questionType: questionType,
                        questionTypeName: questionTypeName,
                        source: 'ai',
                        difficulty: difficulties[aiDifficultyIndex].value
                    };
                });

                this.setData({
                    pendingQuestions: [...this.data.pendingQuestions, ...questions]
                });

                this.updateSelectedCount();

                wx.showToast({ title: '已生成' + questions.length + '道' + questionTypeName + '，请选择', icon: 'success' });
            } else {
                wx.showToast({ title: res.msg || '生成失败，请重试', icon: 'none' });
            }
        } catch (err) {
            this.setData({ generating: false });
            console.error('生成题目失败:', err);
            wx.showToast({ title: '生成失败', icon: 'none' });
        }
    },

    togglePendingQuestion(e) {
        const index = e.currentTarget.dataset.index;
        const pendingQuestions = this.data.pendingQuestions;
        pendingQuestions[index].selected = !pendingQuestions[index].selected;
        this.setData({ pendingQuestions });
        this.updateSelectedCount();
    },

    confirmPendingQuestions() {
        const { pendingQuestions, singleQuestions, multiQuestions, bankQuestions } = this.data;

        const selectedPending = pendingQuestions.filter(q => q.selected);

        if (selectedPending.length === 0) {
            wx.showToast({ title: '请至少选择一道题目', icon: 'none' });
            return;
        }

        const newSingle = selectedPending.filter(q => q.questionType === 1);
        const newMulti = selectedPending.filter(q => q.questionType === 2);
        const newBank = selectedPending.filter(q => q.source === 'bank');

        this.setData({
            singleQuestions: [...singleQuestions, ...newSingle],
            multiQuestions: [...multiQuestions, ...newMulti],
            bankQuestions: [...bankQuestions, ...newBank],
            pendingQuestions: []
        });

        this.mergeQuestions();

        wx.showToast({ title: '已加入' + selectedPending.length + '道题目', icon: 'success' });
    },

    clearPendingQuestions() {
        this.setData({
            pendingQuestions: []
        });
        this.updateSelectedCount();
        wx.showToast({ title: '已清空', icon: 'success' });
    },

    toggleQuestion(e) {
        const index = e.currentTarget.dataset.index;
        const generatedQuestions = this.data.generatedQuestions;
        generatedQuestions[index].selected = !generatedQuestions[index].selected;
        this.setData({ generatedQuestions });
        this.updateSelectedCount();
    },

    clearAllQuestions() {
        wx.showModal({
            title: '确认清空',
            content: '确定要清空所有已选择的题目吗？',
            success: (res) => {
                if (res.confirm) {
                    this.setData({
                        singleQuestions: [],
                        multiQuestions: [],
                        bankQuestions: [],
                        generatedQuestions: [],
                        pendingQuestions: [],
                        selectedCount: 0
                    });
                    wx.showToast({ title: '已清空', icon: 'success' });
                }
            }
        });
    },

    // 保存并提交审核
    async publishAssignment() {
        const { title, description, deadline, generatedQuestions, userInfo } = this.data;

        if (!title) {
            wx.showToast({ title: '请输入作业标题', icon: 'none' });
            return;
        }

        if (!deadline) {
            wx.showToast({ title: '请选择截止时间', icon: 'none' });
            return;
        }

        const selectedQuestions = generatedQuestions.filter(q => q.selected);

        if (selectedQuestions.length === 0) {
            wx.showToast({ title: '请至少选择一道题目', icon: 'none' });
            return;
        }

        wx.showLoading({ title: '保存中...' });

        try {
            // 第一步：创建作业（保存为草稿）
            const res = await api.createAssignment({
                title,
                description,
                deadline,
                teacherId: userInfo.id,
                questions: selectedQuestions.map((q, index) => ({
                    questionId: q.questionId || Date.now() + index,
                    questionText: q.questionText,
                    options: JSON.stringify(q.options),
                    answer: q.correctAnswer,
                    analysis: q.analysis,
                    type: q.questionType,
                    difficulty: q.difficulty || 2
                }))
            });

            if (res.code === 200 && res.data) {
                const assignmentId = res.data.id;
                // 第二步：提交审核
                try {
                    const reviewRes = await api.submitForReview(assignmentId);
                    wx.hideLoading();
                    if (reviewRes.code === 200) {
                        wx.showModal({
                            title: '提交成功',
                            content: '作业已保存并提交给法务人员审核，审核通过后即可发布。',
                            showCancel: false,
                            success: () => wx.navigateBack()
                        });
                    } else {
                        wx.showToast({ title: '已保存为草稿，可在作业管理中提交审核', icon: 'none' });
                        setTimeout(() => wx.navigateBack(), 2000);
                    }
                } catch (err) {
                    wx.hideLoading();
                    wx.showToast({ title: '已保存为草稿，可在作业管理中提交审核', icon: 'none' });
                    setTimeout(() => wx.navigateBack(), 2000);
                }
            } else {
                wx.hideLoading();
                wx.showToast({ title: res.msg || '保存失败', icon: 'none' });
            }
        } catch (err) {
            wx.hideLoading();
            console.error('保存失败:', err);
            wx.showToast({ title: '保存失败', icon: 'none' });
        }
    }
});