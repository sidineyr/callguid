'use strict';
const { app, BrowserWindow, session, Menu } = require('electron');
const path = require('node:path');
const fs = require('node:fs');
const smoke = process.argv.includes('--smoke-test');
let window;
app.setName('CallGuide');
if(smoke && process.env.CALLGUIDE_SMOKE_DIR) {
  const profile=path.join(process.env.CALLGUIDE_SMOKE_DIR,'profile');fs.mkdirSync(profile,{recursive:true});
  app.setPath('userData',profile);app.setPath('crashDumps',path.join(profile,'crashes'));
}
app.enableSandbox();
app.whenReady().then(async () => {
  session.defaultSession.setPermissionRequestHandler((_contents, _permission, callback) => callback(false));
  session.defaultSession.setPermissionCheckHandler(() => false);
  // Everything is bundled. No renderer network requests or remote navigation are needed.
  session.defaultSession.webRequest.onBeforeRequest((details, callback) => {
    callback({ cancel: !['file:', 'data:', 'devtools:'].some(prefix => details.url.startsWith(prefix)) });
  });
  window = new BrowserWindow({ width:1280, height:900, minWidth:820, minHeight:640,
    show:!smoke, backgroundColor:'#f7f5ed', title:'CallGuide — Ateliê de letras',
    icon:path.join(__dirname,'assets/icon.png'),
    webPreferences:{nodeIntegration:false,contextIsolation:true,sandbox:true,webSecurity:true,backgroundThrottling:!smoke} });
  Menu.setApplicationMenu(Menu.buildFromTemplate([
    ...(process.platform==='darwin' ? [{label:'CallGuide',submenu:[{role:'about'},{type:'separator'},{role:'quit'}]}] : []),
    {label:'Editar',submenu:[{role:'undo'},{role:'redo'},{type:'separator'},{role:'cut'},{role:'copy'},{role:'paste'},{role:'selectAll'}]},
    {label:'Janela',submenu:[{role:'resetZoom'},{role:'zoomIn'},{role:'zoomOut'},{type:'separator'},{role:'togglefullscreen'},{role:'close'}]}
  ]));
  window.webContents.setWindowOpenHandler(() => ({action:'deny'}));
  window.webContents.on('will-navigate',(event,url) => { if(!url.startsWith('file:')) event.preventDefault(); });
  window.webContents.on('render-process-gone',(_event,details) => {
    if(smoke) { console.error(details); app.exit(1); }
  });
  const failures=[];
  window.webContents.on('console-message',event => {
    if(smoke && event.level==='error') failures.push(event.message);
  });
  await window.loadFile(path.join(__dirname,'index.html'));
  if(smoke) {
    const watchdog=setTimeout(()=>{console.error('Smoke test timed out');app.exit(1);},45000);
    try {
      const result=await window.webContents.executeJavaScript('window.callGuideSelfTest()');
      const dir=process.env.CALLGUIDE_SMOKE_DIR;
      if(!dir) throw new Error('CALLGUIDE_SMOKE_DIR is required for smoke testing.');
      fs.mkdirSync(dir,{recursive:true});
      const image=await window.webContents.capturePage();
      fs.writeFileSync(path.join(dir,'CallGuide-desktop.png'),image.toPNG());
      fs.writeFileSync(path.join(dir,'smoke.json'),JSON.stringify({platform:process.platform,arch:process.arch,...result,consoleErrors:failures},null,2));
      clearTimeout(watchdog);
      if(!result.passed || failures.length) app.exit(1); else app.quit();
    } catch(error) { console.error(error); app.exit(1); }
  }
}).catch(error=>{console.error(error);app.exit(1);});
app.on('window-all-closed',()=>app.quit());
