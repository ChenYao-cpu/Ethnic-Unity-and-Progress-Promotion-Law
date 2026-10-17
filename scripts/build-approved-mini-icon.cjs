const fs=require('fs'),path=require('path');
const sharp=require('C:/Users/ASUS/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');
const root=path.resolve(__dirname,'..'),out=path.join(root,'frontend/images/brand'),web=path.join(root,'src/main/resources/static/console/brand');
(async()=>{
 const data=fs.readFileSync(path.join(out,'approved-brand-board.png')).toString('base64');
 const image=`<image width="1536" height="1024" href="data:image/png;base64,${data}"/>`;
 const svg=`<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 512 512"><title>云疆知法 · 小程序竖版标志</title><rect width="512" height="512" rx="70" fill="#faf7eb"/><svg x="70" y="30" width="372" height="310" viewBox="80 685 320 255">${image}</svg><svg x="38" y="362" width="436" height="108" viewBox="675 765 375 94">${image}</svg></svg>`;
 for(const dir of [out,web])fs.writeFileSync(path.join(dir,'app-icon.svg'),svg);
 for(const size of [96,192,512,1024]){
  const name=`app-icon-${size}.png`;
  await sharp(Buffer.from(svg)).resize(size,size).png().toFile(path.join(out,name));
  fs.copyFileSync(path.join(out,name),path.join(web,name));
 }
 console.log('Stacked mini-program icon generated in four sizes');
})();
