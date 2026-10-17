"""Build original polygon icons as SVG + TrueType icon font, using only stdlib."""
from pathlib import Path
import math, struct, json, re, zlib

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'frontend/styles/yunjiang'
OUT.mkdir(parents=True, exist_ok=True)
icons = {}
def icon(name, label, lines):
    contours=[]
    for line in lines:
        # Use four solid ring quarters rather than hundreds of overlapping caps.
        # This keeps circle-heavy glyphs reliable in small-size font rasterizers.
        if len(line)==17 and math.dist(line[0],line[-1])<0.01:
            cx=sum(x for x,y in line[:-1])/16; cy=sum(y for x,y in line[:-1])/16
            radius=math.dist((cx,cy),line[0])
            for quarter in range(4):
                angles=[quarter*math.pi/2+i*math.pi/16 for i in range(9)]
                contours.append([(cx+(radius+1.15)*math.cos(a),cy+(radius+1.15)*math.sin(a)) for a in angles]+[(cx+(radius-1.15)*math.cos(a),cy+(radius-1.15)*math.sin(a)) for a in reversed(angles)])
            continue
        for a,b in zip(line,line[1:]):
            dx,dy=b[0]-a[0],b[1]-a[1]; length=math.hypot(dx,dy)
            if not length: continue
            x,y=-dy/length*1.15,dx/length*1.15
            contours.append([(a[0]+x,a[1]+y),(b[0]+x,b[1]+y),(b[0]-x,b[1]-y),(a[0]-x,a[1]-y)])
        for cx,cy in line:
            contours.append([(cx+1.15*math.cos(i*math.pi/6),cy+1.15*math.sin(i*math.pi/6)) for i in range(12)])
    icons[name]=(label,contours)
