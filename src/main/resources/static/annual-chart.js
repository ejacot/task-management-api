(() => {
  const months = ['Ian','Feb','Mar','Apr','Mai','Iun','Iul','Aug','Sep','Oct','Nov','Dec'];
  const colors = {a:'#747b86', b:'#1768e5'};
  let mode = 'hours';
  let annualData = null;

  const el = id => document.getElementById(id);
  const years = () => [...new Set((annualData?.logs || []).map(log => Number(log.date.slice(0,4))))].sort((a,b) => a-b);
  const totalsFor = year => {
    const result = Array(12).fill(0);
    annualData.logs.filter(log => Number(log.date.slice(0,4)) === year)
      .forEach(log => result[Number(log.date.slice(5,7))-1] += Number(log.hours));
    const rate = mode === 'money' ? Number(annualData.me.hourlyRate || 0) : 1;
    return result.map(value => value * rate);
  };
  const format = (value, signed=false) => {
    const prefix = signed && value > 0 ? '+' : '';
    if (mode === 'money') return prefix + new Intl.NumberFormat('ro-RO',{style:'currency',currency:'EUR',maximumFractionDigits:0}).format(value);
    return prefix + value.toLocaleString('ro-RO',{maximumFractionDigits:1}) + ' h';
  };
  const options = (all, selected) => all.map(year => `<option value="${year}"${year===selected?' selected':''}>${year}</option>`).join('');

  function linePath(values, max, width, height, left, top) {
    const plotW=width-left-30, plotH=height-top-48;
    return values.map((value,index) => `${index?'L':'M'} ${left+(plotW/11)*index} ${top+plotH-(value/max)*plotH}`).join(' ');
  }

  function renderSvg(a, b, yearA, yearB) {
    const width=1200,height=440,left=64,top=28,plotW=width-left-30,plotH=height-top-48;
    const rawMax=Math.max(...a,...b,1), step=mode==='money'?Math.max(500,Math.ceil(rawMax/5/500)*500):Math.max(50,Math.ceil(rawMax/5/50)*50),max=step*5;
    const y=value=>top+plotH-(value/max)*plotH,x=index=>left+(plotW/11)*index;
    const grid=Array.from({length:6},(_,i)=>{const value=step*(5-i),py=top+(plotH/5)*i;return `<line x1="${left}" y1="${py}" x2="${width-30}" y2="${py}" class="grid-line"/><text x="${left-14}" y="${py+5}" class="axis-value">${mode==='money'?'€'+value:value+' h'}</text>`}).join('');
    const monthLabels=months.map((month,i)=>`<text x="${x(i)}" y="${height-12}" class="month-axis">${month}</text>`).join('');
    const series=(values,color,year,offset)=>values.map((value,i)=>value>0?`<circle cx="${x(i)}" cy="${y(value)}" r="6" fill="${color}" class="chart-dot"/><text x="${x(i)}" y="${y(value)+offset}" class="point-label" fill="${color}">${format(value)}</text>`:'').join('');
    const area=`M ${x(0)} ${y(b[0])} ${b.map((value,i)=>`L ${x(i)} ${y(value)}`).join(' ')} L ${x(11)} ${top+plotH} L ${x(0)} ${top+plotH} Z`;
    return `<svg viewBox="0 0 ${width} ${height}" role="img" aria-label="Comparație ${yearA} și ${yearB}"><defs><linearGradient id="comparison-fill" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#1768e5" stop-opacity=".12"/><stop offset="1" stop-color="#1768e5" stop-opacity="0"/></linearGradient></defs>${grid}<path d="${area}" fill="url(#comparison-fill)"/><path d="${linePath(a,max,width,height,left,top)}" class="series-line" stroke="${colors.a}"/><path d="${linePath(b,max,width,height,left,top)}" class="series-line" stroke="${colors.b}"/>${series(a,colors.a,yearA,24)}${series(b,colors.b,yearB,-14)}${monthLabels}</svg>`;
  }

  function render() {
    if (!annualData?.logs || !el('annual-chart')) return;
    const all=years(); if(!all.length)return;
    const selectA=el('annual-year-a'),selectB=el('annual-year-b');
    let yearA=Number(selectA.value),yearB=Number(selectB.value);
    if(!all.includes(yearA))yearA=all.includes(2025)?2025:all[0];
    if(!all.includes(yearB))yearB=all.find(year=>year!==yearA)??yearA;
    selectA.innerHTML=options(all,yearA);selectB.innerHTML=options(all,yearB);
    const a=totalsFor(yearA),b=totalsFor(yearB),totalA=a.reduce((s,v)=>s+v,0),totalB=b.reduce((s,v)=>s+v,0);
    const difference=totalB-totalA,percent=totalA?difference/totalA*100:0,better=b.filter((value,i)=>value>a[i]).length;
    el('annual-title').textContent=mode==='money'?'Comparație venit brut':'Comparație ore lucrate';
    el('comparison-period').textContent=`Ianuarie–Decembrie ${yearA} vs. ${yearB}`;
    el('label-year-a').textContent=`TOTAL ${yearA}`;el('label-year-b').textContent=`TOTAL ${yearB}`;
    el('total-year-a').textContent=format(totalA);el('total-year-b').textContent=format(totalB);
    el('annual-difference').textContent=format(difference,true);el('annual-percent').textContent=(percent>0?'+':'')+percent.toLocaleString('ro-RO',{maximumFractionDigits:1})+'%';
    el('annual-difference').className=difference>=0?'positive':'negative';el('annual-percent').className=difference>=0?'positive':'negative';
    el('annual-chart').innerHTML=renderSvg(a,b,yearA,yearB);
    el('chart-legend').innerHTML=`<span><i style="background:${colors.a}"></i>${yearA}</span><span><i style="background:${colors.b}"></i>${yearB}</span>`;
    el('insight-difference').textContent=format(difference,true);el('insight-percent').textContent=(percent>0?'+':'')+percent.toLocaleString('ro-RO',{maximumFractionDigits:1})+'%';
    el('insight-average').textContent=format(totalB/12);el('insight-better').textContent=`${better} din 12`;
    el('annual-note').classList.toggle('hidden-note',mode==='hours');
  }

  document.addEventListener('DOMContentLoaded',()=>{
    ['annual-year-a','annual-year-b'].forEach(id=>el(id)?.addEventListener('change',render));
    document.querySelectorAll('[data-chart-mode]').forEach(button=>button.addEventListener('click',()=>{mode=button.dataset.chartMode;document.querySelectorAll('[data-chart-mode]').forEach(item=>item.classList.toggle('active',item===button));render()}));
  });
  window.renderAnnualChart=data=>{annualData=data;render()};
})();
