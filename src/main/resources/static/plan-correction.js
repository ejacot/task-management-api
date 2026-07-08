(() => {
  let selectedLog=null;
  const $=selector=>document.querySelector(selector);

  function ensureDialog(){
    if($('#correction-dialog'))return;
    document.body.insertAdjacentHTML('beforeend',`<dialog id="correction-dialog"><form id="correction-form"><div class="dialog-head"><div><p class="kicker">MODIFICARE FAȚĂ DE PLAN</p><h2>Corectează orele lucrate</h2></div><button type="button" id="close-correction">×</button></div><div class="planned-reference"><span>Planul managerului</span><strong id="correction-plan"></strong></div><div class="field-grid"><div><label>Am început la</label><input id="correction-start" type="time" required></div><div><label>Am terminat la</label><input id="correction-end" type="time" required></div><div><label>Pauză</label><select id="correction-break"><option value="0">Fără pauză</option><option value="30">30 minute</option><option value="45">45 minute</option></select></div></div><label>De ce diferă față de plan?</label><textarea id="correction-reason" rows="3" maxlength="500" required placeholder="De exemplu: am lucrat o oră în plus"></textarea><p class="correction-help">Doar modificarea va fi trimisă managerului. Dacă nu modifici planul, pontajul rămâne aprobat automat.</p><p id="correction-error" class="error"></p><div class="dialog-actions"><button type="button" id="cancel-correction" class="btn ghost">Renunță</button><button type="submit" class="btn primary">Trimite modificarea</button></div></form></dialog>`);
    $('#close-correction').addEventListener('click',()=>$('#correction-dialog').close());
    $('#cancel-correction').addEventListener('click',()=>$('#correction-dialog').close());
    $('#correction-form').addEventListener('submit',submitCorrection);
  }

  function openCorrection(log){
    selectedLog=log;$('#correction-error').textContent='';
    $('#correction-plan').textContent=`${log.workType} · ${log.date} · ${log.startTime?.slice(0,5)}–${log.endTime?.slice(0,5)}`;
    $('#correction-start').value=log.startTime?.slice(0,5)||'';$('#correction-end').value=log.endTime?.slice(0,5)||'';$('#correction-break').value=String(log.breakMinutes||0);$('#correction-reason').value='';
    $('#correction-dialog').showModal();
  }

  async function submitCorrection(event){
    event.preventDefault();const auth=sessionStorage.getItem('roomly.auth');
    const response=await fetch(`/api/hotel/logs/${selectedLog.id}/correction`,{method:'PUT',headers:{'Content-Type':'application/json',Authorization:`Basic ${auth}`},body:JSON.stringify({startTime:$('#correction-start').value,endTime:$('#correction-end').value,breakMinutes:Number($('#correction-break').value),reason:$('#correction-reason').value.trim()})});
    if(!response.ok){let body={};try{body=await response.json()}catch{}$('#correction-error').textContent=body.message||'Modificarea nu a putut fi trimisă.';return;}
    $('#correction-dialog').close();location.reload();
  }

  window.setupPlanCorrections=data=>{
    if(data.me.role!=='EMPLOYEE')return;ensureDialog();
    const rows=[...document.querySelectorAll('#history-list .history-row')];
    data.logs.forEach((log,index)=>{
      if(!log.shiftPlanId||!rows[index])return;const statusCell=rows[index].children[4];if(!statusCell)return;
      if(log.status==='SUBMITTED'){const pending=document.createElement('small');pending.className='plan-change-pending';pending.textContent='Modificare în așteptare';statusCell.append(pending);return;}
      const button=document.createElement('button');button.type='button';button.className='correct-plan-btn';button.textContent='Modifică orele';button.addEventListener('click',()=>openCorrection(log));statusCell.append(button);
    });
  };
})();
