'use strict';
(() => {
 const app=document.querySelector('#app');
 const reduced=window.matchMedia('(prefers-reduced-motion: reduce)');
 let scheduled=false;
 const observer='IntersectionObserver' in window?new IntersectionObserver(entries=>{
  for(const entry of entries)if(entry.isIntersecting){entry.target.classList.add('is-visible');observer.unobserve(entry.target);}
 },{threshold:.12,rootMargin:'0px 0px -16px 0px'}):null;
 function refresh(){
  scheduled=false;
  app.querySelectorAll('.public-hero-copy,.public-section-heading,.public-poster-grid article').forEach(node=>{
   if(node.dataset.motionReady)return;
   node.dataset.motionReady='true';
   if(observer&&!reduced.matches&&!document.hidden){node.classList.add('motion-pending');observer.observe(node);}
   else node.classList.add('is-visible');
  });
  document.querySelector('.public-header')?.classList.toggle('is-scrolled',window.scrollY>16);
 }
 function schedule(){if(!scheduled){scheduled=true;requestAnimationFrame(refresh);}}
 const changes=new MutationObserver(schedule);changes.observe(app,{childList:true,subtree:true});
 window.addEventListener('scroll',schedule,{passive:true});
 document.addEventListener('visibilitychange',()=>{document.documentElement.classList.toggle('motion-paused',document.hidden);if(!document.hidden)schedule();});
 reduced.addEventListener('change',()=>{if(reduced.matches){app.querySelectorAll('.motion-pending').forEach(node=>node.classList.add('is-visible'));observer?.disconnect();}schedule();});
 refresh();
})();
