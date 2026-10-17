const api=require('../../utils/api.js')
Page({
 data:{questionId:null,answer:'',reason:'',busy:false,feedback:null,error:''},
 onLoad(options){this.setData({questionId:Number(options.id),answer:options.answer?decodeURIComponent(options.answer):''})},
 onAnswer(e){this.setData({answer:e.detail.value.toUpperCase()})},
 onReason(e){this.setData({reason:e.detail.value})},
 async askTutor(){
  const user=wx.getStorageSync('userInfo')
  if(!user||!user.authToken){wx.showModal({title:'请重新登录',content:'AI辅导需要登录验证，请重新登录后再进入本页。',showCancel:false,success:()=>wx.navigateTo({url:'/pages/login/login'})});return}
  if(!this.data.questionId||!this.data.answer){wx.showToast({title:'请填写你所选的答案字母',icon:'none'});return}
  this.setData({busy:true,error:'',feedback:null})
  try{const res=await api.tutorQuestion(this.data.questionId,this.data.answer,this.data.reason);if(res.code!==200)throw new Error(res.msg||'辅导失败');this.setData({feedback:res.data})}
  catch(e){this.setData({error:e.message||'网络请求失败，请稍后重试'})}
  finally{this.setData({busy:false})}
 }
})
