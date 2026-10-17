'use strict';
function withContentState(load){
 const wrapped=async function(...args){
  const container=document.querySelector('#content');
  if(container){container.innerHTML=skeletonView();container.setAttribute('aria-busy','true');}
  try{return await load.apply(this,args)}
  catch(error){if(container?.isConnected){container.innerHTML=`<div class="error" role="alert"><h3>暂时没有加载成功</h3><p>${e(error.message)}</p><button data-state-retry>重新加载</button></div>`;container._retryContent=()=>wrapped.apply(this,args);}throw error;}
  finally{if(container?.isConnected)container.removeAttribute('aria-busy');}
 };
 return wrapped;
}
detail=withContentState(detail);courseForm=withContentState(courseForm);showDetailedProgress=withContentState(showDetailedProgress);
root.addEventListener('click',event=>{if(event.target.closest('[data-state-retry]')){const c=document.querySelector('#content');if(c?._retryContent)c._retryContent().catch(error=>toast(error.message));}});
