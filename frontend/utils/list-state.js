// Only read endpoints used for visible content are tracked; writes and AI replies keep their own feedback.
const states = new WeakMap()
const keys = { '/notice/list':'notices','/notice/detail':'noticeDetail','/course/list':'courses','/note/list':'notes','/wrong/list':'wrong','/game/leaderboard':'leaderboard','/game/achievements':'achievements','/assignment/student/list':'assignments','/assignment/submission/list':'assignments','/assignment/teacher/list':'assignments','/reminder/my':'reminders','/ai/history':'history','/study/total':'study','/study/recent':'study','/study/stats':'study','/study/profile':'study','/study/checkin/calendar':'study','/study/report/weekly':'study','/question/list':'questions' }
function begin(url, method) {
 if (method !== 'GET' || typeof getCurrentPages !== 'function') return () => {}
 const endpoint=url.split('?')[0], key=keys[endpoint] || (endpoint.startsWith('/notice/detail/')?'noticeDetail':null)
 const pages=getCurrentPages(), page=pages[pages.length-1]
 if(!key||!page)return () => {}
 let groups=states.get(page);if(!groups){groups={};states.set(page,groups)}
 const state=groups[key]||(groups[key]={pending:0,error:''})
 if(state.pending===0)state.error=''
 state.pending++
 page.setData({['_listStates.'+key]:{loading:true,error:''}})
 let finished=false
 return error=>{if(finished)return;finished=true;state.pending--;if(error)state.error=String(error.message||error.errMsg||error);if(!state.pending && getCurrentPages().includes(page))page.setData({['_listStates.'+key]:{loading:false,error:state.error}})}
}
module.exports={begin}
