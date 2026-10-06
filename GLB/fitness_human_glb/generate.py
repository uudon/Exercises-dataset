import json, math, struct, pathlib, zipfile
P=pathlib.Path(__file__).parent
mapping={}
class Model:
 def __init__(self,sex):
  self.sex=sex;self.parts=[]
 def tube(self,name,cn,rings,color,region=None):
  # Each ring: center xyz, lateral radius, depth radius; Y-up, front +Z.
  n=32;v=[]
  for x,y,z,rx,rz in rings:
   for j in range(n):
    a=2*math.pi*j/n;v.append([x+rx*math.cos(a),y,z+rz*math.sin(a)])
  f=[]
  for i in range(len(rings)-1):
   for j in range(n):
    a=i*n+j;b=i*n+(j+1)%n;c=b+n;d=a+n;f.extend([[a,b,c],[a,c,d]])
  v.extend([list(rings[0][:3]),list(rings[-1][:3])]);lo=len(v)-2;hi=len(v)-1
  for j in range(n):f.extend([[lo,(j+1)%n,j],[hi,(len(rings)-1)*n+j,(len(rings)-1)*n+(j+1)%n]])
  # Keep the generator standard-library-only so it remains reproducible on
  # the offline build host.
  norms=[[0.0,0.0,0.0] for _ in v]
  for tri in f:
   a,b,c=(v[i] for i in tri)
   ab=[b[k]-a[k] for k in range(3)];ac=[c[k]-a[k] for k in range(3)]
   nn=[ab[1]*ac[2]-ab[2]*ac[1],ab[2]*ac[0]-ab[0]*ac[2],ab[0]*ac[1]-ab[1]*ac[0]]
   for k in tri:
    norms[k]=[norms[k][axis]+nn[axis] for axis in range(3)]
  for i,norm in enumerate(norms):
   length=max(math.sqrt(sum(value*value for value in norm)),1e-8)
   norms[i]=[-value/length for value in norm]
  f=[[tri[0],tri[2],tri[1]] for tri in f]
  # Orient outward; accumulate smooth vertex normals.
  rid=region or name;mapping[name]={'region_id':rid,'name_zh':cn,'selectable':region is not None}
  self.parts.append((name,cn,rid,v,norms,f,color,region is not None))
 def ell(self,name,cn,c,s,col,region=None):
  rings=[]
  for i in range(19):
   a=-math.pi/2+.015+(math.pi-.03)*i/18
   rings.append((c[0],c[1]+s[1]*math.sin(a),c[2],s[0]*math.cos(a),s[2]*math.cos(a)))
  self.tube(name,cn,rings,col,region)
 def export(self):
  g={'asset':{'version':'2.0','generator':'Original procedural fitness prototype'},'scene':0,'scenes':[{'nodes':[]}],'nodes':[],'meshes':[],'materials':[],'buffers':[{}],'bufferViews':[],'accessors':[]};blob=bytearray()
  def acc(arr,typ,ct,target):
   while len(blob)%4:blob.append(0)
   if typ=='VEC3':
    packed=b''.join(struct.pack('<3f',*row) for row in arr);mins=[min(row[i] for row in arr) for i in range(3)];maxs=[max(row[i] for row in arr) for i in range(3)]
   else:
    packed=struct.pack('<%dI'%len(arr),*arr);mins=maxs=None
   off=len(blob);blob.extend(packed);vi=len(g['bufferViews']);g['bufferViews'].append({'buffer':0,'byteOffset':off,'byteLength':len(packed),'target':target});a={'bufferView':vi,'componentType':ct,'count':len(arr),'type':typ}
   if typ=='VEC3':a.update(min=mins,max=maxs)
   g['accessors'].append(a);return len(g['accessors'])-1
  for name,cn,rid,v,n,f,col,sel in self.parts:
   pi=acc(v,'VEC3',5126,34962);ni=acc(n,'VEC3',5126,34962);ii=acc([index for tri in f for index in tri],'SCALAR',5125,34963);mi=len(g['materials']);g['materials'].append({'name':name+'_material','pbrMetallicRoughness':{'baseColorFactor':col,'metallicFactor':0,'roughnessFactor':.8},'alphaMode':'OPAQUE','doubleSided':False});idx=len(g['nodes']);g['meshes'].append({'name':name,'primitives':[{'attributes':{'POSITION':pi,'NORMAL':ni},'indices':ii,'material':mi}]});g['nodes'].append({'name':name,'mesh':idx,'extras':{'region_id':rid,'name_zh':cn,'selectable':sel}});g['scenes'][0]['nodes'].append(idx)
  g['buffers'][0]['byteLength']=len(blob);j=json.dumps(g,ensure_ascii=False,separators=(',',':')).encode();j+=b' '*((-len(j))%4);blob+=b'\0'*((-len(blob))%4);out=struct.pack('<III',0x46546c67,2,12+8+len(j)+8+len(blob))+struct.pack('<II',len(j),0x4e4f534a)+j+struct.pack('<II',len(blob),0x004e4942)+blob;(P/(self.sex+'.glb')).write_bytes(out)
  return {'file':self.sex+'.glb','bytes':len(out),'triangles':sum(len(x[5]) for x in self.parts),'meshes':len(self.parts),'selectableMeshes':sum(1 for x in self.parts if x[7])}
