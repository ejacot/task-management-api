(() => {
  let data=null,planOffset=0,historyDate=new Date(),planStartX=null,historyStartX=null;
  const $=selector=>document.querySelector(selector);
  const iso=date=>`${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`;
  const addDays=(date,days)=>{const copy=new Date(date);copy.setDate(copy.getDate()+days);return copy};
  const monday=()=>{const now=new Date(),date=new Date(now);date.setHours(12,0,0,0);date.setDate(now.getDate()-((now.getDay()+6)%7));return date};
  const shortDate=date=>new Intl.DateTimeFormat('ro-RO',{day:'2-digit',month:'short'}).format(date);
  const time=value=>value?value.slice(0,5):'';
  const planName=plan=>plan.kind==='FREE'?'Liber':plan.kind==='VACATION'?'Concediu':plan.kind==='SICK'?'Medical':plan.workType;

  function renderPlan(direction='forward'){
    const start=addDays(monday(),planOffset*7),end=addDays(start,6),today=iso(new Date());
    $('#plan-week-label').textContent=`${shortDate(start)} – ${shortDate(end)} ${end.getFullYear()}`;
    $('#plan-week-title').textContent=planOffset===0?'Săptămâna aceasta':planOffset===1?'Săptămâna viitoare':planOffset===-1?'Săptămâna trecută':'Calendar săptămânal';
    const cells=Array.from({length:7},(_,index)=>{
      const date=addDays(start,index),key=iso(date),plans=data.plans.filter(plan=>plan.date===key);
      return `<article class="detailed-day ${key===today?'today':''}"><header><span>${new Intl.DateTimeFormat('ro-RO',{weekday:'long'}).format(date)}</span><strong>${date.getDate()}</strong></header><div class="day-events">${plans.map(plan=>`<div class="detailed-plan kind-${plan.kind}" style="--event-color:${plan.color}"><strong>${planName(plan)}</strong>${plan.kind==='WORK'?`<span>${time(plan.startTime)}–${time(plan.endTime)}</span>`:''}${plan.notes?`<small>${plan.notes}</small>`:''}</div>`).join('')||'<p class="empty-day">Nimic planificat</p>'}</div></article>`;
    }).join('');
    const board=$('#full-plan');board.className=`weekly-plan-calendar calendar-${direction}`;board.innerHTML=cells;
  }

  function changePlan(delta){planOffset+=delta;renderPlan(delta>0?'forward':'back')}

  function renderHistory(direction='forward'){
    const year=historyDate.getFullYear(),month=historyDate.getMonth(),first=new Date(year,month,1,12),daysInMonth=new Date(year,month+1,0).getDate(),leading=(first.getDay()+6)%7;
    $('#history-month-label').textContent=new Intl.DateTimeFormat('ro-RO',{month:'long',year:'numeric'}).format(first);
    const weekdayHeaders=['Lun','Mar','Mie','Joi','Vin','Sâm','Dum'].map(day=>`<span>${day}</span>`).join('');
    const blanks=Array.from({length:leading},()=>'<div class="month-day outside"></div>').join('');
    const days=Array.from({length:daysInMonth},(_,index)=>{
      const day=index+1,key=`${year}-${String(month+1).padStart(2,'0')}-${String(day).padStart(2,'0')}`,logs=data.logs.filter(log=>log.date===key),hours=logs.reduce((sum,log)=>sum+Number(log.hours),0);
      const events=logs.slice(0,3).map(log=>`<div class="month-log status-${log.status}"><span>${log.workType}</span><b>${Number(log.hours).toLocaleString('ro-RO',{maximumFractionDigits:1})}h</b></div>`).join('');
      const linked=logs.find(log=>log.shiftPlanId&&log.status!=='SUBMITTED'),pending=logs.some(log=>log.shiftPlanId&&log.status==='SUBMITTED');
      return `<article class="month-day ${key===iso(new Date())?'today':''}"><header><strong>${day}</strong>${hours?`<span>${hours.toLocaleString('ro-RO',{maximumFractionDigits:1})} h</span>`:''}</header><div>${events}${logs.length>3?`<small class="more-logs">+${logs.length-3} activități</small>`:''}</div>${pending?'<small class="pending-change">Modificare în așteptare</small>':linked?`<button class="month-correct" data-log-id="${linked.id}">Modifică orele</button>`:''}</article>`;
    }).join('');
    const table=$('#history .table-card');table.className=`card month-calendar-card calendar-${direction}`;table.innerHTML=`<div class="month-weekdays">${weekdayHeaders}</div><div class="month-grid">${blanks}${days}</div>`;
    table.querySelectorAll('[data-log-id]').forEach(button=>button.addEventListener('click',()=>{const log=data.logs.find(item=>item.id===Number(button.dataset.logId));if(log)window.openPlanCorrection?.(log)}));
  }

  function changeHistory(delta){historyDate=new Date(historyDate.getFullYear(),historyDate.getMonth()+delta,1,12);renderHistory(delta>0?'forward':'back')}

  function createControls(){
    const planHead=$('#plan .page-head>div');planHead.querySelector('h1').id='plan-week-title';planHead.querySelector('.muted').textContent='Programul tău, organizat pe săptămâni.';
    $('#plan .page-head').insertAdjacentHTML('beforeend','<div class="calendar-nav"><button id="plan-prev" aria-label="Săptămâna anterioară">‹</button><strong id="plan-week-label"></strong><button id="plan-next" aria-label="Săptămâna următoare">›</button></div>');
    $('#history .table-card').insertAdjacentHTML('beforebegin','<div class="history-calendar-nav"><button id="history-prev" aria-label="Luna anterioară">‹</button><strong id="history-month-label"></strong><button id="history-next" aria-label="Luna următoare">›</button></div>');
    $('#plan-prev').addEventListener('click',()=>changePlan(-1));$('#plan-next').addEventListener('click',()=>changePlan(1));$('#history-prev').addEventListener('click',()=>changeHistory(-1));$('#history-next').addEventListener('click',()=>changeHistory(1));
    const plan=$('#full-plan'),history=$('#history .table-card');plan.addEventListener('pointerdown',event=>planStartX=event.clientX);plan.addEventListener('pointerup',event=>{if(planStartX===null)return;const d=event.clientX-planStartX;planStartX=null;if(d<-55)changePlan(1);if(d>55)changePlan(-1)});history.addEventListener('pointerdown',event=>historyStartX=event.clientX);history.addEventListener('pointerup',event=>{if(historyStartX===null)return;const d=event.clientX-historyStartX;historyStartX=null;if(d<-55)changeHistory(1);if(d>55)changeHistory(-1)});
  }

  window.setupDetailedCalendars=bootstrap=>{data=bootstrap;if(!$('#plan-prev'))createControls();renderPlan();renderHistory()};
})();
