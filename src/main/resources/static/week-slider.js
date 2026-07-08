(() => {
  let activeWeek=0,startX=null,weekData=null;
  const $=selector=>document.querySelector(selector);
  const iso=date=>`${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`;
  const monday=()=>{const now=new Date(),result=new Date(now);result.setHours(12,0,0,0);result.setDate(now.getDate()-((now.getDay()+6)%7));return result};
  const plusDays=(date,days)=>{const result=new Date(date);result.setDate(result.getDate()+days);return result};
  const plannedHours=plan=>{
    if(plan.kind!=='WORK'||!plan.startTime||!plan.endTime)return 0;
    const [sh,sm]=plan.startTime.split(':').map(Number),[eh,em]=plan.endTime.split(':').map(Number),minutes=(eh*60+em)-(sh*60+sm);
    const type=weekData.workTypes.find(item=>item.name===plan.workType);
    const pause=type?.unit==='ROOMS'?0:(type?.defaultBreakMinutes|| (minutes>=510?30:0));
    return Math.max(0,(minutes-pause)/60);
  };

  function render(direction='next'){
    const calendar=$('#employee-calendar'),days=[...calendar.querySelectorAll('.calendar-day')];if(days.length<14)return;
    days.forEach((day,index)=>day.classList.toggle('week-hidden',activeWeek===0?index>=7:index<7));
    calendar.classList.remove('slide-forward','slide-back');void calendar.offsetWidth;calendar.classList.add(direction==='next'?'slide-forward':'slide-back');
    const start=plusDays(monday(),activeWeek*7),end=plusDays(start,6),from=iso(start),to=iso(end);
    $('#visible-week-title').textContent=activeWeek===0?'Săptămâna aceasta':'Săptămâna viitoare';
    $('#visible-week-range').textContent=new Intl.DateTimeFormat('ro-RO',{day:'2-digit',month:'short'}).format(start)+' – '+new Intl.DateTimeFormat('ro-RO',{day:'2-digit',month:'short',year:'numeric'}).format(end);
    const total=weekData.plans.filter(plan=>plan.date>=from&&plan.date<=to).reduce((sum,plan)=>sum+plannedHours(plan),0);
    $('#visible-week-hours').textContent=total.toLocaleString('ro-RO',{maximumFractionDigits:1})+' h';
    $('#previous-week').disabled=activeWeek===0;$('#next-week').disabled=activeWeek===1;
  }

  function changeWeek(next){const target=Math.max(0,Math.min(1,next));if(target===activeWeek)return;const direction=target>activeWeek?'next':'back';activeWeek=target;render(direction)}

  window.setupWeekSlider=data=>{
    weekData=data;const card=$('.calendar-card'),heading=card?.querySelector('.card-head>div:first-child');if(!card||!heading)return;
    heading.querySelector('h2').id='visible-week-title';heading.querySelector('h2').textContent='Săptămâna aceasta';
    let range=heading.querySelector('#visible-week-range');if(!range){range=document.createElement('p');range.id='visible-week-range';range.className='visible-week-range';heading.append(range)}
    const totals=card.querySelector('.week-totals');totals.innerHTML=`<span class="visible-total">Total planificat<strong id="visible-week-hours">0 h</strong></span><div class="week-navigation"><button id="previous-week" type="button" aria-label="Săptămâna anterioară">‹</button><button id="next-week" type="button" aria-label="Săptămâna următoare">›</button></div>`;
    $('#previous-week').addEventListener('click',()=>changeWeek(activeWeek-1));$('#next-week').addEventListener('click',()=>changeWeek(activeWeek+1));
    card.addEventListener('pointerdown',event=>{startX=event.clientX});card.addEventListener('pointerup',event=>{if(startX===null)return;const distance=event.clientX-startX;startX=null;if(distance<-55)changeWeek(1);if(distance>55)changeWeek(0)});
    render();
  };
})();