def circle(x,y,r): return [(x+r*math.cos(i*math.pi/8),y+r*math.sin(i*math.pi/8)) for i in range(17)]
book=[[(9,17),(20,15),(32,19),(44,15),(55,17),(55,48),(44,46),(32,50),(20,46),(9,48),(9,17)],[(32,19),(32,50)],[(16,25),(25,26)],[(39,26),(48,25)],[(16,33),(25,34)],[(39,34),(48,33)]]
icon('home','同心家园',[[(9,29),(32,10),(55,29)],[(15,26),(15,52),(26,52),(26,38),(38,38),(38,52),(49,52),(49,26)],[(26,20),(32,25),(38,20)]])
icon('study','法典学习',book)
icon('user','个人中心',[circle(32,21,9),[(13,53),(15,44),(22,38),(42,38),(49,44),(51,53)],[(25,46),(32,50),(39,46)]])
icon('unity','同心连接',[circle(32,32,8),circle(32,12,4),circle(13,25,4),circle(20,48,4),circle(44,48,4),circle(51,25,4),[(24,13),(15,18)],[(11,33),(14,42)],[(28,52),(36,52)],[(50,42),(53,33)],[(49,18),(40,13)],[(32,16),(32,24)],[(17,27),(24,30)],[(23,44),(27,39)],[(41,44),(37,39)],[(47,27),(40,30)]])
icon('ai','智能助学',[[(17,18),(47,18),(52,23),(52,46),(47,51),(17,51),(12,46),(12,23),(17,18)],[(32,18),(32,10)],circle(32,8,2),[(22,29),(22,34)],[(42,29),(42,34)],[(23,42),(32,45),(41,42)],[(6,28),(6,40)],[(58,28),(58,40)]])
icon('practice','趣味练习',[[(19,20),(45,20),(50,24),(56,45),(53,50),(47,50),(39,42),(25,42),(17,50),(11,50),(8,45),(14,24),(19,20)],[(18,30),(28,30)],[(23,25),(23,35)],circle(43,28,2),circle(48,34,2)])
icon('exam','普法测验',[[(23,12),(14,12),(14,54),(50,54),(50,12),(41,12)],[(24,9),(40,9),(40,17),(24,17),(24,9)],[(22,30),(27,35),(36,25)],[(23,44),(41,44)]])
icon('note','学习笔记',[[(40,13),(13,13),(13,53),(48,53),(48,36)],[(22,24),(33,24)],[(22,32),(29,32)],[(32,43),(35,33),(49,19),(55,25),(41,39),(32,43)],[(46,22),(52,28)]])
icon('notice','普法公告',[[(12,28),(25,28),(49,17),(49,45),(25,35),(12,35),(12,28)],[(20,36),(24,51),(31,51),(28,38)],[(55,25),(59,22)],[(55,36),(60,39)]])
icon('wrong','错题复习',[[(13,13),(44,13),(44,51),(13,51),(13,13)],[(21,23),(34,23)],[(21,31),(30,31)],circle(45,44,12),[(41,40),(49,48)],[(49,40),(41,48)]])
icon('match','法条匹配',[[(9,15),(28,15),(28,27),(34,27),(34,37),(28,37),(28,50),(9,50),(9,15)],[(37,15),(55,15),(55,50),(37,50),(37,40)],[(15,24),(21,24)],[(43,24),(49,24)]])
icon('quick','快速答题',[[(35,8),(16,35),(29,35),(25,56),(49,27),(35,27),(35,8)]])
icon('scenario','情境学习',[[(9,14),(55,14),(55,45),(38,45),(27,54),(27,45),(9,45),(9,14)],circle(24,26,4),[(17,38),(19,33),(29,33),(31,38)],[(37,25),(47,25)],[(37,33),(45,33)]])
icon('rank','学习排行',[[(10,51),(10,34),(23,34),(23,51)],[(25,51),(25,24),(39,24),(39,51)],[(41,51),(41,39),(54,39),(54,51)],[(7,53),(57,53)],[(27,14),(32,9),(37,14),(32,19),(27,14)]])
icon('medal','学习成就',[circle(32,37,15),[(20,24),(15,9),(25,9),(32,21),(39,9),(49,9),(44,24)],[(32,28),(35,34),(42,35),(37,40),(38,47),(32,44),(26,47),(27,40),(22,35),(29,34),(32,28)]])
icon('review','法务核验',[[(32,8),(51,15),(49,37),(43,47),(32,55),(21,47),(15,37),(13,15),(32,8)],[(22,31),(29,38),(43,24)]])
icon('agents','多智能体协同',[circle(15,17,7),circle(49,17,7),circle(32,47,7),[(22,17),(42,17)],[(18,24),(28,40)],[(46,24),(36,40)],[(29,14),(34,17),(29,20)],[(23,32),(24,38),(30,36)],[(42,31),(37,33),(39,39)]])
icon('trace','证据溯源',[[(12,10),(37,10),(43,16),(43,30)],[(12,10),(12,51),(30,51)],[(20,21),(33,21)],[(20,29),(29,29)],circle(41,41,11),[(49,49),(57,57)],[(36,41),(40,45),(47,37)]])
icon('course','视频课程',[[(10,16),(54,16),(54,49),(10,49),(10,16)],[(27,25),(40,32),(27,40),(27,25)],[(20,55),(44,55)]])

