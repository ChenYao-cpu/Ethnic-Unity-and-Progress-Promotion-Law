# API接口文档

## 用户模块 `/api/user`
| 接口 | 方法 | 说明 |
|------|------|------|
| /login | POST | 登录 |
| /register | POST | 注册 |
| /info | GET | 获取用户信息 |
| /update | PUT | 更新用户信息 |

## 题目模块 `/api/question`
| 接口 | 方法 | 说明 |
|------|------|------|
| /random | GET | 随机获取题目 |
| /check | POST | 提交答案 |
| /notes | GET | 获取题目笔记 |
| /notes | POST | 保存题目笔记 |

## 错题模块 `/api/wrong`
| 接口 | 方法 | 说明 |
|------|------|------|
| /list | GET | 错题列表 |
| /remove | DELETE | 移除错题 |

## 考试模块 `/api/exam`
| 接口 | 方法 | 说明 |
|------|------|------|
| /start | POST | 开始考试 |
| /submit | POST | 提交考试 |
| /history | GET | 考试历史 |

## 课程模块 `/api/course`
| 接口 | 方法 | 说明 |
|------|------|------|
| /list | GET | 课程列表 |
| /detail/{id} | GET | 课程详情 |
| /recommend | GET | 推荐课程 |

## 学习记录 `/api/study`
| 接口 | 方法 | 说明 |
|------|------|------|
| /record | POST | 记录学习 |
| /overview | GET | 学习总览 |
| /progress | GET | 课程进度 |
| /stats | GET | 学习统计 |

## 笔记模块 `/api/note`
| 接口 | 方法 | 说明 |
|------|------|------|
| /list | GET | 笔记列表 |
| /save | POST | 保存笔记 |
| /delete | DELETE | 删除笔记 |

## 公告模块 `/api/notice`
| 接口 | 方法 | 说明 |
|------|------|------|
| /latest | GET | 最新公告 |

## AI模块 `/api/ai`
| 接口 | 方法 | 说明 |
|------|------|------|
| /ask | POST | AI提问 |
| /history | GET | 对话历史 |
