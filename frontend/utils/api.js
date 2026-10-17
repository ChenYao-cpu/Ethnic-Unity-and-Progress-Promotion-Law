const { request } = require('./request.js')

const api = {
    login: (data) => request('/user/login', 'POST', data),
    register: (data) => request('/user/register', 'POST', data),
    getUserInfo: (userId) => request('/user/info?userId=' + userId, 'GET'),
    updateUserInfo: (data) => request('/user/update', 'PUT', data),

    getCourses: (params) => request('/course/list', 'GET', params),
    getCourseDetail: (id) => request(`/course/detail/${id}`, 'GET'),
    startStudy: (data) => request('/study/start', 'POST', data),
    updateStudyProgress: (data) => request('/study/progress', 'POST', data),
    getStudyRecords: (userId) => request('/study/records?userId=' + userId, 'GET'),

    getQuestions: (params) => request('/question/list', 'GET', params),
    checkAnswer: (userId, questionId, userAnswer) =>
        request('/question/answer?userId='+userId+'&questionId='+questionId+'&userAnswer='+encodeURIComponent(userAnswer), 'POST'),
    getWrongQuestions: (userId) => request('/wrong/list?userId=' + userId, 'GET'),
    removeWrongQuestion: (userId, questionId) =>
        request('/wrong/remove', 'DELETE', { userId, questionId }),

    removeWrongRecord: (id) => request('/wrong/'+id, 'DELETE'),
    getKnowledgeTree: (userId) => request('/tree/my?userId=' + userId, 'GET'),
    growTree: (data) => request('/tree/feed', 'POST', data),

    getNotices: () => request('/notice/list', 'GET'),
    getNoticeDetail: (id) => request(`/notice/detail/${id}`, 'GET'),

    getNotes: (userId) => request('/note/list?userId=' + userId, 'GET'),
    createNote: (data) => request('/note/add?userId='+data.userId, 'POST', data),
    updateNote: (data) => request('/note/update', 'PUT', data),
    deleteNote: (id) => request(`/note/${id}`, 'DELETE'),

    startExam: (type) => request('/exam/start?type=' + type, 'POST'),
    submitExam: (data) => request('/exam/submit', 'POST', data),

    startMatchLevel: (userId, levelId) => request('/game/match/start', 'POST', { userId, levelId }),
    submitMatchLevel: (userId, levelId, answers, timeUsed) =>
        request('/game/match/submit', 'POST', { userId, levelId, answers, timeUsed }),

    startQuickLevel: (userId, levelId) => request('/game/quick/start', 'POST', { userId, levelId }),
    submitQuickLevel: (userId, levelId, answers, timeUsed) =>
        request('/game/quick/submit', 'POST', { userId, levelId, answers, timeUsed }),

    startScenarioLevel: (userId, levelId) => request('/game/scenario/start', 'POST', { userId, levelId }),
    submitScenarioLevel: (userId, levelId, answers) =>
        request('/game/scenario/submit', 'POST', { userId, levelId, answers }),

    getAchievements: (userId) => request('/game/achievements?userId=' + userId, 'GET'),
    getLeaderboard: (limit = 10) => request('/game/leaderboard?limit=' + limit, 'GET'),
    getGameStats: (userId) => request('/game/stats?userId=' + userId, 'GET'),

    // ================= 打卡相关接口 =================
    checkin: (userId) => request('/study/checkin?userId=' + userId, 'POST'),
    getCheckinCalendar: (userId) => request('/study/checkin/calendar?userId=' + userId, 'GET'),
    getCheckinStats: (userId) => request('/study/stats/summary?userId=' + userId, 'GET'),

    // ================= 学习统计相关接口 =================
    getStudyOverview: (userId) => request('/study/total?userId=' + userId, 'GET'),
    getStudyRecent: (userId, limit = 10) => request('/study/recent?userId=' + userId + '&limit=' + limit, 'GET'),
    getStudyStats: (userId, days = 7) => request('/study/stats?userId=' + userId + '&days=' + days, 'GET'),
    getStudyProfile: (userId) => request('/study/profile?userId=' + userId, 'GET'),
    getWeeklyReport: (userId) => request('/study/report/weekly?userId=' + userId, 'GET'),

    // ================= 教师/管理模块 =================
    getTeacherStats: () => request('/teacher/stats', 'GET'),
    getTeacherAssignments: (params) => request('/assignment/teacher/list', 'GET', params),
    getStudentAssignments: (params) => request('/assignment/student/list', 'GET', params),
    getAssignmentDetail: (id) => request(`/assignment/detail/${id}`, 'GET'),
    getAssignmentSubmissions: (params) => request('/assignment/submissions', 'GET', params),
    publishAssignment: (id) => request(`/assignment/publish/${id}`, 'POST'),
    deleteAssignment: (id) => request(`/assignment/${id}`, 'DELETE'),
    exportAssignmentScores: (id) => request(`/assignment/export/${id}`, 'GET'),
    createAssignment: (data) => request('/assignment/create', 'POST', data),
    updateAssignment: (id, data) => request(`/assignment/update/${id}`, 'PUT', data),
    submitAssignment: (data) => request('/assignment/submit', 'POST', data),

    getStudentProgressList: (params) => request('/student/progress/list', 'GET', params),
    getStudentDetail: (studentId) => request(`/student/detail/${studentId}`, 'GET'),

    // 新增的教师端接口
    getAssignmentSubmissionsByAssignmentId: (assignmentId) => request(`/teacher/assignment/${assignmentId}/submissions`, 'GET'),
    getAssignmentStatistics: (assignmentId) => request(`/teacher/assignment/${assignmentId}/statistics`, 'GET'),
    getStudentProgress: (studentId) => request(`/teacher/student/${studentId}/progress`, 'GET'),

    // ================= AI助学模块 =================
    askAI: (userId, question) => request('/ai/ask', 'POST', { userId, question }),
    getChatHistory: (userId) => request('/ai/history?userId=' + userId, 'GET'),

    // 法条解读（返回结构化数据，由页面层格式化显示）
    interpretLaw: async (userId, keyword) => {
        const res = await request('/ai/interpret', 'POST', { userId, keyword })
        return res
    },

    // AI生成题目（教师端专用）
    aiGenerateQuestions: (userId, questionCount, questionType, difficulty) =>
        request('/ai/generateQuestions', 'POST', { userId, questionCount, questionType, difficulty }),

    // 保存题目到数据库（教师端专用）
    createQuestion: (data) => request('/question/create', 'POST', data),

    // 获取作业提交记录（学生端专用）
    getAssignmentSubmission: (assignmentId, studentId) =>
        request(`/assignment/submission?assignmentId=${assignmentId}&studentId=${studentId}`, 'GET'),

    // 获取学生所有提交记录
    getAssignmentSubmissionList: (studentId) =>
        request(`/assignment/submission/list?studentId=${studentId}`, 'GET'),

    // ================= 邮箱验证 =================
    sendVerifyCode: (email) => request('/user/sendCode', 'POST', { email }),

    // ================= 审核相关接口 =================
    submitForReview: (id) => request(`/assignment/${id}/submit-review`, 'POST'),
    getPendingReviews: () => request('/review/pending', 'GET'),
    getReviewHistory: () => request('/review/history', 'GET'),
    getReviewDetail: (id) => request(`/review/detail/${id}`, 'GET'),
    approveReview: (id, data) => request(`/review/${id}/approve`, 'POST', data),
    rejectReview: (id, data) => request(`/review/${id}/reject`, 'POST', data),
    // 逐题审核
    approveReviewQuestion: (reviewId, questionId, comment) =>
        request(`/review/${reviewId}/approve-question/${questionId}`, 'POST', { comment }),
    rejectReviewQuestion: (reviewId, questionId, comment) =>
        request(`/review/${reviewId}/reject-question/${questionId}`, 'POST', { comment }),
    submitReviewResult: (reviewId, data) =>
        request(`/review/${reviewId}/submit-review`, 'POST', data),

    // ================= 作业题目管理 =================
    getAssignmentQuestions: (id) => request(`/assignment/${id}/questions`, 'GET'),
    addQuestionToAssignment: (assignmentId, questionData) =>
        request(`/assignment/${assignmentId}/questions`, 'POST', questionData),
    removeQuestionFromAssignment: (assignmentId, questionId) =>
        request(`/assignment/${assignmentId}/questions/${questionId}`, 'DELETE'),
    updateQuestion: (id, data) => request(`/question/update/${id}`, 'PUT', data),
    deleteQuestion: (id) => request(`/question/${id}`, 'DELETE'),

    // ================= 学习进度相关接口 =================
    getAssignmentProgress: (assignmentId) =>
        request(`/teacher/assignment/${assignmentId}/progress`, 'GET'),
    getTeacherAssignmentsList: (teacherId) =>
        request(`/teacher/assignments?teacherId=${teacherId}`, 'GET'),

    // ================= 催促提醒接口 =================
    sendReminder: (data) => request('/reminder/send', 'POST', data),
    sendBatchReminder: (data) => request('/reminder/send-batch', 'POST', data),
    getMyReminders: (userId) => request(`/reminder/my?userId=${userId}`, 'GET'),
    getUnreadReminderCount: (userId) => request(`/reminder/unread-count?userId=${userId}`, 'GET'),
    markReminderRead: (id) => request(`/reminder/${id}/read`, 'POST'),
    markAllRemindersRead: (userId) => request('/reminder/read-all', 'POST', { userId }),

    // ================= 课程管理（教师端） =================
    getTeacherCourses: (teacherId) => request(`/course/teacher/list?teacherId=${teacherId}`, 'GET'),
    createCourse: (data) => request('/course/create', 'POST', data),
    updateCourse: (id, data) => request(`/course/update/${id}`, 'PUT', data),
    deleteCourse: (id) => request(`/course/${id}`, 'DELETE'),
    submitCourseForReview: (id) => request(`/course/${id}/submit-review`, 'POST'),
    publishCourse: (id) => request(`/course/${id}/publish`, 'POST'),
    unpublishCourse: (id) => request(`/course/${id}/unpublish`, 'POST'),

    // ================= 课程审核（法务端） =================
    getPendingCourseReviews: () => request('/review/course/pending', 'GET'),
    approveCourse: (id, data) => request(`/review/course/${id}/approve`, 'POST', data),
    rejectCourse: (id, data) => request(`/review/course/${id}/reject`, 'POST', data)
}

module.exports = api