# Polygon contours are shared by SVG and glyf; no dependency on a raster generator.
for name,(label,contours) in icons.items():
    path=' '.join('M'+' L'.join(f'{x:.3f},{y:.3f}' for x,y in c)+' Z' for c in contours)
    (OUT/f'{name}.svg').write_text(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64"><title>{label}</title><path fill="#791817" d="{path}"/></svg>',encoding='utf-8')

u16=lambda n: struct.pack('>H',n)
s16=lambda n: struct.pack('>h',n)
u32=lambda n: struct.pack('>I',n&0xffffffff)
glyphs=[b'']; offsets=[0]; metrics=[]
for label,contours in icons.values():
    pts=[]; ends=[]
    for contour in contours:
        # Clockwise in font coordinate system, including all overlapping stroke shapes.
        points=[(round(x*16),round((60-y)*16)) for x,y in contour]
        area=sum(a[0]*b[1]-b[0]*a[1] for a,b in zip(points,points[1:]+points[:1]))
        if area>0: points.reverse()
        pts.extend(points); ends.append(len(pts)-1)
    xs=[p[0] for p in pts]; ys=[p[1] for p in pts]
    blob=s16(len(contours))+s16(min(xs))+s16(min(ys))+s16(max(xs))+s16(max(ys))+b''.join(u16(e) for e in ends)+u16(0)+bytes([1]*len(pts))
    for axis in (0,1):
        prev=0
        for p in pts: blob+=s16(p[axis]-prev); prev=p[axis]
    blob+=b'\0'*((-len(blob))%4); glyphs.append(blob)
glyf=b''.join(glyphs)
pos=0
for g in glyphs: pos+=len(g); offsets.append(pos)
n=len(glyphs)
head=u32(0x10000)+u32(0x10000)+u32(0)+u32(0x5f0f3cf5)+u16(3)+u16(1024)+b'\0'*16+s16(0)+s16(-100)+s16(1024)+s16(960)+u16(0)+u16(8)+s16(2)+s16(1)+s16(0)
hhea=u32(0x10000)+s16(960)+s16(-64)+s16(0)+u16(1024)+s16(0)+s16(0)+s16(1024)+s16(1)+s16(0)+s16(0)+b'\0'*8+s16(0)+u16(n)
maxp=u32(0x10000)+u16(n)+u16(max(sum(len(c) for c in cs) for _,cs in icons.values()))+u16(max(len(cs) for _,cs in icons.values()))+b'\0'*22
codes=[0xe900+i for i in range(n-1)]+[0xffff]; count=len(codes)
seg=u16(4)+u16(16+8*count)+u16(0)+u16(2*count)+u16(2*(2**int(math.log2(count)) ))+u16(int(math.log2(count)))+u16(2*count-2*(2**int(math.log2(count))))
seg+=b''.join(u16(c) for c in codes)+u16(0)+b''.join(u16(c) for c in codes)+b''.join(u16((i+1-c)&0xffff) for i,c in enumerate(codes[:-1]))+u16(1)+b'\0'*(2*count)
cmap=u16(0)+u16(1)+u16(3)+u16(1)+u32(12)+seg
names=[(1,'Yunjiang Icons'),(2,'Regular'),(3,'YunjiangIcons-1.1'),(4,'Yunjiang Icons'),(5,'Version 1.1'),(6,'YunjiangIcons')]
data=b''; records=b''
for nid,value in names:
    encoded=value.encode('utf-16-be'); records+=u16(3)+u16(1)+u16(0x409)+u16(nid)+u16(len(encoded))+u16(len(data)); data+=encoded
name=u16(0)+u16(len(names))+u16(6+12*len(names))+records+data
os2=u16(0)+s16(1024)+u16(400)+u16(5)+u16(0)+b'\0'*20+s16(0)+b'\0'*10+b'\0'*16+b'YJZF'+u16(64)+u16(codes[0])+u16(codes[-2])+s16(960)+s16(-64)+s16(0)+u16(960)+u16(64)
tables={'head':head,'hhea':hhea,'maxp':maxp,'hmtx':(u16(1024)+s16(0))*n,'glyf':glyf,'loca':b''.join(u32(x) for x in offsets),'cmap':cmap,'name':name,'post':u32(0x30000)+b'\0'*28,'OS/2':os2}
def checksum(b):
    b+=b'\0'*((-len(b))%4)
    return sum(struct.unpack('>'+str(len(b)//4)+'I',b))&0xffffffff
nt=len(tables); p=2**int(math.log2(nt)); header=u32(0x10000)+u16(nt)+u16(p*16)+u16(int(math.log2(nt)))+u16(nt*16-p*16)
body=b''; directory=b''; offset=12+16*nt; headpos=0
for tag,blob in sorted(tables.items()):
    directory+=tag.encode()+u32(checksum(blob))+u32(offset)+u32(len(blob))
    if tag=='head': headpos=offset
    padded=blob+b'\0'*((-len(blob))%4); body+=padded; offset+=len(padded)
font=bytearray(header+directory+body); font[headpos+8:headpos+12]=u32(0xb1b0afba-checksum(bytes(font)))
(OUT/'yunjiang-icons-v2.ttf').write_bytes(font)
# WOFF 1 wrapper around the same TrueType tables for web delivery.
woff_records=b''; woff_body=b''; woff_offset=44+20*nt
for i in range(nt):
    record=12+16*i; tag=bytes(font[record:record+4])
    check,offset,length=struct.unpack_from('>III',font,record+4)
    blob=bytes(font[offset:offset+length]); packed=zlib.compress(blob)
    if len(packed)>=len(blob): packed=blob
    woff_records+=tag+u32(woff_offset)+u32(len(packed))+u32(len(blob))+u32(check)
    padded=packed+b'\0'*((-len(packed))%4);woff_body+=padded;woff_offset+=len(padded)
woff_header=b'wOFF'+bytes(font[:4])+u32(woff_offset)+u16(nt)+u16(0)+u32(len(font))+u16(1)+u16(1)+b'\0'*20
(OUT/'yunjiang-icons-v2.woff').write_bytes(woff_header+woff_records+woff_body)
css='@font-face{font-family:yunjiang;src:url("./yunjiang-icons-v2.ttf") format("truetype");font-weight:normal;font-style:normal;font-display:block;}\n.yj-icon.yj-icon{font-family:yunjiang!important;font-style:normal;font-weight:normal;line-height:1;display:inline-block;}\n'
manifest=[]
for i,(name,(label,_)) in enumerate(icons.items()):
    css+=f'.yj-{name}:before{{content:"\\{0xe900+i:x}";}}\n'
    manifest.append({'name':name,'label':label,'unicode':f'{0xe900+i:x}'})
(OUT/'icons.css').write_text(css,encoding='utf-8')
webcss=css.replace('url("./yunjiang-icons-v2.ttf") format("truetype")','url("./yunjiang-icons-v2.woff") format("woff"),url("./yunjiang-icons-v2.ttf") format("truetype")')
(OUT/'icons.css').write_text(webcss,encoding='utf-8')
import base64
(OUT/'icons.wxss').write_text(css.replace('./yunjiang-icons-v2.ttf','data:font/ttf;base64,'+base64.b64encode(font).decode()),encoding='utf-8')
(OUT/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf-8')
web=ROOT/'src/main/resources/static/console/icons'; web.mkdir(parents=True,exist_ok=True)
for f in OUT.iterdir():
    if f.suffix in ('.svg','.css','.ttf','.woff','.json'): (web/f.name).write_bytes(f.read_bytes())
cards=''.join(f'<article><i class="yj-icon yj-{x["name"]}"></i><b>{x["label"]}</b><small>yj-{x["name"]} · {x["unicode"]}</small></article>' for x in manifest)
(web.parent/'icons.html').write_text('<!doctype html><html lang="zh-CN"><meta charset="utf-8"><title>云疆知法 · 原创图标</title><link rel="stylesheet" href="icons/icons.css"><style>body{background:#faf7eb;color:#791817;font:16px system-ui;margin:40px auto;max-width:1100px}h1{font-size:36px}p{color:#665552}.grid{display:grid;grid-template-columns:repeat(5,1fr);gap:16px}article{background:#fff;border:1px solid #f0d3cc;padding:24px 12px;display:flex;align-items:center;flex-direction:column;gap:16px;border-radius:16px}.yj-icon{font-size:54px}small{font-size:11px;color:#665552}.palette{display:flex;gap:14px;margin:28px 0}.swatch{padding:25px;border-radius:12px}@media(max-width:700px){.grid{grid-template-columns:repeat(3,1fr)}body{margin:20px}}</style><h1>云疆知法 · 同心有据</h1><p>原创线性图标 / 64px网格 / 统一圆角笔画 / SVG 与真实 TrueType 字体</p><div class="palette"><span class="swatch" style="background:#791817;color:white">#791817</span><span class="swatch" style="background:#d07367;color:white">#D07367</span><span class="swatch" style="background:#f0d3cc">#F0D3CC</span><span class="swatch" style="background:#fff">#FAF7EB 背景</span></div><div class="grid">'+cards+'</div></html>',encoding='utf-8')
print(f'Generated {len(icons)} original SVGs and {len(font)}-byte TrueType font')
