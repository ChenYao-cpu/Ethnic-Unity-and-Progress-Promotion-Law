const fs=require('fs'),path=require('path'),sharp=require('C:/Users/ASUS/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');
const root=path.resolve(__dirname,'..'),out=path.join(root,'frontend/images/features');fs.mkdirSync(out,{recursive:true});
(async()=>{
for(const name of ['study','ai','practice','exam','note','notice']){
 const original=fs.readFileSync(path.join(root,'frontend/styles/yunjiang',name+'.svg'),'utf8');
 const mark=original.replace(/<svg[^>]*>|<\/svg>|<title>.*?<\/title>/g,'').replace(/fill="#791817"/g,'fill="url(#ink)"');
 const svg=`<svg xmlns="http://www.w3.org/2000/svg" width="128" height="128" viewBox="0 0 128 128"><defs><linearGradient id="ink" x2=".7" y2="1"><stop stop-color="#d07367"/><stop offset=".55" stop-color="#9e3027"/><stop offset="1" stop-color="#791817"/></linearGradient><linearGradient id="paper" x2="1" y2="1"><stop stop-color="#fff9e9"/><stop offset="1" stop-color="#f3d8c3"/></linearGradient></defs><rect x="8" y="8" width="112" height="112" rx="30" fill="url(#paper)"/><path d="M24 99Q39 85 56 98T101 94" fill="none" stroke="#d9a36b" stroke-width="3" opacity=".6"/><path d="M88 24l3 7 7 3-7 3-3 7-3-7-7-3 7-3z" fill="#d9a36b"/><g transform="translate(20 20) scale(1.375)">${mark}</g></svg>`;
 fs.writeFileSync(path.join(out,name+'.svg'),svg);await sharp(Buffer.from(svg)).resize(256,256).png().toFile(path.join(out,name+'.png'));
}
const names=['study','ai','practice','exam','note','notice'],labels=['课程学习','AI助学','趣味练习','普法测验','学习笔记','普法公告'];
let tiles='';for(let i=0;i<6;i++){const x=24+(i%3)*220,y=24+Math.floor(i/3)*240;tiles+=`<g transform="translate(${x} ${y})"><rect width="204" height="224" rx="16" fill="#fffdf8" stroke="#ead8ca"/><image x="38" y="20" width="128" height="128" href="data:image/png;base64,${fs.readFileSync(path.join(out,names[i]+'.png')).toString('base64')}"/><text x="102" y="190" text-anchor="middle" font-family="Microsoft YaHei" font-size="20" fill="#791817">${labels[i]}</text></g>`;}
await sharp(Buffer.from(`<svg xmlns="http://www.w3.org/2000/svg" width="708" height="504"><rect width="100%" height="100%" fill="#faf7eb"/>${tiles}</svg>`)).png().toFile(path.join(root,'docs/feature-icons-preview.png'));
})();