def make(sex):
 m=Model(sex);female=sex=='female';skin=[.69,.46,.32,1];cloth=[.045,.09,.15,1] if not female else [.16,.07,.22,1];w=.94 if female else 1
 # torso split into front/back so region picking can distinguish chest and back
 # entire thorax is back region; front patches provide chest selection
 m.tube('torso','背部',[(0,1.01,0,.145*w,.085),(0,1.10,0,.125*w,.079),(0,1.22,0,.14*w,.09),(0,1.35,0,.19*w,.103),(0,1.43,0,.19*w,.088),(0,1.46,0,.09,.068)],skin,'back')
 m.ell('abs','腹部',(0,1.13,.072),(.115,.12,.025),skin,'abs')
 for side,sg in [('left',1),('right',-1)]:
  m.ell('chest_'+side,'胸部',(sg*.085,1.32,.086),(.089,.093,.037 if not female else .061),skin,'chest')
 m.tube('neck','颈部',[(0,1.44,0,.055,.052),(0,1.55,0,.047,.047)],skin)
 m.ell('head','头部',(0,1.64,.006),(.079,.108,.076),skin)
 m.ell('nose','鼻部',(0,1.645,.079),(.014,.024,.016),skin)
 for sg in [-1,1]:
  m.ell('ear_'+str(sg),'耳部',(sg*.078,1.65,0),(.012,.023,.014),skin)
  m.ell('eye_'+str(sg),'眼睛',(sg*.028,1.673,.075),(.008,.004,.002),[.08,.065,.05,1])
 m.tube('briefs','运动内裤',[(0,.90,0,.16 if not female else .177,.096),(0,.97,0,.169 if not female else .182,.111),(0,1.045,0,.147 if not female else .157,.098)],cloth)
 for side,sg in [('left',1),('right',-1)]:
  x=sg*(.083 if not female else .095)
  # The front and rear thigh regions are separate meshes so ray hits can
  # distinguish quadriceps from hamstrings without name-based inference.
  m.tube('quadriceps_'+side,'股四头肌',[(x,.48,.031,.045,.038),(x,.61,.037,.058,.049),(x,.78,.031,.071,.061),(x,.94,.024,.067,.064)],skin,'quadriceps')
  m.tube('hamstrings_'+side,'腘绳肌',[(x,.48,-.031,.045,.038),(x,.61,-.037,.058,.049),(x,.78,-.031,.071,.061),(x,.94,-.024,.067,.064)],skin,'hamstrings')
  m.ell('knee_'+side,'膝部',(x,.48,.008),(.051,.049,.054),skin)
  m.tube('calves_'+side,'小腿',[(x,.09,0,.031,.031),(x,.2,0,.04,.043),(x,.32,-.012,.052,.057),(x,.44,0,.043,.046)],skin,'calves')
  m.ell('foot_'+side,'足部',(x,.052,.047),(.045,.045,.096),skin)
  m.ell('shoulders_'+side,'肩部',(sg*.195,1.385,0),(.064,.075,.065),skin,'shoulders')
  # Biceps and triceps deliberately use different geometry and front/back
  # centers. They are not aliases for one generic upper-arm node.
  m.tube('biceps_'+side,'肱二头肌',[(sg*.273,1.115,.026,.026,.031),(sg*.245,1.23,.032,.034,.039),(sg*.204,1.385,.025,.039,.043)],skin,'biceps')
  m.tube('triceps_'+side,'肱三头肌',[(sg*.273,1.115,-.026,.026,.031),(sg*.245,1.23,-.032,.034,.039),(sg*.204,1.385,-.025,.039,.043)],skin,'triceps')
  m.ell('elbow_'+side,'肘部',(sg*.276,1.105,0),(.038,.037,.039),skin)
  m.tube('forearms_'+side,'前臂',[(sg*.332,.865,0,.026,.025),(sg*.315,.97,0,.035,.039),(sg*.278,1.105,0,.036,.039)],skin,'forearms')
  m.ell('hand_'+side,'手部',(sg*.34,.815,.005),(.031,.066,.023),skin)
  m.ell('glutes_'+side,'臀部',(sg*.084,.985,-.078),(.081,.084,.038),cloth,'glutes')
 if female:
  m.tube('sports_bra_band','运动内衣围带',[(0,1.25,0,.155,.101),(0,1.30,0,.177,.113)],cloth)
  for side,sg in [('left',1),('right',-1)]:
   m.ell('sports_bra_'+side,'胸部',(sg*.084,1.328,.093),(.094,.095,.062),cloth,'chest')
   m.tube('bra_strap_'+side,'运动内衣肩带',[(sg*.11,1.31,-.006,.018,.108),(sg*.11,1.40,-.006,.018,.093),(sg*.11,1.445,-.006,.018,.077)],cloth)
 return m.export()
