const fs=require('fs'),path=require('path');
const sharp=require('C:/Users/ASUS/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');
const root=path.resolve(__dirname,'..');
const source=path.join(root,'frontend/images/brand/approved-brand-board.png');
const out=path.join(root,'frontend/images/brand'),web=path.join(root,'src/main/resources/static/console/brand');
(async()=>{

 const encoded=fs.readFileSync(source).toString('base64');
 const variants={'logo-horizontal':[466,730,590,185],'logo-web':[466,730,590,185],'logo-symbol':[80,690,320,245],'app-icon':[68,655,344,344]};
 for(const [name,[x,y,w,h]] of Object.entries(variants)){
  // A viewport of the approved artwork preserves the exact selected design.
  const svg=`<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="${x} ${y} ${w} ${h}"><title>云疆知法</title><image width="1536" height="1024" href="data:image/png;base64,${encoded}"/></svg>`;
  for(const dir of [out,web])fs.writeFileSync(path.join(dir,name+'.svg'),svg);
  if(name!=='app-icon'){
   await sharp(Buffer.from(svg)).resize({width:name==='logo-symbol'?640:1180}).png().toFile(path.join(out,name+'.png'));
   fs.copyFileSync(path.join(out,name+'.png'),path.join(web,name+'.png'));
  }else for(const size of [96,192,512,1024]){
   const file=`app-icon-${size}.png`;
   await sharp(Buffer.from(svg)).resize(size,size).png().toFile(path.join(out,file));
   fs.copyFileSync(path.join(out,file),path.join(web,file));
  }
 }
 console.log('Approved artwork installed in web and mini-program asset directories');
})();
