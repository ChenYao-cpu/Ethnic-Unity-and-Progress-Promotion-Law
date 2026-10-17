const fs=require('fs'),path=require('path');
const sharp=require('C:/Users/ASUS/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');
const root=path.resolve(__dirname,'..'),out=path.join(root,'docs/posters'),web=path.join(root,'src/main/resources/static/console/posters');
fs.mkdirSync(web,{recursive:true});
const brand=fs.readFileSync(path.join(root,'frontend/images/brand/logo-horizontal-transparent.png')).toString('base64');
const img=`data:image/png;base64,${brand}`;
const defs=`<defs><linearGradient id="paper" x2="1" y2="1"><stop stop-color="#fffdf5"/><stop offset="1" stop-color="#f4e8da"/></linearGradient><linearGradient id="red" x2="1" y2="1"><stop stop-color="#d07367"/><stop offset="1" stop-color="#791817"/></linearGradient><linearGradient id="gold" x2=".7" y2="1"><stop stop-color="#f6dcaf"/><stop offset="1" stop-color="#c68b54"/></linearGradient></defs>`;
const text=(x,y,s,size=40,color='#791817',weight=400,spacing=0)=>`<text x="${x}" y="${y}" font-family="Microsoft YaHei" font-size="${size}" fill="${color}" font-weight="${weight}" letter-spacing="${spacing}">${s}</text>`;
const logo=(x,y,w)=>`<image x="${x}" y="${y}" width="${w}" height="${w/3}" href="${img}"/>`;
const symbol=(x,y,w,h=w)=>`<svg x="${x}" y="${y}" width="${w}" height="${h}" viewBox="0 0 760 725"><image width="2170" height="725" href="${img}"/></svg>`;
const line=(x1,y1,x2,y2,c='#d3ad86')=>`<path d="M${x1} ${y1}L${x2} ${y2}" stroke="${c}" stroke-width="2" fill="none"/>`;
const spark=(x,y,s)=>`<path d="M${x} ${y-s}Q${x+3} ${y-3} ${x+s} ${y}Q${x+3} ${y+3} ${x} ${y+s}Q${x-3} ${y+3} ${x-s} ${y}Q${x-3} ${y-3} ${x} ${y-s}" fill="url(#gold)"/>`;
const landscape=(w,h)=>`<path d="M0 ${h-190}L${w*.17} ${h-365}L${w*.28} ${h-245}L${w*.46} ${h-490}L${w*.66} ${h-225}L${w*.81} ${h-375}L${w} ${h-180}V${h}H0Z" fill="#e8c6ac" opacity=".5"/><path d="M0 ${h-110}Q${w*.24} ${h-350} ${w*.55} ${h-150}T${w} ${h-215}V${h}H0Z" fill="url(#red)"/><path d="M0 ${h-95}Q${w*.25} ${h-310} ${w*.56} ${h-130}T${w} ${h-190}" fill="none" stroke="url(#gold)" stroke-width="4"/>`;
function mobile(name,index,kicker,titleLines,sub,details){
 const w=1080,h=1920;
 let content=`${defs}<rect width="${w}" height="${h}" fill="url(#paper)"/><circle cx="1000" cy="680" r="550" fill="#f2d8c5" opacity=".36"/><circle cx="1000" cy="680" r="470" fill="none" stroke="#d6b48f" stroke-width="1" opacity=".5"/>${logo(80,70,440)}${text(84,295,kicker,25,'#a95f4c',400,7)}`;
 titleLines.forEach((t,i)=>content+=text(80,408+i*112,t,86,'#791817',700,1));
 content+=line(84,710,205,710)+text(84,785,sub,31,'#8d5f50',400,2);
 if(name==='mobile-agents'){
  content+=`<path d="M220 1130L540 910L855 1130L540 1405Z" fill="none" stroke="#d6ab77" stroke-width="3" stroke-dasharray="8 12"/>`;
  for(const [x,y,label] of [[220,1130,'检索'],[540,910,'审题'],[855,1130,'修订']])content+=`<circle cx="${x}" cy="${y}" r="62" fill="#fffaf0" stroke="#d6ab77" stroke-width="2"/>${text(x-34,y+12,label,32,'#791817',600)}`;
  content+=symbol(275,1010,550,540);
 }else if(name==='mobile-review'){
  content+=symbol(205,920,670,640)+`<circle cx="818" cy="1340" r="94" fill="#791817"/><path d="M774 1340l31 32 58-67" fill="none" stroke="#f4dab0" stroke-width="14" stroke-linecap="round" stroke-linejoin="round"/>`;
 }else{content+=symbol(205,920,670,640)+spark(195,1070,22)+spark(885,940,30);}
 content+=landscape(w,h)+text(84,1610,details,29,'#8d5f50')+text(80,1840,'云疆知法',26,'#fff4df',600,3)+text(800,1840,index+' / 03',24,'#fff4df',400,3);
 return `<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}">${content}</svg>`;
}
(async()=>{
const webSvg=`<svg xmlns="http://www.w3.org/2000/svg" width="1920" height="1080">${defs}<rect width="1920" height="1080" fill="url(#paper)"/><circle cx="1550" cy="420" r="600" fill="#f1d6c2" opacity=".35"/><circle cx="1550" cy="420" r="450" fill="none" stroke="#d6b48f" stroke-width="1"/>${logo(100,60,440)}${text(105,310,'同心学法 · 有据可查',24,'#a75b49',400,6)}${text(100,455,'让每一次普法学习，',88,'#791817',700)}${text(100,575,'更有依据。',104,'#791817',700)}${line(105,645,250,645)}${text(105,715,'多智能体协同 · 法务核验 · 同心学法',31,'#8d5f50',400,2)}${symbol(1110,190,710,700)}${spark(1710,240,35)}${landscape(1920,1080)}${text(105,1000,'课程学习  /  趣味练习  /  专业审核',27,'#fff4df',400,4)}</svg>`;
const posters={web:webSvg,'mobile-learning':mobile('mobile-learning','01','学习 · LEARNING',['把法律知识，','学进日常。'],'课程学习 · 趣味练习 · 学习笔记','让每一次主动探索，都成为新的积累。'),'mobile-agents':mobile('mobile-agents','02','协同 · COLLABORATION',['多智能体协同，','让出题','有据可循。'],'法条检索 · 独立审题 · 反馈修订','从检索到修订，让知识建设环环相扣。'),'mobile-review':mobile('mobile-review','03','核验 · REVIEW',['以专业核验，','守护学习质量。'],'法务审核 · 课程管理 · 学习反馈','逐题核查题目与解析，记录审核意见。')};
for(const [name,svg] of Object.entries(posters)){
 fs.writeFileSync(path.join(out,name+'.svg'),svg);await sharp(Buffer.from(svg)).png().toFile(path.join(out,name+'.png'));fs.copyFileSync(path.join(out,name+'.png'),path.join(web,name+'.png'));
}
console.log('Created one 1920x1080 web poster and three 1080x1920 mobile posters');
})();