stats=[make('male'),make('female')]
(P/'muscle_regions.json').write_text(json.dumps(mapping,ensure_ascii=False,indent=2))
(P/'README.md').write_text('''# 健身人体 GLB 原型

男女独立 GLB，单位米，Y 向上、正面 +Z，原点位于足底附近。自然比例 A 姿势，成年人体风格。不透明实体材质，无外部贴图和网络资源。程序化原创几何，无第三方资源；本交付原创内容可修改、商用及随 App 分发。

这是简化的人体交互原型，不是写实角色或医学解剖模型。身体使用多个重叠曲面，关节与服装处可能有接缝；没有骨骼绑定、动画、真实手指及精细肌肉形态。臀部区域显示在服装表面。身体分区仅为训练部位示意，不用于医学判断。

## 可选择区域

每个模型都包含以下 11 个共享逻辑 `region_id`，且每个左右节点都是独立 mesh：`chest`、`shoulders`、`back`、`biceps`、`triceps`、`forearms`、`abs`、`glutes`、`quadriceps`、`hamstrings`、`calves`。其中肱二头肌/肱三头肌和股四头肌/腘绳肌分别使用独立几何，不通过重命名通用上臂或大腿节点实现。

## 接入
分别加载 male.glb / female.glb 实现性别切换。GLB 本身不提供手势、按钮或点击回调：需由 App 的 3D 引擎实现轨道相机、缩放限制、重置视角、射线拾取和材质高亮。推荐初始相机 (0, 1.0, 3.0)，观察目标 (0, 0.9, 0)。

节点名称、extras.region_id、extras.name_zh、extras.selectable 与 muscle_regions.json 对应。仅高亮 selectable=true 的节点。相同 region_id 的左右区域可一起高亮。胸部有衣物时应拾取最外层 sports_bra 节点。分区是近似区域，背部节点包括胸廓主体，侧面也可命中。恢复高亮时还原原始材质，避免修改共享材质影响其他区域。

将 GLB 和映射 JSON 放入 Android assets 目录即可打包离线资源；具体渲染引擎的离线依赖也需随 App 打包。无需下载模型。没有提供 App 源码或已验证的 Android 集成。

## 重新生成
使用 Python 3 运行 generate.py。文件不包含压缩扩展，使用 glTF 2.0 核心格式。
''')
required_regions={'chest','shoulders','back','biceps','triceps','forearms','abs','glutes','quadriceps','hamstrings','calves'}
for st in stats:
 st['requiredRegions']=sorted(required_regions)
(P/'stats.json').write_text(json.dumps(stats,indent=2,ensure_ascii=False))
# Validate GLB structural consistency and buffer bounds.
for st in stats:
 b=(P/st['file']).read_bytes();magic,version,length=struct.unpack_from('<III',b);assert magic==0x46546c67 and version==2 and length==len(b);jl=struct.unpack_from('<I',b,12)[0];g=json.loads(b[20:20+jl]);assert all(v['byteOffset']+v['byteLength']<=g['buffers'][0]['byteLength'] for v in g['bufferViews']);assert len(g['nodes'])==st['meshes']
 regions={node['extras'].get('region_id') for node in g['nodes'] if node.get('extras',{}).get('selectable')}
 assert regions==required_regions, (st['file'], sorted(regions))
with zipfile.ZipFile(P.parent/'fitness_human_glb.zip','w',zipfile.ZIP_DEFLATED) as z:
 for p in P.iterdir():z.write(p,p.name)
print(json.dumps(stats))
