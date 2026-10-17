const app = getApp()

function shuffleArray(array) {
  const arr = [...array]
  for (let i = arr.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[arr[i], arr[j]] = [arr[j], arr[i]]
  }
  return arr
}

async function generateExamPaper(config) {
  const {
    singleCount = 10,
    multiCount = 4,
    judgmentCount = 5,
    fillCount = 5,
    essayCount = 2
  } = config

  return new Promise((resolve, reject) => {
    wx.request({
      url: `${app.globalData.baseUrl}/question/list`,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.code === 200) {
          const allQuestions = res.data.data || []

          const single = shuffleArray(allQuestions.filter(q => q.type === 1)).slice(0, singleCount)
          const multi = shuffleArray(allQuestions.filter(q => q.type === 2)).slice(0, multiCount)
          const judgment = shuffleArray(allQuestions.filter(q => q.type === 3)).slice(0, judgmentCount)
          const fill = shuffleArray(allQuestions.filter(q => q.type === 4)).slice(0, fillCount)
          const essay = shuffleArray(allQuestions.filter(q => q.type === 5)).slice(0, essayCount)

          let id = 1
          const questions = [
            ...single.map(q => ({
              ...q,
              id: id++,
              type: 'single',
              score: 5,
              options: q.options ? JSON.parse(q.options) : []
            })),
            ...multi.map(q => ({
              ...q,
              id: id++,
              type: 'multi',
              score: 5,
              options: q.options ? JSON.parse(q.options) : []
            })),
            ...judgment.map(q => ({
              ...q,
              id: id++,
              type: 'judgment',
              score: 2,
              options: q.options ? JSON.parse(q.options) : []
            })),
            ...fill.map(q => ({
              ...q,
              id: id++,
              type: 'fill',
              score: 3
            })),
            ...essay.map(q => ({
              ...q,
              id: id++,
              type: 'essay',
              score: 10
            }))
          ]

          resolve({
            questions,
            totalScore: 100,
            config: { singleCount, multiCount, judgmentCount, fillCount, essayCount }
          })
        } else {
          reject(new Error('获取题目失败'))
        }
      },
      fail: (err) => {
        reject(err)
      }
    })
  })
}

module.exports = { generateExamPaper }
