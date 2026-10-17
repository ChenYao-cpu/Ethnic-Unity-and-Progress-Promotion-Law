'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');

const root = path.resolve(__dirname, '../..');
const file = path.join(root, 'src/main/resources/static/console/schools.js');
const REGIONS = ['民族院校', '西南地区', '新疆', '华北地区', '东北地区', '华东地区', '华中地区', '华南地区', '西北地区', '单位'];
const ORDER = ['民族院校', '西南地区', '新疆', '华北地区', '东北地区', '华东地区', '华中地区', '华南地区', '西北地区', '单位'];
const REQUIRED = ['西南大学', '西南财经大学', '西南民族大学', '西南科技大学'];

function load(){
  const sandbox = { window: {} };
  vm.runInNewContext(fs.readFileSync(file, 'utf8'), sandbox);
  return sandbox.window.YJ_SCHOOL_UNITS;
}

test('schools.js 导出 220 条以上候选且名称唯一', () => {
  const list = load();
  assert.ok(Array.isArray(list), 'window.YJ_SCHOOL_UNITS 应为数组');
  assert.ok(list.length >= 220, `条目数不足：${list.length}`);
  const seen = new Set();
  for(const item of list){
    assert.equal(seen.has(item.name), false, `名称重复：${item.name}`);
    seen.add(item.name);
  }
  assert.equal(seen.size, list.length);
});

test('每条记录的 name/region/type 非空且取值合法', () => {
  const list = load();
  for(const item of list){
    assert.equal(typeof item.name, 'string');
    assert.equal(typeof item.region, 'string');
    assert.equal(typeof item.type, 'string');
    assert.ok(item.name.length > 0, 'name 不能为空');
    assert.ok(item.region.length > 0, 'region 不能为空');
    assert.ok(item.type.length > 0, 'type 不能为空');
    assert.ok(['高校', '单位'].includes(item.type), `type 非法：${item.type}`);
    assert.ok(REGIONS.includes(item.region), `region 非法：${item.region}`);
  }
});

test('必含西南相关高校，且按“包含”匹配筛选“西南”可命中 6 条以上', () => {
  const list = load();
  const names = list.map(item => item.name);
  for(const name of REQUIRED){
    assert.ok(names.includes(name), `缺少必含项：${name}`);
  }
  const hits = names.filter(name => name.includes('西南'));
  assert.ok(hits.length >= 6, `包含“西南”的名称过少：${hits.length}`);
  // 输入“西南”时下拉里能直接看到西南大学与西南财经大学
  assert.ok(hits.includes('西南大学') && hits.includes('西南财经大学'));
});

test('数组首组为民族院校，分组顺序与条目数符合要求', () => {
  const list = load();
  assert.equal(list[0].region, '民族院校', '首组应为民族院校');
  const firstSeen = [];
  for(const item of list){
    if(!firstSeen.includes(item.region)) firstSeen.push(item.region);
  }
  assert.deepEqual(firstSeen, ORDER, '分组顺序应符合展示顺序');
  assert.ok(list.some(item => item.region === '西南地区'));
  assert.ok(list.some(item => item.region === '新疆'));
  const count = region => list.filter(item => item.region === region).length;
  assert.ok(count('民族院校') >= 10, `民族院校不足：${count('民族院校')}`);
  assert.ok(count('西南地区') >= 30, `西南地区不足：${count('西南地区')}`);
  assert.ok(count('新疆') >= 15, `新疆不足：${count('新疆')}`);
  const others = count('华北地区') + count('东北地区') + count('华东地区') + count('华中地区') + count('华南地区') + count('西北地区');
  assert.ok(others >= 150, `全国其他地区不足：${others}`);
});

test('单位条目数量在 15-25 条之间且排在数组末尾', () => {
  const list = load();
  const units = list.filter(item => item.type === '单位');
  assert.ok(units.length >= 15 && units.length <= 25, `单位条目数异常：${units.length}`);
  for(const unit of units){
    assert.equal(unit.region, '单位');
    assert.ok(/统战部|民族宗教事务委员会|民族事务委员会|司法厅|教育厅/.test(unit.name), `单位名称可疑：${unit.name}`);
  }
  assert.equal(list.at(-1).type, '单位');
});

test('name 无首尾空格、长度不超过 30、不含 < > & 等字符', () => {
  const list = load();
  for(const item of list){
    assert.equal(item.name, item.name.trim(), `name 存在首尾空格：${item.name}`);
    assert.ok(item.name.length <= 30, `name 过长：${item.name}`);
    assert.equal(/[<>&]/.test(item.name), false, `name 含非法字符：${item.name}`);
    assert.equal(/\s{2,}/.test(item.name), false, `name 含连续空格：${item.name}`);
  }
});
