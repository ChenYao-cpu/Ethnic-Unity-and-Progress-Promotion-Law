if(require('fs').existsSync(require('path').join(__dirname,'../frontend/images/brand/approved-brand-board.png'))){const cp=require('child_process');for(const script of ['install-approved-brand.cjs','build-approved-mini-icon.cjs'])cp.execFileSync(process.execPath,[require('path').join(__dirname,script)],{stdio:'inherit'});return;}
const fs=require('fs'),path=require('path');
const sharp=require('C:/Users/ASUS/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');
const root=path.resolve(__dirname,'..'),out=path.join(root,'frontend/images/brand'),web=path.join(root,'src/main/resources/static/console/brand');
(async()=>{
 for(const name of ['logo-web','logo-symbol','logo-horizontal']) {
  await sharp(path.join(out,name+'.svg')).resize({width:name==='logo-horizontal'?1800:1024}).png().toFile(path.join(out,name+'.png'));
  fs.copyFileSync(path.join(out,name+'.png'),path.join(web,name+'.png'));
 }
 for(const size of [1024,512,192,96]) {
  const file=`app-icon-${size}.png`;
  await sharp(path.join(out,'app-icon.svg')).resize(size,size).png().toFile(path.join(out,file));
  fs.copyFileSync(path.join(out,file),path.join(web,file));
 }
 const mark=fs.readFileSync(path.join(out,'logo-web.svg'),'utf8').replace(/<\/?svg[^>]*>/g,''),app=fs.readFileSync(path.join(out,'app-icon.svg'),'utf8').replace(/<\/?svg[^>]*>/g,'');
 const board=`<svg xmlns="http://www.w3.org/2000/svg" width="1400" height="880" viewBox="0 0 1400 880"><rect width="1400" height="880" fill="#f8f4ec"/><rect x="60" y="60" width="650" height="760" rx="28" fill="white"/><rect x="750" y="60" width="590" height="760" rx="28" fill="white"/><g transform="translate(128 155)">${mark}</g><g transform="translate(821 225) scale(.88)">${app}</g><text x="90" y="112" font-family="Arial" font-size="18" fill="#796762">01 / WEB LOGO</text><text x="780" y="112" font-family="Arial" font-size="18" fill="#796762">02 / MINI PROGRAM ICON</text><rect x="90" y="735" width="60" height="30" rx="6" fill="#791817"/><rect x="160" y="735" width="60" height="30" rx="6" fill="#d07367"/><rect x="230" y="735" width="60" height="30" rx="6" fill="#f0d3cc"/><rect x="300" y="735" width="60" height="30" rx="6" fill="#faf7eb"/></svg>`;
 await sharp(Buffer.from(board)).png().toFile(path.join(out,'brand-preview.png'));
 console.log('Rendered transparent PNGs and brand-preview.png');
})().catch(e=>{console.error(e);process.exit(1)});
