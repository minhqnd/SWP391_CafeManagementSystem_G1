const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const jsp=fs.readFileSync('src/main/webapp/admin-purchases.jsp','utf8');
const source=jsp.match(/<script id="purchase-form-script">([\s\S]*?)<\/script>/)[1];
function setup(selected='') {
    const events={};
    const request={value:selected,addEventListener:(type,fn)=>events.select=fn};
    const add={disabled:false,addEventListener:(type,fn)=>events.add=fn};
    const lines={rows:[{}],replaceChildren(fragment){this.rows=fragment;},
        appendChild(fragment){this.rows.push(...fragment);},querySelectorAll(){return this.rows;},
        addEventListener:(type,fn)=>events.remove=fn};
    const template=ids=>({content:{cloneNode:()=>ids.map(id=>({id}))}});
    const nodes={purchaseRequest:request,purchaseLines:lines,addPurchaseLine:add,
        purchaseLineTemplate:template(['blank']),'purchaseRequest-12':template([7,8])};
    vm.runInNewContext(source,{document:{getElementById:id=>nodes[id]}});
    return {request,add,lines,events};
}
test('linked request loads all ingredients and disables arbitrary added rows',()=>{
    const s=setup();s.request.value='12';s.events.select();
    assert.deepEqual(s.lines.rows.map(r=>r.id),[7,8]);assert.equal(s.add.disabled,true);
    s.events.add();assert.equal(s.lines.rows.length,2);
    assert.match(jsp,/name="ingredientId" value="\$\{it\.ingredientId\}"/);
});
test('unlinked form supports multiple rows but retains at least one and caps at 100',()=>{
    const s=setup();s.events.add();assert.equal(s.lines.rows.length,2);
    const button={closest:()=>({remove:()=>s.lines.rows.pop()})};
    s.events.remove({target:{closest:()=>button}});assert.equal(s.lines.rows.length,1);
    s.events.remove({target:{closest:()=>button}});assert.equal(s.lines.rows.length,1);
    for(let i=0;i<110;i++)s.events.add();assert.equal(s.lines.rows.length,100);
});
test('changing request resets lines and restored selections are reconciled',()=>{
    const s=setup('12');assert.equal(s.lines.rows.length,2);assert.equal(s.add.disabled,true);
    s.request.value='';s.events.select();assert.equal(s.lines.rows.length,1);assert.equal(s.add.disabled,false);
});
