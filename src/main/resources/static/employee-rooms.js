(()=>{
 let initialized=false;
 const $=value=>document.querySelector(value);
 const auth=()=>sessionStorage.getItem('roomly.token')?`Bearer ${sessionStorage.getItem('roomly.token')}`:`Basic ${sessionStorage.getItem('roomly.auth')}`;
 const today=()=>{const value=new Date();return `${value.getFullYear()}-${String(value.getMonth()+1).padStart(2,'0')}-${String(value.getDate()).padStart(2,'0')}`};
 async function load(){
  const date=$('#my-rooms-date').value;
  const response=await fetch(`/api/employee/rooms?from=${date}&to=${date}`,{headers:{Authorization:auth()}});
  const rooms=await response.json();
  $('#my-rooms-list').innerHTML=rooms.length?`<div class="room-assignment-list">${rooms.map(room=>`<span class="room-pill status-${room.status}"><strong>${room.roomNumber}</strong><small>${{NORMAL:'Normal',JUNIOR:'Junior Suite',PRESIDENT:'President'}[room.category]||room.category} · ${room.status}</small>${['ASSIGNED','DEFECT'].includes(room.status)?`<button class="small-btn good" data-complete-room="${room.id}">Terminat</button>`:''}</span>`).join('')}</div><p class="muted">${rooms.length} camere repartizate</p>`:'<p class="muted">Managerul nu ți-a repartizat camere pentru această zi.</p>';
 }
 window.setupEmployeeRooms=data=>{
  if(data.me.role!=='EMPLOYEE'||initialized)return;
  initialized=true;
  $('.sidebar nav').insertAdjacentHTML('beforeend','<button class="nav employee-rooms-nav"><i>▦</i>Camerele mele</button>');
  $('.main').insertAdjacentHTML('beforeend',`<section id="employee-rooms-view" class="view"><div class="page-head"><div><p class="kicker">LISTA ZILEI</p><h1>Camerele mele</h1><p class="muted">Lista repartizată de manager pentru ziua selectată.</p></div><input id="my-rooms-date" type="date" value="${today()}"></div><section class="card"><div id="my-rooms-list"></div></section></section>`);
  $('.employee-rooms-nav').onclick=()=>{document.querySelectorAll('.view').forEach(view=>view.classList.toggle('active-view',view.id==='employee-rooms-view'));document.querySelectorAll('.nav').forEach(item=>item.classList.toggle('active',item.classList.contains('employee-rooms-nav')));load()};
  $('#my-rooms-date').onchange=load;
  $('#my-rooms-list').onclick=async event=>{const id=event.target.dataset.completeRoom;if(!id)return;await fetch(`/api/employee/rooms/${id}/complete`,{method:'PUT',headers:{'Content-Type':'application/json',Authorization:auth()},body:JSON.stringify({notes:null})});load()};
 };
})();
