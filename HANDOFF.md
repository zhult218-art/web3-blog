# web3-blog é¡¹ç›®äº¤æ¥æ–‡æ¡£ï¼ˆHANDOFFï¼‰

> æ›´æ–°æ—¥æœŸï¼š2026-08-11ï¼ˆå«ç¤¾åŒºåŠŸèƒ½å¼€å‘è½®æ¬¡ï¼‰
> ç”¨é€”ï¼šè·¨ä¼šè¯æ— ç¼æ‰¿æ¥ã€‚token è¶…é™å¼€å¯æ–°ä¼šè¯æ—¶ï¼Œå…ˆè¯»å–æœ¬æ–‡ä»¶ï¼Œå¤è¿°ç†è§£å¹¶æŒ‡å‡ºæ­§ä¹‰ï¼Œç”¨æˆ·ç¡®è®¤åå†æ‰§è¡Œã€‚
> è§„åˆ™ï¼šè‹¥ä¿¡æ¯å†²çªï¼Œä»¥æœ¬æ–‡ä»¶ä¸­ã€Œæœ€åä¸€æ¬¡æ˜ç¡®ç¡®è®¤ã€çš„å†…å®¹ä¸ºå‡†ã€‚

---

## 1. æœ€ç»ˆç›®æ ‡

ä¿®å¤é¦–é¡µé‡æ„åç”¨æˆ·çš„ä¸‰ä¸ªé—®é¢˜ï¼šâ‘  ç¬¬ä¸€å± 3D æ˜Ÿçƒä¸æ˜¾ç¤ºï¼›â‘¡ æ˜Ÿé€” AI åŠ©æ‰‹ç‚¹å‡»æ— ååº”ï¼›â‘¢ SPA è·¯ç”±è·³è½¬éœ€åˆ·æ–°ã€‚
åŒæ—¶ä¿è¯ï¼šé¦–é¡µç¬¬äºŒå±è‡³é¡µè„šçš„èµ›åšè§†è§‰ï¼ˆTechMatrix / ShowcaseGrid / ArticleCards / GlowCursor / Footerï¼‰å®Œæ•´ï¼Œæ˜Ÿé€”æ˜Ÿçƒï¼ˆå…¨æ¯ 3D orb è¯­éŸ³åŠ©æ‰‹ï¼‰å®Œæ•´ä¿ç•™ä¸”åŠŸèƒ½æ­£å¸¸ã€‚

## 2. å…³é”®èƒŒæ™¯

- é¡¹ç›®ï¼š`F:\project\my-blog\web3-blog`ï¼ˆgit repoï¼Œä½† **master æ— ä»»ä½•æäº¤å†å²**ï¼Œæ— æ³• git diff å›æ»šï¼‰
- ä¸»å‰ç«¯ï¼š`frontend/`ï¼ŒVue 3 + Viteï¼ˆJavaScriptï¼Œé TSï¼‰ï¼›ç»„ä»¶åº“åŸºäº scoped CSS
- é¦–é¡µé‡æ„ï¼š`frontend/src/components/home/` ä¸‹æ–°å¢ `TechMatrix.vue` / `ShowcaseGrid.vue` / `ArticleCards.vue` / `GlowCursor.vue` / `SectionHead.vue`ï¼›`GhibliHome.vue` é‡å†™ï¼ˆhero ä¿ç•™ã€3D æ»šåŠ¨è”åŠ¨ã€bg-field ç²’å­å±‚ã€æç®€ Footerï¼‰
- æ˜Ÿé€” AI åŠ©æ‰‹ï¼š`frontend/src/components/common/XingTuAssistant.vue`ï¼ˆçº¦ 715 è¡Œï¼‰ï¼Œå…¨æ¯è¯­éŸ³åŠ©æ‰‹ï¼Œ`App.vue:8` å…¨å±€æŒ‚è½½ï¼Œå³ä¸‹è§’ fixed `z-index:9999` çš„ orb æŒ‰é’® + å¯å±•å¼€é¢æ¿ï¼›å«è¯­éŸ³è¯†åˆ«/åˆæˆã€å¿«æ·å‘½ä»¤ã€æ¬¢è¿è¯­ã€åç«¯ä¼šè¯
- `city/` ä¸ºä»“åº“å†…å¦ä¸€å­åº”ç”¨ï¼ˆ`src/main.tsx`ï¼ŒReact + @react-three/fiber/dreiï¼‰ï¼Œä¼šå¹²æ‰° Vite é¢„æ„å»ºæ‰«æ
- ç¯å¢ƒï¼šWindows (win32)ï¼ŒPowerShell 5.1ï¼ˆä¸æ”¯æŒ `&&`ï¼Œç”¨ `;` æˆ– `if ($?)`ï¼‰
- æµ‹è¯•æ–¹å¼ï¼špuppeteer-core + Edge headlessï¼ˆ`C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe`ï¼‰ï¼ŒçœŸå® CDP ç‚¹å‡»éªŒè¯ï¼›æµ‹è¯•è„šæœ¬æ”¾ `C:\Users\zhult\AppData\Local\Temp\opencode\`ï¼ˆå‹¿å†™å…¥é¡¹ç›®ï¼‰
- æ¨¡å‹æ— å›¾åƒè¾“å…¥èƒ½åŠ›ï¼Œç”¨æˆ·è´´å›¾éœ€é æ–‡å­—/question å·¥å…·ç¡®è®¤
- æœåŠ¡çŠ¶æ€ï¼šdev 5173ï¼ˆ`npm run dev`ï¼Œvite --hostï¼Œæ—¥å¿— `frontend/vite-dev.log`ï¼‰ï¼›preview 4173ï¼ˆ`npm run preview -- --port 4173`ï¼Œæ—¥å¿— `frontend/vite-preview.log`ï¼‰ï¼Œä¸¤è€…å‡åŸºäº `frontend/` ç›®å½•

## 3. å·²ç¡®è®¤çš„äº‹å®ï¼ˆæœ€åä¸€æ¬¡ç¡®è®¤ä¸ºå‡†ï¼‰

1. **ä¸‰é¡¹é—®é¢˜åŒä¸€æ ¹å› **ï¼š`ShowcaseGrid.vue` æŠŠ `setInterval` è¿”å›å€¼ï¼ˆnumberï¼‰èµ‹ `_card` å±æ€§ï¼ŒESM ä¸¥æ ¼æ¨¡å¼æŠ› `TypeError: Cannot create property '_card' on number '9'` â†’ mounted é’©å­æŠ¥é”™ + Vue Router warnã€Œuncaught error during route navigationã€â†’ SPA è·³è½¬éœ€åˆ·æ–°ã€é¡µé¢æŒ‚è½½é“¾è·¯å¼‚å¸¸
2. timers æ”¹ Map åï¼šbuild é€šè¿‡ã€dev æ— é”™è¯¯ã€SPA è·³è½¬æ­£å¸¸ï¼ˆçœŸå®ç‚¹å‡»ã€Œç¤¾åŒº/å•†åŸ/åª’ä½“ã€â†’ `/community` `/shop` `/media` å³æ—¶æ¸²æŸ“ï¼Œæ— åˆ·æ–°æ— æŠ¥é”™ï¼‰
3. **3D æ˜Ÿçƒæ¸²æŸ“æ­£å¸¸**ï¼šhero canvas åœ¨ rAF å¸§å†… readPixels éé›¶åƒç´  **20.4%**ï¼›æ­¤å‰æµ‹å‡º 0% æ˜¯ `preserveDrawingBuffer:false` çš„æ¢æµ‹å‡è±¡ï¼ˆæµè§ˆå™¨åˆæˆåæ¸…ç©ºå¸§ç¼“å†²ï¼‰ï¼Œä¸æ˜¯æ¸²æŸ“é—®é¢˜
4. **æ˜Ÿé€”ç‚¹å‡»åœ¨ dev(5173) ä¸ preview(4173) å‡æ­£å¸¸**ï¼ˆæ­¤å‰åˆ¤å®šã€Œdev ç‚¹å‡»æ— ååº”ã€æ˜¯è¯¯åˆ¤ï¼‰ï¼šwelcome é€»è¾‘ä¼šåœ¨é¦–æ¬¡è®¿é—®ï¼ˆlocalStorage æ—  visited æ ‡è®°ï¼‰2 ç§’åè‡ªåŠ¨æ‰“å¼€é¢æ¿ï¼Œæµ‹è¯•ç‚¹å‡»æŠŠå®ƒ toggle å…³é—­è¢«è¯¯è¯»ä¸ºã€Œæ— ååº”ã€
5. æœ€ç»ˆç‚¹å‡»æ–¹æ¡ˆï¼šorb **æ— å†…è” @click**ï¼Œç”± setup é¡¶å±‚ `document.addEventListener('click', onOrbClick, true)`ï¼ˆcaptureï¼‰å¤„ç†ï¼Œ`onBeforeUnmount` ä»¥ `removeEventListener(..., true)` å¯¹åº”æ¸…ç†ï¼ˆ`XingTuAssistant.vue:151-160` å®šä¹‰/æ³¨å†Œï¼Œ`457` æ¸…ç†ï¼‰
6. é•¿æ—¶åºéªŒè¯ï¼ˆdev ä¸ preview ä¸€è‡´ï¼‰ï¼št+2s é¢æ¿è‡ªåŠ¨ open â†’ ä¸‰æ¬¡çœŸå®ç‚¹å‡» openâ†’closeâ†’openâ†’close å…¨éƒ¨æ­£å¸¸
7. `vite.config.js` å·²åŠ  `optimizeDeps: { entries: ['./index.html'] }`ï¼Œå¦åˆ™é¢„æ„å»ºè¯¯æ‰« `city/src/main.tsx` ç¼º react-dom/@react-three/fiber/drei æŠ¥é”™
8. dev é‡å¯ + åˆ é™¤é‡å»º `node_modules/.vite` åä¸€åˆ‡æ­£å¸¸ï¼›æœ€è¿‘ä¸€æ¬¡ `npm run build` é€šè¿‡ï¼ˆ13.74sï¼›>500kB chunk è­¦å‘Šä»…æç¤ºéé”™è¯¯ï¼‰
9. é¡¹ç›®æ—  git æäº¤å†å²ï¼ˆmaster æ—  commitsï¼‰

## 4. é•¿æœŸåå¥½

- ä¸­æ–‡äº¤æµï¼Œç®€æ´ç›´æ¥ï¼Œå…³é”®ç»“è®ºæ”¾æœ€å‰
- ä»£ç å¼•ç”¨ç”¨ `file_path:line` æ ¼å¼
- æ¯ä¸ªä¿®å¤å¿…é¡»éªŒè¯ï¼šbuild é€šè¿‡ + Edge headless çœŸå®ç‚¹å‡»/è¡Œä¸ºæµ‹è¯•
- æ ¹å› è¦æŒ–åˆ°åº•ï¼Œä¸æ¥å—è¡¨é¢ä¿®å¤
- æµ‹è¯•è„šæœ¬åªæ”¾ç³»ç»Ÿä¸´æ—¶ç›®å½•ï¼Œä¸æ±¡æŸ“é¡¹ç›®

## 5. ç¡¬æ€§è§„åˆ™

- ä¸å¾—åˆ é™¤æˆ–é‡æ„æ˜Ÿé€”æ˜Ÿçƒ/åŠ©æ‰‹ç»„ä»¶ï¼ˆç”¨æˆ·å®ç‰©è¯ä»£ç ï¼Œ`z-index:9999`ï¼‰
- ä¸å¾—éšæ„æ”¹åŠ¨ `App.vue:8` çš„æ˜Ÿé€”æŒ‚è½½æ–¹å¼
- ä¸å¾—åœ¨ä»£ç ä¸­åŠ æ³¨é‡Šï¼ˆé™¤éç”¨æˆ·è¦æ±‚ï¼‰
- ä¸åœ¨é¡¹ç›®é‡Œç•™ä¸´æ—¶æµ‹è¯•æ–‡ä»¶
- ä¸éšæ„ä¿®æ”¹ `vite.config.js` çš„ optimizeDeps é…ç½®
- ä¿¡æ¯å†²çªæ—¶ä»¥æœ€åä¸€æ¬¡æ˜ç¡®ç¡®è®¤å†…å®¹ä¸ºå‡†

## 6. è¾“å‡ºæ ¼å¼ï¼ˆå¯¹ç”¨æˆ·å›å¤ï¼‰

- ç®€æ´ä¸­æ–‡ï¼›ç»“è®ºå…ˆè¡Œï¼›éªŒè¯ç»“æœç”¨å¯è¯»æ ‡ç­¾/åˆ—è¡¨å‘ˆç°
- é•¿åº¦é€‚ä¸­ï¼Œé¿å…å†—é•¿è§£é‡Š

## 7. å·²å®Œæˆçš„å·¥ä½œ

1. `ShowcaseGrid.vue` timers é‡æ„ï¼šå¯¹è±¡å±æ€§æ”¹ä¸º `Map`ï¼ˆ`timers.set(i, tid)` / `clearInterval(tid)` / `timers.delete(i)`ï¼‰â€”â€”ä¿®å¤æ ¹å› 
2. `vite.config.js` å¢åŠ  `optimizeDeps.entries` é™åˆ¶
3. dev server é‡å¯ + `node_modules/.vite` é¢„æ„å»ºç¼“å­˜é‡å»º
4. é¦–é¡µé‡æ„ï¼ˆæ—©æœŸä¼šè¯å®Œæˆï¼‰ï¼š`GhibliHome.vue` é‡å†™ + home ç»„ä»¶äº”ä¸ªï¼ˆè§ç¬¬ 2 èŠ‚ï¼‰
5. æ˜Ÿé€” orb ç‚¹å‡»æ”¹é€ ï¼šç§»é™¤å†…è” `@click="togglePanel"`ï¼Œæ”¹ setup é¡¶å±‚ document capture å§”æ‰˜ `onOrbClick`ï¼ˆå•ä¸€è§¦å‘è·¯å¾„ï¼‰
6. `gsap` å·²å®‰è£…
7. `npm run build` é€šè¿‡ï¼›preview 4173 å·²é‡å¯åŠ è½½æœ€æ–°æ„å»º
8. å…¨é“¾è·¯éªŒè¯å®Œæˆï¼šæ˜Ÿé€”åŒå‘ toggleã€welcome è‡ªåŠ¨å¼€é¢æ¿ã€SPA çœŸå®ç‚¹å‡»å¯¼èˆªã€3D æ¸²æŸ“ï¼ˆ20.4% åƒç´ ï¼‰åœ¨ dev ä¸ preview å‡æ­£å¸¸

## 8. é‡è¦å†³ç­–åŠç†ç”±

- **timers ç”¨ Map**ï¼šé¿å…ã€Œnumber ä¸Šèµ‹å±æ€§ã€åœ¨ ESM ä¸¥æ ¼æ¨¡å¼æŠ› TypeError
- **optimizeDeps.entries é™å®š index.html**ï¼šéš”ç¦» city å­åº”ç”¨å¯¹é¢„æ„å»ºçš„æ±¡æŸ“
- **orb ç‚¹å‡»æ”¹ document capture å§”æ‰˜**ï¼šsetup é¡¶å±‚åŒæ­¥æ‰§è¡Œå¿…ç„¶æ³¨å†ŒæˆåŠŸï¼Œcapture é˜¶æ®µå…ˆäºä¸€åˆ‡å­å…ƒç´ /æ‹¦æˆªï¼Œä»»ä½•ç¯å¢ƒï¼ˆdev/preview/çœŸå®æµè§ˆå™¨ï¼‰éƒ½å¯é ï¼›å•ä¸€è·¯å¾„é¿å…åŒè§¦å‘
- **ä¿ç•™ welcome è‡ªåŠ¨å¼€é¢æ¿**ï¼šäº§å“è¡Œä¸ºï¼ˆé¦–æ¬¡è®¿é—®æ¬¢è¿ï¼‰ï¼Œé bugï¼Œä¸æ”¹
- **ä»¥ preview(4173) ä¸ºç”Ÿäº§å¯¹ç…§**ï¼šheadless ä¸‹ preview ä¸çœŸå®æµè§ˆå™¨è¡Œä¸ºä¸€è‡´ï¼Œdev çš„å¼‚å¸¸è¦å…ˆæ€€ç–‘æµ‹è¯•è¯¯åˆ¤

## 9. è¢«å¦å®šçš„æ–¹æ¡ˆ

- é‡å¯ + æ¸… `.vite` ç¼“å­˜æ¥ä¿®å¤ï¼ˆæ— æ•ˆï¼Œé—®é¢˜åœ¨ä»£ç ä¸åœ¨ç¼“å­˜ï¼‰
- `dispatchEvent` æ¨¡æ‹Ÿç‚¹å‡»ä½œä¸ºä¿®å¤ï¼ˆä»…æµ‹è¯•æ‰‹æ®µï¼›éªŒè¯å¿…é¡»ç”¨ CDP çœŸå®ç‚¹å‡»ï¼‰
- orb ä¿ç•™ @click + å¢åŠ å§”æ‰˜å…œåº•çš„åŒè·¯å¾„ï¼ˆå¯èƒ½åŒè§¦å‘ï¼Œæ”¹ä¸ºå•ä¸€å§”æ‰˜è·¯å¾„ï¼‰
- ã€Œdev ç‰¹æœ‰ Vue äº‹ä»¶ç»‘å®šé—®é¢˜ã€ï¼ˆç»é•¿æ—¶åºéªŒè¯æ˜¯ welcome è‡ªåŠ¨æ‰“å¼€é€ æˆçš„è¯¯åˆ¤ï¼‰
- git å›æ»šï¼ˆé¡¹ç›®æ— æäº¤å†å²ï¼Œä¸å¯è¡Œï¼‰

## 10. å½“å‰è¿›åº¦

- ä¸‰é¡¹é—®é¢˜å…¨éƒ¨ä¿®å¤å¹¶éªŒè¯é€šè¿‡ï¼ˆdev 5173 ä¸ preview 4173 å‡å®æµ‹æ­£å¸¸ï¼‰
- ä¸¤ä¸ªæœåŠ¡å‡åœ¨è¿è¡Œï¼›build ä¸ºæœ€æ–°
- å”¯ä¸€å¾…ç”¨æˆ·ç¡®è®¤é¡¹ï¼šç”¨æˆ·çœŸå®æµè§ˆå™¨ç¡¬åˆ·æ–° 5173 å¤æµ‹

## 11. å°šæœªå®Œæˆçš„ä»»åŠ¡

- ç­‰å¾…ç”¨æˆ·åœ¨çœŸå®æµè§ˆå™¨ç¡®è®¤ä¸‰é¡¹é—®é¢˜æ¶ˆå¤±ï¼ˆAI æ— æ³•ä»£åšï¼‰
- åç»­æ–°éœ€æ±‚ï¼ˆé¦–é¡µè§†è§‰å†ä¼˜åŒ–ã€æ˜Ÿé€”åŠŸèƒ½å¢å¼ºç­‰ï¼‰æŒ‰ç”¨æˆ·æŒ‡ç¤ºæ¨è¿›
- **æŒç»­ç»´æŠ¤æœ¬æ–‡æ¡£**ï¼šæ¯æ¬¡ token è¶…é™/å¼€å¯æ–°ä¼šè¯æ—¶ï¼ŒæŠŠæ–°è¿›å±•è¿½åŠ åˆ°å¯¹åº”å°èŠ‚å¹¶æ›´æ–°æ—¥æœŸï¼ˆç”¨æˆ·æ˜ç¡®è¦æ±‚ï¼‰

## 12. ä¸èƒ½éšæ„ä¿®æ”¹çš„å†…å®¹

- `XingTuAssistant.vue` å…¨éƒ¨æ ¸å¿ƒé€»è¾‘ï¼ˆè¯­éŸ³è¯†åˆ«/åˆæˆã€å‘½ä»¤ç³»ç»Ÿã€æ¬¢è¿é€»è¾‘ã€æ ·å¼ï¼‰ä¸ orb å§”æ‰˜æœºåˆ¶ï¼ˆ151-160ã€457ï¼‰
- `App.vue` ä¸­æ˜Ÿé€”æŒ‚è½½ï¼ˆApp.vue:8ï¼‰ä¸ `z-index:9999`
- `vite.config.js` çš„ `optimizeDeps` é…ç½®
- é¦–é¡µ heroï¼ˆ`GhibliHome.vue` ç¬¬ä¸€å±ï¼‰åŠå…¶ 3D ç”»å¸ƒ
- home ç»„ä»¶çš„èµ›åš/ç§‘æŠ€è§†è§‰é£æ ¼ä¸ç»“æ„

## 13. æ–°ä¼šè¯ç¬¬ä¸€æ­¥ï¼ˆç»™æ–° AI çš„æ“ä½œé¡ºåºï¼‰

1. è¯»å–æœ¬æ–‡ä»¶ï¼ˆHANDOFF.mdï¼‰
2. å‘ç”¨æˆ·**å¤è¿°ä½ çš„ç†è§£å¹¶æŒ‡å‡ºæ­§ä¹‰**ï¼Œç­‰ç”¨æˆ·ç¡®è®¤åå†ç»§ç»­ï¼ˆç”¨æˆ·ä¼šå…ˆå‘è¿™æ¡æŒ‡ä»¤ï¼‰
3. ç¡®è®¤åï¼šæ£€æŸ¥ 5173/4173 æœåŠ¡æ˜¯å¦åœ¨è¿è¡Œï¼ˆ`Get-NetTCPConnection` æˆ–ç›´æ¥è¯·æ±‚ï¼‰â†’ å¤æµ‹ä¸‰é¡¹ï¼ˆæ˜Ÿé€” orb åŒå‘ toggleã€SPA çœŸå®ç‚¹å‡»å¯¼èˆªã€hero 3D æ¸²æŸ“ï¼‰â†’ å†æ‰§è¡Œç”¨æˆ·æ–°éœ€æ±‚
4. ç»“æŸæ—¶æŒ‰ç¬¬ 11 èŠ‚ç»´æŠ¤æœ¬æ–‡æ¡£

---

## é™„ï¼šç”¨æˆ·å‘æ–°ä¼šè¯å‘é€çš„å¯åŠ¨æŒ‡ä»¤ï¼ˆåŸæ ·ç²˜è´´ï¼‰

> è¯·å…ˆå¤è¿°ä½ çš„ç†è§£å¹¶æŒ‡å‡ºæ­§ä¹‰ï¼Œæˆ‘ç¡®è®¤åå†ç»§ç»­æ‰§è¡Œã€‚

---

## 14. ç¤¾åŒºåŠŸèƒ½çœŸå®æ•°æ®åŒ– + è§†é¢‘ä¸Šä¼  + ä¸Šä¼ å®‰å…¨ç­–ç•¥ï¼ˆæœ¬æ¬¡ä¼šè¯å®Œæˆï¼‰

### 14.1 ç›®æ ‡
ç¤¾åŒºæ¨¡å—ï¼ˆæµè§ˆ/è¯„è®º/ç‚¹èµ/ç»Ÿè®¡ï¼‰å…¨éƒ¨èµ°æ•°æ®åº“çœŸå®æ•°æ®ï¼›å‘å¸ƒå†…å®¹å¯ä¸Šä¼ è§†é¢‘ï¼›ä¸Šä¼ æ–‡ä»¶é¡»è¿‡ç½‘ç»œå®‰å…¨æ ¡éªŒï¼ˆé˜²ç—…æ¯’/ä¼ªè£…æ¶æ„æ–‡ä»¶ï¼‰ã€‚

### 14.2 å·²å®Œæˆ
- **æ•°æ®åº“**ï¼š`post` åŠ  `view_count/author_name/media_url/media_type`ï¼Œ`comment` åŠ  `author_name`ï¼Œ`article` åŠ  `like_count/author_name`ï¼Œæ–°å»º `web3_blog.article_like`ï¼ˆarticle_id+user_id å”¯ä¸€é”®ï¼‰ã€‚init SQL å·²åŒæ­¥ï¼ˆ`backend/deploy/mysql/init/03-web3_blog.sql`ã€`04-web3_forum.sql`ï¼‰
- **forum-service(8083)**ï¼š`GET /post/stats`ï¼ˆtotalPosts/totalComments/todayPosts/topAuthors/topTagsï¼Œæ³¨å…¥ CommentMapperï¼‰ï¼›`getDetail` è‡ªå¢ viewCountï¼›create/comment è½ authorName
- **blog-service(8082)**ï¼š`POST /article/{id}/like` toggleï¼ˆarticle_like è¡¨ + like_count å¢å‡ï¼‰
- **media-service(8085)**ï¼š`POST /video/upload`ï¼ˆç™»å½•å¯è§ï¼‰+ `GET /video/file/{filename}`ï¼›`FileSecurityChecker` å®‰å…¨ç­–ç•¥ï¼šæ‰©å±•åç™½åå•ï¼ˆmp4/webm/mov/mkv/avi/flv + jpg/jpeg/png/gif/webpï¼‰ã€å¤§å°ä¸Šé™ 500MBï¼ˆ`media.upload.max-size`ï¼‰ã€é­”æ•°æ ¡éªŒï¼ˆMZ/ELF/class/ZIP ä¼ªè£…æ‹¦æˆªï¼‰ã€å±é™©ç‰¹å¾æ‰«æï¼ˆHTML script/php ç­‰ï¼‰ï¼›æ–‡ä»¶å UUID + æ­£åˆ™ç™½åå•é˜²è·¯å¾„ç©¿è¶Šï¼›`media.upload.dir=runtime/uploads`
- **å‰ç«¯**ï¼š`api/media.js`ï¼ˆgetMusicList/getVideoList/**uploadMedia** ä¸‰è€…å…±å­˜ï¼Œå‹¿åˆ åŸå¯¼å‡ºï¼‰ï¼›create.vue è§†é¢‘/å°é¢çœŸå®ä¸Šä¼  + authorName + mediaTypeï¼›detail.vue è§†é¢‘æ’­æ”¾å™¨ + ç‹¬ç«‹ loadCommentsï¼ˆä¿®å¤è¯„è®ºæ’ç©ºï¼‰ï¼›index.vue ç»Ÿè®¡/è¯é¢˜/ä½œè€…çœŸå®åŒ– + video tab è¿‡æ»¤ `mediaType==='video'` + article tab ä¿®å¤
- **API å®æµ‹**ï¼šç»Ÿè®¡/æµè§ˆè‡ªå¢/ç‚¹èµ toggle/ä¸Šä¼ å®‰å…¨æ‹¦æˆªï¼ˆä¼ªè£… MZã€fakehtmlã€.sh å…¨éƒ¨ 400ï¼‰å…¨è¿‡

### 14.3 å…³é”®å‘ï¼ˆé‡è¦ï¼Œå‹¿å†è¸©ï¼‰
- **é›ªèŠ± ID ç²¾åº¦ä¸¢å¤±**ï¼šLong id ç» JSON æ•°å­—è§£æåœ¨ JS ä¾§ä¸¢ç²¾åº¦ï¼ˆ19 ä½ > 2^53ï¼‰ã€‚å·²ç»™ PostVO/CommentVO/ArticleVO/VideoVO çš„ `id` åŠ  `@JsonSerialize(using = ToStringSerializer.class)`ï¼›detail.vue çš„ id **ä¸èƒ½ `Number()`**ï¼Œä¿æŒå­—ç¬¦ä¸²ã€‚å…¶ä½™æœåŠ¡ï¼ˆsoftware/tool/resource/shop/user/quantï¼‰VO çš„ id ä»æ˜¯æ•°å­—ï¼Œåç»­æ¶‰åŠç‚¹é€‰è·³è½¬æ—¶åŒæ ·è¦æ”¹
- **axios ä¸Šä¼  multipart**ï¼šä¸èƒ½æ‰‹åŠ¨è®¾ `Content-Type: multipart/form-data`ï¼ˆæ—  boundary ä¼š 500ã€Œno multipart boundaryã€ï¼‰ï¼›ä¸” uploadMedia å¿…é¡»æŠŠ File åŒ…æˆ FormData å† post
- **gateway è·¯ç”±æ—  `/api` å‰ç¼€**ï¼šç›´è¿ 8080 æµ‹è¯•è·¯å¾„æ˜¯ `/post/...` è€Œé `/api/post/...`ï¼ˆ`/api` ç”± vite ä»£ç†å‰¥æ‰ï¼‰
- **å‰ç«¯ç‚¹èµå¿…é¡» toggle**ï¼š`likeCount + (liked ? -1 : 1); liked = !liked`ï¼ˆæ—§ä»£ç åª +1ï¼ŒäºŒæ¬¡ç‚¹å‡»å¤±æ•ˆï¼‰
- **åˆ—è¡¨å¡ç‰‡è·¯ç”±**ï¼š`goDetail` ä¸­ `item.type === 'article' ? 'article' : 'post'`ï¼ˆè§†é¢‘å¸– type='video' ä¹Ÿèµ° postï¼‰
- **e2e è„šæœ¬**ï¼šåŒä¸€ evaluate å†… set value + click æ—¶ Vue å“åº”å¼æœª flushï¼ŒæŒ‰é’®ä» disabledï¼›éœ€æ‹†ä¸¤æ­¥ç­‰å¾…
- åç«¯åç¼–è¯‘é£æ ¼ï¼šæ—  Lombokï¼Œæ‰‹å†™ getter/setterï¼›`@Update` SQL å†™åœ¨ mapper æ¥å£

### 14.4 å½“å‰çŠ¶æ€
- æœåŠ¡è¿è¡Œä¸­ï¼š3306 MySQLã€8080 gatewayã€8082 blogã€8083 forumã€8085 mediaã€8089 resourceã€5173 devã€4173 previewï¼ˆæœ€æ–° buildï¼‰
- æ„å»ºï¼šMaven `F:\tools\apache-maven-3.9.6` + JDK17 `F:\tools\jdk17`ï¼ˆmvn ä¸åœ¨ PATHï¼‰ï¼›æ‰“åŒ…å‰å…ˆæ€ java è¿›ç¨‹ï¼ˆjar å ç”¨ï¼‰
- æµ‹è¯•æ•°æ®å·²æ¸…ç†ï¼ˆå¸–å­/è¯„è®º/ç‚¹èµ/ä¸Šä¼ æ–‡ä»¶å›åˆ°åˆå§‹æ€ï¼šposts=7ã€comments=7ã€likes=0ã€articles=8ï¼‰
- å®Œæ•´é“¾è·¯å®æµ‹é€šè¿‡ï¼šå‘å¸ƒè§†é¢‘å¸– â†’ è§†é¢‘ tab å¡ç‰‡ â†’ è¯¦æƒ…æ’­æ”¾å™¨ â†’ è¯„è®ºå…¥åº“ â†’ ç‚¹èµ toggle â†’ æµè§ˆè‡ªå¢ â†’ ç»Ÿè®¡ï¼ˆä½œè€…ã€Œç®¡ç†å‘˜ã€å‡ºç°ï¼‰â†’ æ–‡ç« è¯¦æƒ…æ­£å¸¸

---

## 15. ç™»å½•ä¿®å¤ + æ˜Ÿé€”å”¤é†’è¯ç³»ç»Ÿï¼ˆæœ¬æ¬¡ä¼šè¯å®Œæˆï¼‰

### 15.1 ç™»å½•ã€ŒæˆåŠŸä½†æ²¡è¿›å»ã€æ ¹å› ä¸ä¿®å¤
- æ ¹å› ï¼šåç«¯ä¸šåŠ¡é”™è¯¯ï¼ˆå¦‚ç”¨æˆ·ä¸å­˜åœ¨/å¯†ç é”™ï¼‰è¿”å› `{code:500, data:null}` ä½† **HTTP 200**ï¼›auth.js æ—§é€»è¾‘ `res.data || res` æå–ä¸åˆ° token æ—¶**é™é»˜ä¸æŠ›é”™** â†’ login.vue è¯¯å¼¹ã€Œç™»å½•æˆåŠŸã€å¹¶è·³è½¬ï¼Œä½†æ—  tokenï¼Œè¿›å—ä¿æŠ¤é¡µè¢« 401 è¸¢å›
- ä¿®å¤ï¼š`stores/modules/auth.js` login/register ä¸¥æ ¼æ ¡éªŒ `payload.token` å­˜åœ¨ï¼Œå¦åˆ™ `throw new Error(res.message)`ï¼›login.vue catch æ˜¾ç¤ºåç«¯å…·ä½“é”™è¯¯ï¼ˆã€Œç”¨æˆ·ä¸å­˜åœ¨æˆ–å¯†ç é”™è¯¯ã€è€Œéç¬¼ç»Ÿã€Œç™»å½•å¤±è´¥ã€ï¼‰
- å®æµ‹ï¼šé”™è¯¯å¯†ç  â†’ åœç•™ /login + æ—  token + å…·ä½“é”™è¯¯ toast âœ“ï¼›admin/admin123 ç™»å½• + redirect å›åŸé¡µ âœ“ï¼ˆdev ä¸ preview å‡æ­£å¸¸ï¼‰

### 15.2 æ˜Ÿé€”å”¤é†’ï¼ˆã€Œæ˜Ÿé€”æ˜Ÿé€”ã€é—¨ç¦ + æŒç»­ç›‘å¬ + å†…å®¹è·å–ä¿®å¤ï¼‰
ç”¨æˆ·éœ€æ±‚ï¼šæŒç»­ç›‘å¬ï¼›é¡»è¯´ã€Œæ˜Ÿé€”æ˜Ÿé€”ã€æ‰å”¤é†’ï¼›ä¿®å¤è¯†åˆ«å†…å®¹é”™ä¹±ã€‚
- **å”¤é†’é—¨ç¦**ï¼š`processTranscript` é‡å†™â€”â€”åªæœ‰å«ã€Œæ˜Ÿé€”æ˜Ÿé€”ã€çš„è¯­å¥æ‰æ‰“å¼€é¢æ¿å¹¶å“åº”ï¼›æœªå”¤é†’çš„ä»»ä½•è¯­å¥å…¨éƒ¨å¿½ç•¥ï¼›å”¤é†’å **5 ç§’çª—å£**å†…å¯ç›´æ¥ä¸‹è¾¾æŒ‡ä»¤ï¼ˆå¯è¿ç»­è¯´ï¼‰ï¼›æŒ‡ä»¤è‡ªåŠ¨å‰¥ç¦»å”¤é†’è¯ï¼ˆã€Œæ˜Ÿé€”æ˜Ÿé€”æ‰“å¼€ç¤¾åŒºã€â†’ æŒ‡ä»¤ã€Œæ‰“å¼€ç¤¾åŒºã€ï¼‰ï¼›å•ç‹¬è¯´å”¤é†’è¯ â†’ å›ã€Œæˆ‘åœ¨ï¼â€¦ã€
- **æŒç»­ç›‘å¬**ï¼š`silentRestart` å»æ‰ `isOpen` ä¾èµ–ï¼ˆé¢æ¿å…³é—­ä¹Ÿåœ¨ç›‘å¬ï¼‰ï¼Œä»…å— `manualStop`ï¼ˆæ‰‹åŠ¨ç‚¹ã€Œè¯­éŸ³/åœæ­¢ã€ï¼‰ä¸ `document.visibilityState` é™åˆ¶ï¼›ä¿®å¤æ—§ bugã€Œæ‰‹åŠ¨åœæ­¢å 450ms åˆè¢«è‡ªåŠ¨æ‹‰èµ·ã€ï¼ˆstopListening ç°åœ¨ç½® `manualStop=true`ï¼‰
- **å†…å®¹è·å–ä¿®å¤**ï¼š`onresult` ç”±å…¨é‡æ‹¼æ¥å†å²ï¼ˆæ¯æ¬¡ final éƒ½å¸¦å‰é¢æ‰€æœ‰å¥å­ â†’ å†…å®¹é”™ä¹±/é‡å¤ï¼‰æ”¹ä¸º**å¢é‡å¤„ç†**â€”â€”æ¯ä¸ª final ç»“æœåªè§¦å‘ä¸€æ¬¡ processTranscriptï¼Œinterim ä»…åˆ·æ–°çŠ¶æ€æ˜¾ç¤ºã€Œè¯†åˆ«ä¸­...ã€
- **å¯¼èˆªæŒ‡ä»¤æœ¬åœ°åŒ–**ï¼š`respond` å…ˆæŸ¥æœ¬åœ°å¯¼èˆªå‘½ä»¤è¡¨ï¼ˆå‘½ä¸­ path ç«‹å³è·³è½¬+å›å¤ï¼Œä¸ä¾èµ–åç«¯ jarvis 9001â€”â€”åç«¯å¯¹ã€Œæ‰“å¼€ç¤¾åŒºã€ç­‰ä¸­æ–‡å¯¼èˆªä¸è¿”å› NAVIGATEï¼‰ï¼›éå¯¼èˆªæŒ‡ä»¤ä»åç«¯ä¼˜å…ˆã€å¤±è´¥æœ¬åœ°å…œåº•
- éªŒè¯æ–¹å¼ï¼ˆheadless æ— éº¦å…‹é£ï¼‰ï¼š`page.evaluateOnNewDocument` æ³¨å…¥ fake `SpeechRecognition`ï¼Œé©±åŠ¨çœŸå®ç»„ä»¶çš„ onresult â†’ æ–­è¨€é¢æ¿/æ¶ˆæ¯/è·³è½¬ã€‚å®æµ‹ï¼šæœªå”¤é†’æŒ‡ä»¤è¢«å¿½ç•¥ âœ“ å”¤é†’å¼€é¢æ¿+æ¬¢è¿è¯­ âœ“ çª—å£å†…æŒ‡ä»¤å“åº” âœ“ ã€Œæ˜Ÿé€”æ˜Ÿé€”æ‰“å¼€ç¤¾åŒºã€å‰¥ç¦»å”¤é†’è¯å¹¶è·³è½¬ /community âœ“ 5s çª—å£è¿‡æœŸåæŒ‡ä»¤è¢«å¿½ç•¥ âœ“
- æ¶‰åŠæ–‡ä»¶ï¼š`components/common/XingTuAssistant.vue`ï¼ˆprocessTranscript/respond/silentRestart/start/stopListening/onresultï¼‰ã€`stores/modules/auth.js`ã€`views/login/index.vue`

### 15.3 æœªæ”¹åŠ¨/æœªéªŒè¯
- æ˜Ÿé€”åç«¯ jarvis-service(9001) çš„è¯†åˆ«èƒ½åŠ›æœªæ”¹ï¼ˆæœ¬åœ°å¯¼èˆªå·²å…œåº•ï¼‰ï¼›çœŸå®éº¦å…‹é£è¯­éŸ³è¯†åˆ«éœ€ç”¨æˆ·åœ¨ Chrome/Edge å®æœºéªŒè¯
- welcome è‡ªåŠ¨å¼€é¢æ¿ã€orb ç‚¹å‡»å§”æ‰˜æœºåˆ¶ä¿æŒåŸæ ·

---

## 16. é¦–é¡µç¬¬ä¸€å±æŒ‰é’®æ¸…ç† + å„å±ç‚¹å‡»è¡¥é½ï¼ˆæœ¬æ¬¡ä¼šè¯å®Œæˆï¼‰

- **ç¬¬ä¸€å±**ï¼šåˆ é™¤ hero ã€Œæ¢ç´¢æ¨¡å—ã€ï¼ˆscrollToSection('showcase')ï¼‰ä¸ã€Œæ ¸å¿ƒæ¶æ„ã€ï¼ˆscrollToSection('tech')ï¼‰ä¸¤ä¸ªæŒ‰é’®åŠ scrollToSection/action-btn CSS/ç§»åŠ¨ç«¯æ ·å¼ï¼ˆ`views/home/GhibliHome.vue`ï¼‰ã€‚**å…¨ç«™ä¸å†å‡ºç°ã€Œæ¢ç´¢æ¨¡å—ã€å­—æ ·**ï¼›ã€Œæ ¸å¿ƒæ¶æ„ã€ä»…åœ¨ [01] TechMatrix æ ‡é¢˜å‡ºç° â†’ å·²ä¸€å¹¶æ”¹ä¸ºã€ŒæŠ€æœ¯çŸ©é˜µä¸ç³»ç»Ÿæ¶æ„ã€
- **ç‚¹å‡»æ²¡ååº”æ ¹å› **ï¼š[01] TechMatrix çš„ 4 å¼ å¡ç‰‡ `cursor:pointer` ä½†æ— ä»»ä½•ç‚¹å‡»äº‹ä»¶ â†’ è¡¥ `@click="go(item.path)"` + routerï¼ˆAI æ™ºèƒ½å¼•æ“â†’/communityã€3Dâ†’/mediaã€å¾®æœåŠ¡â†’/resourcesã€å¹²è´§â†’/communityï¼‰ï¼›éº»æœ¨åŠ  mode è§’æ ‡ä¿ç•™
- **æ–‡ç« ç›´è¾¾è¯¦æƒ…**ï¼šArticleCards çš„ `go(id)` åŸæ¥åªè·³ `/community?focus=id`ï¼ˆåˆ—è¡¨é¡µä¸å¤„ç† focusï¼Œç­‰äºæ²¡ååº”ï¼‰â†’ æ”¹ä¸º `router.push(\`/community/article/${id}\`)`ï¼ˆæœ‰ id ç›´è¾¾è¯¦æƒ…ï¼Œæ—  id è½ /communityï¼‰
- å®æµ‹ï¼ˆheadless çœŸå®ç‚¹å‡»ï¼‰ï¼šhero æ— æŒ‰é’® âœ“ï¼›TechMatrix å¡ â†’ /community âœ“ï¼›Showcase LIVE DEMO â†’ /community âœ“ï¼›Showcase å¡ç‰‡ overlay å±•å¼€ï¼ˆAI çŸ¥è¯†åº“æ™ºèƒ½ä½“ï¼‰âœ“ï¼›ArticleCards 8 ç¯‡çœŸå®æ–‡ç« åŠ è½½ & ç‚¹å‡» â†’ /community/article/1 âœ“
- build é€šè¿‡ï¼ˆ20.68sï¼‰ï¼Œ4173 preview å·²é‡å¯åŠ è½½æœ€æ–°æ„å»º

---

## 17. Ê×Ò³×é¼ş¸÷¹éÄÚÈİÒ³ + ÒôÀÖÒ³ÖØ×öÎªÔÓÖ¾º£±¨·ç²¥·ÅÆ÷£¨±¾´Î»á»°Íê³É£©

### 17.1 Ê×Ò³×é¼ş ¡ú ×¨ÊôÄÚÈİÒ³£¨²»ÔÙÈ«Ìø /community£©
- ĞÂÔö 3 ¸ö¾²Ì¬ÄÚÈİÒ³£¨GSAP + ScrollTrigger ½¥ÏÔ/ÉìËõ£¬°µÉ«Èü²©·çÓëÊ×Ò³Ò»ÖÂ£©£º
  - rontend/src/views/ai/index.vue£¨/ai£©£ºAI ÖÇÄÜÒıÇæ ¡ª¡ª hero + 6 ²½ RAG Á÷Ë®Ïß¿¨Æ¬ + 6 ÄÜÁ¦¾ØÕó
  - rontend/src/views/three/index.vue£¨/three£©£º3D Á£×Ó¿Õ¼ä ¡ª¡ª Three.js Á£×ÓĞÇÔÆ hero£¨1800 Á£×Ó£¬Êó±êÁ÷¶¯/¹ö¶¯É¢¾Û£©+ Á£×ÓĞÎÌ¬ 3 ¿¨ + ÊÖÊÆÕ¹ÌüÁ÷³Ì£¨MediaPipe£©
  - rontend/src/views/architecture/index.vue£¨/architecture£©£ºÎ¢·şÎñÍØÆË ¡ª¡ª Íø¹Ø¡ú7 ·şÎñ´Ø¡úMySQL/Nacos/RabbitMQ/MinIO ÍØÆË + Á÷¹âÏÂÂä¶¯»­ + 4 ÕÅ¼¼ÊõÕ»¿¨
- Â·ÓÉ£ºouter/index.js ÈıÒ³¾ùÌ× Web3Layout£¨/media Í¬¿îĞ´·¨£©£¬²»½øµ¼º½²Ëµ¥£¬Ê×Ò³¿¨Æ¬Ö±´ï
- **TechMatrix ¿¨ path**£ºAI¡ú/ai¡¢3D¡ú/three¡¢Î¢·şÎñ¡ú/architecture¡¢¸É»õ¡ú/community£¨²»±ä£©
- **ShowcaseGrid ¿¨ path**£ºAI ÖªÊ¶¿â¡ú/ai¡¢3D Á£×ÓÔÆ¡ú/three¡¢Î¢·şÎñ½ÅÊÖ¼Ü¡ú/architecture¡¢ÊÖÊÆÕ¹Ìü¡ú/three

### 17.2 ÒôÀÖÒ³ ¡ú FASHION ÔÓÖ¾º£±¨·ç²¥·ÅÆ÷£¨/music ÖØĞ´£©
- **Ô¼Êø´ï³É**£º×é¼ş»¯ iews/music/components/MusicPlayer.vue£»Âß¼­³é¶ÀÁ¢ iews/music/musicLogic.js£¨´¿ JS ¹¤³§£¬ÎŞ Vue ÒÀÀµ£©£»¸è´ÊÊı¾İ¶ÀÁ¢ iews/music/lyricsData.js£¨3 Ê×ÑİÊ¾Çú + LRC Ê±¼ä´Á¸è´Ê£©£»ÑùÊ½Âß¼­·ÖÀë ?
- **musicLogic.js**£ºÄ£Äâ²¥·ÅºËĞÄ£¨setInterval 250ms ÍÆ½ø time 0.25s/²½£©£¬play/toggle/pause/resume/next/prev/seekByPercent/cycleMode(Ë³Ğò¡¤Ñ­»·¡¤Ëæ»ú¡¤µ¥Çú)/toggleLike/setPlaylist/destroy + indLyricIndex£»ËùÓĞÃüÁî¶¼ emitTick ¡ú ×é¼şÄÚ eactive ¾µÏñ Object.assign(ui, player.state) Çı¶¯ÏìÓ¦Ê½£¨´¿¶ÔÏóÖ±½Ó¶Á²»»á´¥·¢äÖÈ¾£¬´Ë¿ÓÒÑ²ÈÒÑ½â£©£»ºóĞø¶Ô½ÓÕæÊµ Audio Ö»ĞèÌæ»» tick ÍÆ½ø
- **MusicPlayer.vue**£º×ó²àºÚ½º³ªÆ¬£¨CSS Í¬ĞÄÔ²ÎÆ + ·´¹â + ÖĞĞÄ·âÃæ/½¥±äÊ××Ö±êÇ©Õ¼Î»£¬²¥·ÅĞı×ª/ÔİÍ£Í£ + ³ª±ÛËæ²¥·ÅÂä±Û£©+ ¹ö¶¯¸è´Ê£¨µ±Ç°ĞĞ·Å´ó¸ßÁÁ¡¢translateY ¾ÓÖĞ£©£»µ×²¿Ä¥É°¿ØÖÆÀ¸£¨½ø¶ÈÌõ¿ÉµãÍÏ¡¢Ê±¼ä¡¢ÒôÖÊ chip¡¢Ñ­»·/ÉÏÏÂÇú/²¥·ÅÔİÍ£/¸èµ¥/µãÔŞ/ÆÀÂÛ + µ¯³ö¸èµ¥Ãæ°å£©£»¿Õ¸ñ¼ü²¥·ÅÔİÍ££»@track-change ÊÂ¼şÍ¨ÖªÒ³Ãæ
- **index.vue º£±¨Ò³**£ºÃ×°×Ö½ÕÅÖÊ¸Ğ + ¿ÅÁ£ + ¸¡¶¯¹âÔÎ£¨Êó±êÊÓ²î GSAP quickTo£©£»¿¯Í·¡¸FASHION ¡¤ VOL.01¡¹+ SELF-PORTRAIT ±êÇ©Ìõ£»ÓÒ²àÈËÎï²å²Û personImg£¨Ô¤Áô£¬Î´ÌîÏÔÊ¾¼ôÓ°Õ¼Î»£©+ CSS »¨¶ä + »¨Ó°ºôÎü¹âÔÎ + ¸¡¶¯»¨°ê£»¸èÇúÊı¾İÏÈÇëÇó media-service /music/list£¬¿ÕÔò»ØÍË DEMO_TRACKS
- **²âÊÔ**£ºbuild Í¨¹ı£¨14.7s£©£»4173 preview ÖØÆô£»/ai /three /architecture /music ¾ù 200

### 17.3 ×¢Òâ
- Èı¸öÄÚÈİÒ³Îª¾²Ì¬Õ¹Ê¾Ò³£¨ÎŞºó¶Ë½Ó¿ÚÖ§³Å£©£»/three Á£×Ó Canvas ÒÑ onBeforeUnmount ÇåÀí
- ÒôÀÖÒ³ @import ÁË Google Fonts£¨Playfair Display£©£¬ÀëÏß/ÎŞÍøÊ±»ØÍËÏµÍ³³ÄÏß£¬²»Ó°Ïì²¼¾Ö
- ÈËÎï²å²Û½ÓÈëÕæÈËÍ¼ºó£¬person-placeholder ¼ôÓ°×Ô¶¯Òş²Ø£¨v-if/v-else ÒÑ´¦Àí£©

## 18. Êé¼®ÔÄ¶ÁÄ£¿é + Ã½Ìå¹ÜÀí´òÍ¨£¨±¾»á»°Íê³É£©
### 18.1 Êı¾İ²ã
- ĞÂÔö `backend/deploy/mysql/init/12-web3_book.sql`£¨UTF-8£¬Îğ»ìĞ´½ø GBK µÄ 06 ÎÄ¼ş£©£º`book` + `book_chapter` ½¨±í + ÖÖ×Ó£¨µÀµÂ½›81ÕÂ/Õ“ÕZ20/ŒO×Ó±ø·¨13/Ç§×ÖÎÄ/Èı×Ö½›£¬¹² 5 Êé 116 ÕÂ£¬Ô¼ 6 Íò×Ö£¬Ô´ zh.wikisource.org ¹«°æ¹Å¼®£©
- ÖÖ×Ó×¢ÒâÊÂÏî£ºWikimedia API ±ØĞë´ø User-Agent Í·£»Windows ÏÂ Node fetch ²»×ß´úÀí»á³¬Ê±£¬¸ÄÓÃ PowerShell Invoke-WebRequest£»Ğ´½Å±¾ÓÃ \uXXXX ×ªÒå·À±àÂëÂÒÂë£»Ç§×ÖÎÄ×÷ÕßÎóĞ´"ÖÜÅdÃã"ÒÑĞŞÎª"ÖÜÅdËÃ"£¨´æ¿â + init ÎÄ¼şÁ½´¦£©
- ÔËĞĞ¿âÒÑÖ²Èë£»ĞÂ×°¿âÖ´ĞĞ 12 ºÅ½Å±¾¼´¿É

### 18.2 media-service
- Book/BookChapter ÊµÌå+Mapper+VO+Service+Controller Æë±¸£¨·ÖÒ³/ÏêÇé/Ä¿Â¼/ÕÂ½Ú/ÔöÉ¾¸Ä£¬¾ù´ø requireAdmin ¼øÈ¨£©£¬½Ó¿Ú¾­Íø¹Ø `/book/**`
- ĞŞ¸´È«ÏîÄ¿Í¨²¡£ºmedia-service È± MyBatis-Plus ·ÖÒ³À¹½ØÆ÷µ¼ÖÂ PageResult.total ºãÎª 0 ¡ª¡ª ĞÂÔö `config/MybatisPlusConfig`£¨PaginationInnerInterceptor, maxLimit=200£©£¬music/video/book È«²¿ÉúĞ§£¨Êµ²â total=5/20/12£©
- `POST /video/upload` À©Õ¹¿ÉÑ¡²ÎÊı title/description/tags/cover£¨È±Ê¡ title=ÎÄ¼şÃû£©£»VideoService.upload Í¬²½Èë¿â video ±í
- ÒôÀÖÎÄ¼şÈÔ×ß resource-service ÍĞ¹Ü£¨downloadUrl »ØÌî music.url£©£¬ÊÓÆµÎÄ¼ş×ß media-service ±¾µØ `runtime/uploads`

### 18.3 Ç°¶Ë
- `/media` ¸ÄÎªË«À¸²¼¾Ö£ºÒôÀÖ+Êé¼®ÉÏÅÅ£¨xl Á½À¸£©¡¢ÊÓÆµÏÂÅÅ£»Êé¼®¿¨Æ¬¹Å¼®·âÃæÑùÊ½£¨ÊúÅÅÊéÃû£©
- ĞÂÔöÊé¼®ÔÄ¶ÁÒ³ `/media/book/:id`£¨`views/media/book.vue`£©£º×ó²àÕ³ĞÔÄ¿Â¼µ±Ç°ÕÂ¸ßÁÁ¡¢ÓÒ²à³ÄÏßÕıÎÄ¡¢ÉÏÏÂÕÂÇĞ»»¡¢×ÖÊıÍ³¼Æ¡¢ÊúÅÅ·½°¸ÒÑ¿³£¨ÒÆ¶¯¶ËºáÅÅ£©
- admin/media.vue Èı Tab£¨ÒôÀÖ/ÊÓÆµ/Êé¼®£©£ºÊé¼®Ö§³ÖÔöÉ¾¸Ä£¨×ß createBook/updateBook/deleteBook£©£»ÊÓÆµÉÏ´«¸Ä×ß `POST /video/upload`£¨media-service Èë¿â£©£»ÒôÀÖÉÏ´«=resource-service È¡ downloadUrl + `POST /music` Èë¿â£¨ĞŞ¸´Óë media-service ÍÑ¹³ÎÊÌâ£©
- ĞÇÍ¾Ê¶±ğĞŞ¸´£¨XingTuAssistant.vue£©£ºµ¥µ÷ÓÎ±ê lastFinalIndex Ö»´¦ÀíĞÂ final ½á¹û£¨Chrome ÖØ·¢ÀúÊ·½á¹ûµ¼ÖÂÖØ¸´/´íÂÒ£©£»½á¹ûË÷ÒıÔ½½çÇ¯ÖÆ£»no-speech ´íÎó²»ÔÙÍ£¼àÌı£¨¾²Ä¬ĞøÆô£©£»disposed Ğ¶ÔØÊØÎÀ·ÀÒ³ÃæÏú»ÙºóÖØÆôÊ¶±ğ

### 18.4 ÑéÖ¤
- admin/admin123 µÇÂ¼ ¡ú ´´½¨/¸üĞÂ/É¾³ı book + music È«²¿ 200£¬×ÜÁ¿»Ø 5
- Íø¹Ø /book/list /music/list /video/list total ÕıÈ·£»/media /media/book/1 /admin/media /ai /three /architecture /music È«²¿ 200
- ´ı°ì£ºĞÇÍ¾Ê¶±ğĞèÕæ»ú Chrome Êµ²âÈ¥ÖØĞ§¹û£»ÊÓÆµÊ±³¤/·âÃæ½âÎöÈë¿â£¨µ±Ç° duration=0£¬ÉÏ´«½Ó¿ÚÎ´×ö ffprobe£©£»ÕÂ½Ú¼¶¹ÜÀí£¨admin Î´¼ûÕÂ±à¼­ UI£¬½Ó¿ÚÒÑ±¸£©
## 19. ĞÇÍ¾»½ĞÑÓÅ»¯ + Ã½ÌåÒ³µÈ¸ß + ÒôÀÖ¹İ×ª³¡ÖØ¹¹£¨±¾»á»°Íê³É£©
### 19.1 ĞÇÍ¾£¨XingTuAssistant.vue£©
- »½ĞÑÌáÊ¾ÎÄ°¸È¥µô"Ëµ"×Ö£º`»½ĞÑ£º"ĞÇÍ¾ĞÇÍ¾"`
- Ğü¸¡Çò¶¨Î»´ÓÓÒÏÂ½Ç¸ÄÎªÒ³ÃæÕıÏÂ·½¾ÓÖĞ£¨`left:50% + translateX(-50%)`£¬ÒÆ¶¯¶Ë bottom:12px£©

### 19.2 Ã½ÌåÒ³£¨views/media/index.vue£©
- ÒôÀÖ/Êé¼®Á½À¸µÈ¸ß£º`xl:h-[560px]` + flex-col£¬ÁĞ±íÇø `flex-1 min-h-0 overflow-y-auto`£¨³¬³ö×Ô´ø¹ö¶¯Ìõ£¬×ÏÉ«Ï¸¹ö¶¯Ìõ `.music-scroll`£©£¬¿ÕÌ¬¾ÓÖĞ

### 19.3 ÒôÀÖ¹İ×ª³¡ÖØ¹¹£¨views/music/index.vue È«ĞÂ£©
- ³õÊ¼³¡¾°£ºÃ×°×Å¯µ× + ÈËÎï¼ôÓ°£¨CSS Õ¼Î»£©+ ÊÖ³ÖÍ¸Ã÷³ªÆ¬£¨"ÊÇ""·ñ"Á½×Ö¡¢5.5s Çá°Ú¶¯»­£©+ µ×²¿"µã»÷ÈÎÒâ´¦"Âö³åÌáÊ¾
- ×ª³¡£ºµã»÷ÈÎÒâ´¦»ò 3.2s ×Ô¶¯´¥·¢£¨GSAP timeline ~2.6s£©¡ª¡ª³ªÆ¬´ÓÊÖÖĞ·ÉÏòÖ÷³¡¾°Âäµã£¨power3.inOut£©£¬ÖĞÍ¾ progress>0.55 Í¸Ã÷³ªÆ¬Ô­µØ»»·ôÎªºÚ½º£¨grooves/¹âÔó/Å¯É« label + ÈËÏñ¼ôÓ°Õ¼Î» + ×Ô×ª£©£»ÈËÎïµ­³ö¡¢±êÌâ FASHION/SELF-PORTRAIT/1901 ÖğĞĞ skew Èë³¡¡¢²¥·ÅÆ÷ÏÂ»¬Èë¡¢±³¾°½»²æÎªÇïÈÕÅ¯³È
- Ö÷³¡¾°£º×óÉÏ FASHION£¨×î´ó£©/SELF-PORTRAIT/1901 Èı¼¶±êÌâ£»ºÚ½º³ªÆ¬¾ÓÖĞ£¨hero-vinyl Õ¼Î»ÒõÓ° + ËÄÖÜ ? Ğı×ªµã×º£©£»³ªÆ¬ÏÂ·½ MusicPlayer£¨¸´ÓÃ×é¼ş£¬`show-vinyl=false` Òş²ØÄÚÖÃºÚ½º¡¢Ö»Áô¸è´Ê+¿ØÖÆÀ¸£¬ĞÂÔöÒôÁ¿Í¼±ê°´Å¥ toggle ¾²Òô£©
- »ÆÒ¶£º26 Æ¬Ëæ»ú£¨Î»ÖÃ/´óĞ¡/Ê±³¤/ÑÓ³Ù/°Ú¶¯·ù¶È/Ğı×ª½Ç/É«µ÷/¾°Éî blur/²ã¼¶£©£¬CSS keyframes ´¹Ö±Æ®ÂäÑ­»·
- ÏìÓ¦Ê½£º¡Ü1024 ×İÏò²¼¾Ö¡¢³ªÆ¬ËõĞ¡¡¢Ò³Ãæ¿É¹ö¶¯£»¡Ü640 Òş²Ø»ÆÒ¶
- ¼¼ÊõÔ¼Êø×ñÑ­£ºVue3 + GSAP£¨²»Òı React Ïµ¿â£©£¬ÎŞÍâ²¿´óÍ¼£¨CSS ½¥±äÓªÔìÇïÈÕ£©

### 19.4 ÑéÖ¤
- build Í¨¹ı£»/music /media /media/book/1 / È«²¿ 200
- ×¢Òâ£ºmedia/index.vue ÔøÒòÎóÉ¾»»ĞĞµ¼ÖÂ `const router = useRouter()const player...` ±àÒë´íÎó£¬ÒÑĞŞ
## 20. Ê×Ò³ÓïÒôÌáÊ¾ + ĞÇÍ¾ÍÏ¶¯ + ÒôÀÖ¹İ¸ÄÁĞ±íÒ³/µ¥Çú²¥·ÅÒ³£¨±¾»á»°Íê³É£©
### 20.1 Ê×Ò³£¨GhibliHome.vue£©
- hero ÓïÒôÌáÊ¾ÎÄ×Ö `Ëµ"ĞÇÍ¾ĞÇÍ¾"»½ĞÑÖÇÄÜÖúÊÖ` ¡ú `ĞÇÍ¾ĞÇÍ¾»½ĞÑÖÇÄÜÖúÊÖ`£¨È¥"Ëµ"×ÖÓëÒıºÅ£©
- voice-hint ÅÅ°æĞŞ¸´£º´Ó subtitle ÏÂ·½ 2.5rem ¼ä¾àÊÕ½ôÎª 0.9rem£¬white-space:nowrap ·À»»ĞĞ£¬±£³Ö hero ÄÚË®Æ½´¹Ö±¾ÓÖĞ£¨º¬ÒÆ¶¯¶Ë margin Í¬²½£©
### 20.2 ĞÇÍ¾Ğü¸¡Çò£¨XingTuAssistant.vue£©
- Ğü¸¡ÇòÖ§³ÖÍÏ¶¯£ºpointerdown/move/up + ÊÓ¿Ú±ß½ç clamp + ËÉÊÖ×óÓÒÌù±ßÎü¸½ + transition ¹Ø±Õ·ÀÍÏ¶Ù£»Î»ÒÆ>4px ÊÓÎªÍÏ¶¯£¬ÍÏÍê suppress 60ms click ·ÀÎó¿ª¹ØÃæ°å£»touch-action:none ·À´¥Ãş¹ö¶¯³åÍ»£»ÍÏ¶¯ºó inline left/top ¸²¸ÇÄ¬ÈÏµ×²¿¾ÓÖĞÑùÊ½
### 20.3 ÒôÀÖ¹İ²ğ·Ö£¨ÖØÒª½á¹¹µ÷Õû£©
- `/music`£¨views/music/index.vue ÖØĞ´£©£½ÒôÀÖ¹İÁĞ±íÒ³£º¹İÍ·(ºÚ½º logo)+·ÖÀà chips+¸èÇúÁĞ±í£¨ĞòºÅ/·âÃæÔ²/¸èÃû/ÒÕÊõ¼Ò/Ê±³¤£©£¬µã»÷ĞĞÌø `/music/player/:id`£»±£Áôµ×²¿ GlobalPlayer
- ĞÂÔö `/music/player/:id`£¨views/music/player.vue£©£½µ¥Çú²¥·ÅÒ³£¨Ô­×ª³¡³¡¾°¸ÄÔì£©£º
  - È¥µô³õÊ¼³¡¾°Óë"ÊÇ/·ñ"³ªÆ¬¡¢µã»÷×ª³¡
  - Èë³¡¶¯»­£ººÚ½º scale0.3+rotate-120 ¸¡ÏÖ£¨Íâ²ã wrap ³ĞÔØ GSAP£¬ÄÚ²ã CSS ³£×ª£¬½â¾ö animation Óë inline transform ³åÍ»£©¡¢·µ»Ø°´Å¥¡¢FASHION ÈıĞĞ±êÌâ¡¢²¥·ÅÆ÷»¬Èë¡¢»ÆÒ¶ÑÓ³Ùµ­Èë
  - ºÚ½º label ÏÔÊ¾µ±Ç°ÇúÄ¿·âÃæ/Ê××Ö£»ÓÒÉÏ"¡û ÒôÀÖ¹İ"·µ»Ø
  - MusicPlayer ĞÂÔö `initialId` prop£º½øÈë¼´²¥·ÅÖ¸¶¨ÇúÄ¿¶ø·ÇÁĞ±íµÚÒ»Ê×
- Â·ÓÉ£º/music children ¹Ò `player/:id`
### 20.4 ÑéÖ¤
- build Í¨¹ı£»/music /music/player/1 /media / È«²¿ 200
## 21. ĞÇÍ¾Ğü¸¡ÇòĞŞ¸´ + hero Áô°× + Ëµ»°·¢¹â£¨±¾»á»°Íê³É£©
### 21.1 Ğü¸¡Çò£¨XingTuAssistant.vue£©
- Ä¬ÈÏÎ»ÖÃ£ºµ×²¿¾ÓÖĞ ¡ú ÓÒ²à£¨container fixed bottom:24px right:24px£¬panel ÔÚÇòÉÏ·½Õ¹¿ª£©
- ĞŞ¸´"µã»÷ºóÏûÊ§"£ºÍÏ¶¯ãĞÖµ 4px¡ú6px£¬pointerup Ê±ÓÃ×ÜÎ»ÒÆ(Math.abs(dx)+Math.abs(dy)>6)¶ş´ÎÅĞ¶¨£¬½öÕæÍÏ¶¯²ÅÌù±ßÎü¸½£»µã»÷²»ÔÙ±»ÎóÅĞÎªÍÏ¶¯¶øË²ÒÆµ½±ßÔµ
- Ä£°åË³Ğòµ÷Õû£ºpanel ·Å orb Ö®Ç°£¨ÇòÏÂ·½/Ãæ°åÉÏ·½µ¯³ö£¬²»ÔÙÕÚµ²Çò£©
- onOrbDragEnd ´«ÊÂ¼ş¶ÔÏó£¬ĞŞ¸´ window pointerup ¶µµ×·ÖÖ§µÄ e.clientX ±¨´íÒş»¼
### 21.2 Ê×Ò³ hero ÖĞ²¿Áô°×£¨GhibliHome.vue£©
- 3D ÖÕ¶Ë margin-bottom 2rem¡ú0.75rem£»badge margin-bottom 1.5rem¡ú1rem£»subtitle margin-bottom 0.9rem¡ú0.75rem£¬ÖĞ²¿ÊÓ¾õ½ô´Õ
### 21.3 Ëµ»°ÉÁË¸·¢¹â
- speak(): utter.onboundary ¡ú pulseOrbCore()£ºorb-core ¼Ó orb-pulse Àà£¨scale2+brightness2+ÇàÀ¶¹âÔÎ 0.3s Âö³å£¬restart ·À¶¶£©
- .orb-speaking ×´Ì¬ÔöÇ¿£ºorb Íâ¿ò¸ßÁÁ + Ë«¹â»·¼ÓËÙ + coreGlow ³£ÁÁºôÎü
### 21.4 ÑéÖ¤
- build Í¨¹ı£¬/ /music /media 200
## 22. È¨ÏŞÏµÍ³ + ĞÇÍ¾µã»÷ĞŞ¸´ + Í·ÏñÏÂÀ­ + È«¾Ö¹â±ê£¨±¾»á»°Íê³É£©
### 22.1 ĞÇÍ¾"µã»÷ÏûÊ§"¸ù³ı£¨XingTuAssistant.vue ÖØ¹¹£©
- ¶¨Î»Ä£ĞÍÖØ¹¹£ºÈİÆ÷¹Ì¶¨Îª 56px Ãªµã£¨Ä¬ÈÏÓÒÏÂ½Ç£©£¬ÍÏ¶¯¸ÄÈİÆ÷ left/top£»orb Ö»¸ºÔğ hover/click£¬»¥²»¸ÉÈÅ
- panel ¸Ä absolute£¨¹ÒÔÚÇòÉÏ·½£©£¬²»ÔÙ²ÎÓë flex ²¼¾Ö£»click ¸Ä orb Ô­ÉúÊÂ¼ş + suppressClickOnce£»pointer capture + 8px ãĞÖµ
### 22.2 ÓÃ»§·şÎñÈ¨ÏŞ£¨ºó¶Ë£©
- user ±íĞÂÔö permissions ÁĞ£¨¶ººÅ·Ö¸ôÈ¨ÏŞÂë£»NULL/¿Õ=Ä¬ÈÏ¼¯£©£»init 02-web3_user.sql Í¬²½
- UserVO ¼Ó permissions; GET/PUT /user/{id}/permissions£¨requireAdmin£©
- admin-service ĞŞ¸´Áã¼øÈ¨£º/admin/dashboard|logs|visit/list|visit/summary ¼Ó requireAdmin£¨POST /admin/visit Âñµã±£³Ö¹«¿ª£©
### 22.3 Ç°¶ËÈ¨ÏŞÌåÏµ
- utils/permissions.js£ºDEFAULT_PERMS£¨home/community/media/music/resources/album/link/comments/about£¬×¢²á¼´ÓµÓĞ£©¡¢·şÎñ×é£¨shop/quant/tools/software£¬Ğè¹ÜÀíÔ±ÊÚÈ¨£©¡¢hasPerm/effectivePerms
- Â·ÓÉ meta.perm È«Á¿±ê×¢£»ÊØÎÀ£ºÎŞÈ¨ÏŞ¡ú»ØÊ×Ò³£»requiresAdmin Ôö¼Ó xingtu_admin_gate£¨sessionStorage£©Ğ£Ñé¡ª¡ª¹ÜÀí½çÃæÖ»ÄÜ´Ó"·şÎñ"²Ëµ¥½øÈë£¬Ö±½ÓÊä URL ±»À¹
- Web3Nav£ºÖ÷µ¼º½/·şÎñ/¸ü¶à/ÒÆ¶¯¶Ë°´ hasPerm ¹ıÂËÏÔÊ¾£»¹ºÎï³µÍ¼±êĞè shop È¨ÏŞ£»"¹ÜÀí·şÎñ"Èë¿Úµã»÷Ğ´ gate£»ÍË³ö/µÇ³öÇå gate
- admin/users.vue£ºĞÂ"È¨ÏŞ"ÁĞ£¨ÏÔÊ¾ÒÑÊÚ·şÎñÈ¨ÏŞ±êÇ©£©+ È¨ÏŞÉèÖÃµ¯´°£¨·Ö×é¹´Ñ¡£¬±£´æ PUT /user/{id}/permissions£©£»Admin ÕËºÅÏÔÊ¾"È«²¿"
### 22.4 Í·ÏñÏÂÀ­¿ò£¨Web3Nav.vue£©
- ´¿ CSS group-hover ¡ú µã»÷ÇĞ»» + ÎÄµµµã»÷¹Ø±Õ£¨ÓëÖ÷ÌâÇĞ»»Æ÷Ò»ÖÂ£©£¬ĞŞ¸´ĞüÍ£Ñ¡ÖĞÊ±ÏÂÀ­ÏûÊ§
### 22.5 È«¾Ö¹â±êÌØĞ§£¨GlowCursor.vue ÖØĞ´£©
- ÏµÍ³¹â±êÒş²Ø£¨html.cursor-hidden * cursor:none£¬½ö¾«Ï¸Ö¸ÕëÉè±¸£©£»Ç¨ÒÆÖÁ Web3Layout È«Õ¾ÉúĞ§£¨Ê×Ò³ÒÆ³ıÖØ¸´¹ÒÔØ£©
- ÌØĞ§£ºÖĞĞÄ°×µã(¿ìËÙ)+ºôÎü¹â»·(ÂıËÙ)+6 Á£×ÓÍÏÎ²(´í·åË¥¼õ)£»hover Á´½Ó/°´Å¥·Å´ó±äÉ«£»°´ÏÂÊÕËõÂö³å£»±£Áô data-magnetic ´ÅÎü
### 22.6 ÑéÖ¤
- mvn ¹¹½¨ user/admin ³É¹¦£»permissions ½Ó¿Ú E2E£ºset [quant,tools]¡úGET ·µ»ØÏàÍ¬£»ÎŞ token ·ÃÎÊ /user/list¡¢/admin/dashboard ¾ù 401
- Ç°¶Ë build Í¨¹ı£¬/ /music /shop /quant /admin /login 200£»ÒÑÊÚÈ¨ÑùÀı£ºalice=quant,tools
## 23. ¹â±ê¸ÄÎª¿Æ»Ã¼ıÍ·£¨±¾»á»°Íê³É£©
- GlowCursor.vue ÖØĞ´£ºÈ¥µôÔ²µã/Ô²»·
- Ö÷¹â±ê£½ÇĞ½Ç¿Æ¼¼¼ıÍ· SVG£¨Çà¡ú×ÏÄÜÁ¿½¥±äÌî³ä + ÇàÉ«Ãè±ß + Ë«²ã»Ô¹â£©£¬¼â¶Ë¾«È·¶Ô×¼Ö¸Õë
- hover Á´½Ó/°´Å¥£º¼ıÍ··Å´ó 1.18 + ¹âÔÎÔöÇ¿£¬Í¬Ê±Õ¹¿ª HUD ËÄ½Ç¿Ì¶È£¨×óÉÏ/ÓÒÉÏ/×óÏÂ/ÓÒÏÂ L ĞÎÃé×¼¿ò£¬·ÇÔ²ĞÎ£©
- °´ÏÂ£º¼ıÍ·ÊÕËõÂö³å + ÁÁ¶ÈÉÁ
- ±£Áô 5 ¸ö·¢¹âÍÏÎ²Á£×Ó£¨Çà×Ï½¥±ä£¬´í·åË¥¼õ£©
- ÏµÍ³¹â±ê¼ÌĞøÈ«¾ÖÒş²Ø£¨html.cursor-hidden£©£¬½ö¾«Ï¸Ö¸ÕëÉè±¸ÉúĞ§
## 24. ÏµÍ³¹â±ê¸ü»»ÎªËŞÙĞ»ğÑæ¼ı + Á£×ÓÍÏÎ²£¨±¾»á»°Íê³É£©
- ÓÃ»§ËØ²Ä£ºOneDrive Desktop µÄ "Jujutsu Kaisen Sukuna Flame Arrow & Hand" .cur/.png£¨128x128£¬PNG ĞÍ¹â±ê£©
- ¸´ÖÆ½ø frontend/public/cursors/£¨¸ÄÃû sukuna-flame-arrow.cur/.png£¬Ô­Ãûº¬¿Õ¸ñ&ÀûÓÚÒıÓÃ£©
- GlowCursor.vue ÖØĞ´Îª´¿ÍÏÎ²×é¼ş£ºÈ¥µô×Ô»æ¼ıÍ·/Ô²»·/HUD ½Ç±ê
  - html.sukuna-cursor È«¾Ö CSS: cursor url(/cursors/sukuna-flame-arrow.cur) 12 24, auto !important£¨ÈÈµã°´ÄÚÈİ bbox ×óÉÏ¼ıÍ·¼â¹ÀËã (12,24)£©
  - ±£Áô 5 Á£Çà¡ú³ÈÄÜÁ¿ÍÏÎ²£¨Sukuna »ğÑæÅäÉ«£©´í·åË¥¼õ¸úËæ£»¾«È·Ö¸ÕëÉè±¸²ÅÆôÓÃ
- ÑéÖ¤£ºbuild Í¨¹ı£»/cursors/sukuna-flame-arrow.cur 200
## 25. ¹â±ê¸ÄÕı³£³ß´çäÖÈ¾ + »ğÑæ¶¯Ì¬£¨±¾»á»°Íê³É£©
- CSS cursor(.cur) ÎŞ·¨Ëõ·Å ¡ú ¸ÄÎª×é¼şäÖÈ¾ img£¨/cursors/sukuna-flame-arrow.png£©¸úËæÊó±ê£¬width 34px Õı³£´óĞ¡£¬ÈÈµã transform-origin (3,6)£¨Ô­Í¼ 12/128¡¢24/128 »»Ëã£©
- ¶¯Ì¬Ğ§¹û£º
  - ³£×¤»ğÑæÉÁË¸¶¯»­ flameFlicker£¨1.4s£ºÃ÷°µºôÎü + Î¢Ğı×ª°Ú¶¯ + ³Èºì»Ô¹âÇ¿Èõ±ä»¯£©
  - hover Á´½Ó/°´Å¥£ºÈ¼ÉÕ¼ÓËÙ£¨0.7s£©+ ·Å´ó 1.3 + ÁÁÑæ
  - °´ÏÂ£º»ğÑæÊÕÂ£Âö³å + ¸ßÁÁ
  - 6 Á£»ğÑæÍÏÎ²£¨³È¡úºì£©´í·å¸úËæ
- ÏµÍ³¹â±êÒş²Ø»Ö¸´ html.cursor-hidden
- build Í¨¹ı£¬Ò³Ãæ 200
## 26. ¹â±êÏûÊ§ĞŞ¸´£¨±¾»á»°Íê³É£©
- ¸ùÒòÅÅ²é£ºimg äÖÈ¾°æÒÀÀµ JS ¹ÒÔØ + cursor-hidden ÓÉ JS Ìí¼Ó£¬Èô×ÊÔ´/¼ÓÔØÒì³£Ôò"ÎŞ¹â±ê"
- ĞŞ¸´£º
  1. hidden ³õÊ¼ false£¨²»ÔÙµÈ mousemove ²ÅÏÔÊ¾£©£¬³õÊ¼Î»ÖÃÆÁÄ»ÄÚ (40,40)
  2. È¥µô isFine JS À¹½Ø£¨´¥ÆÁÓÉ CSS media query ´¦Àí£©£¬±£Ö¤×ÀÃæ±Ø×¢²áÊÂ¼ş
  3. mouseleave ÑÓ³Ù 600ms Òş²Ø£¨·ÀÖ¹ÇĞ´°ÎóÒş²Ø£©
  4. img @error ¶µµ×£ºÍ¼Æ¬¼ÓÔØÊ§°Ü£¨²¿Êğ»·¾³È±×ÊÔ´£©¡ú ÒÆ³ı cursor-hidden£¬»Ö¸´ÏµÍ³¹â±ê£¬±ÜÃâÍêÈ«ÎŞ¹â±ê
- ÌáĞÑ£ºĞè·ÃÎÊĞÂ build£¨4173 preview ÒÑÖØÆô£©£»Éú²ú/Íø¹Ø»·¾³ĞèÍ¬²½²¿Êğ public/cursors
## 28. ÏîÄ¿ÎÄ¼şÏµÍ³ÕûÀí£¨±¾»á»°Íê³É£©
### 28.1 É¾³ıµÄÊµÑé/¹Â¶ùÄÚÈİ
- frontend/city/£¨React ×ÓÓ¦ÓÃ 46MB£¬²»¿É¹¹½¨£©+ vite.city.config.ts + package.json "city" ½Å±¾
- frontend/frontend/£¨Ç¶Ì×¹Â¶ù¸±±¾ 8MB£¬È«²ÖÁãÒıÓÃ£©
- vite.config.js ÒÆ³ı optimizeDeps.entries »ØÍË£¨city É¾³ıºó³ÉÎª¶àÓà£©
### 28.2 Ç°¶ËËÀ´úÂë
- views/home/index.vue + AdventureScene.vue£¨¾ÉÊ×Ò³£¬Â·ÓÉÒÑÓÃ GhibliHome£©
- src/modules/ 8 ¸ö¿ÕÄ¿Â¼¡¢utils/constants¡¢utils/helpers¡¢assets/particles¡¢assets/images£¨È«¿Õ£©
- stores/modules/particle.js ¿Õ stub£¨Í¬²½ÒÆ³ı App.vue ÖĞ initParticles() µ÷ÓÃ£©
### 28.3 ÒÀÀµÇåÀí£¨package.json£©
- ÒÆ³ıÎ´Ê¹ÓÃÒÀÀµ£º@vueuse/core¡¢howler¡¢motion¡¢particles.js¡¢video.js£¨È«²ÖÁã import ÑéÖ¤£©
- ÒÆ³ı²»¿ÉÓÃ lint ½Å±¾£¨eslint Î´°²×°£©£».npmrc ¿ÕÎÄ¼şÉ¾³ı
### 28.4 »º´æ/ÈÕÖ¾/¹¹½¨²úÎï
- frontend/.npm-cache(441MB)¡¢.vite-cache(25MB)¡¢logs£»backend È«²¿ *.log/*.txt¡¢target/ Ä¿Â¼(753MB)
- ¸ù run.log/run-err.log/screenshot.png£»quant-py-service logs
- Á½¸öÈÕÖ¾£¨vite-dev.log¡¢backend/pkg-gw3.log£©±» IntelliJ ¾ä±úÕ¼ÓÃ£¬ÒÑÓÉ¸ù .gitignore *.log ºöÂÔ
### 28.5 ½Å±¾È¥ÖØ
- É¾³ı start-all.bat¡¢start-services.bat¡¢start-local.ps1£¨±£Áô RUN.md ÎÄµµ»¯µÄ start-all-services.bat/ps1£©
- É¾³ıÒ»´ÎĞÔ½Å±¾£ºsetup-local.mjs¡¢export-artifacts.mjs¡¢quant_insert.py¡¢backend/start-jarvis.bat
### 28.6 ÆäËû
- ĞÂÔö¸ù .gitignore£¨*.log¡¢target/¡¢»º´æ¡¢.env¡¢.idea µÈ£©
- docs/ Á½´¦¹ıÆÚÃèÊö¸üĞÂ£¨AdventureScene ÒÑÉ¾£©
- ÑéÖ¤£ºbuild Í¨¹ı£¬/ /music /media /three /admin /tools È«²¿ 200
## 29. È«Á¿×¢ÊÍ + ÏîÄ¿ÎÄµµ£¨±¾»á»°Íê³É£©
### 29.1 ´úÂë×¢ÊÍ£¨244 ¸öÎÄ¼ş£¬Ö»¼Ó×¢ÊÍ²»¸ÄÂß¼­£©
- Ç°¶Ë 84 ¸öÎÄ¼ş£¨src/api¡¤stores¡¤router¡¤utils¡¤directives¡¤components¡¤layouts¡¤views£©£ºÎÄ¼şÍ·ºá·ù×¢ÊÍ + º¯Êı/½Ó¿Ú×¢ÊÍ + Ä£°å¹Ø¼üÇø×¢ÊÍ
- ºó¶Ë 160 ¸ö Java ÎÄ¼ş£¨12 ·şÎñ + common 3 Ä£¿é£©£ºÀà¼¶ Javadoc£¨Ä£¿é/Ö°Ôğ/Â·ÓÉÇ°×º£©+ ·½·¨×¢ÊÍ£¨º¬Ç°¶Ëµ÷ÓÃÂ·ÓÉ£©+ Entity ±ê×¢Êı¾İ¿â±í
- ÑéÖ¤£ºÇ°¶Ë build 11.7s Í¨¹ı£»ºó¶Ë mvn compile ·Ö×é BUILD SUCCESS£¨gateway/admin/user ¡¤ blog/forum/media/shop ¡¤ quant/tool/software/resource/jarvis£©
### 29.2 ÎÄµµ£¨ĞÂÔö/ÖØĞ´£©
- README.md£¨¸ù£©£º¼¼ÊõÕ»¡¢Ä¿Â¼½á¹¹¡¢¹¦ÄÜÄ£¿é±í¡¢3 ÖÖÆô¶¯·½Ê½¡¢¶Ë¿ÚÇåµ¥¡¢µ÷ÓÃÁ´Â·¡¢SQL ³õÊ¼»¯¡¢ÎÄµµË÷Òı
- frontend/README.md£¨ÖØĞ´£©£º¼¼ÊõÕ»¡¢src È«Ä¿Â¼Ê÷º¬Ã¿Ä¿Â¼Ö°Ôğ¡¢Â·ÓÉÒ³ÃæÇåµ¥¡¢³£ÓÃÃüÁî¡¢¿ª·¢´úÀí±í¡¢¹Ø¼üÔ¼¶¨£¨ÇëÇó·â×°/È¨ÏŞ/È«¾ÖÌØĞ§/¶¯Ğ§Ö¸Áî/ÑùÊ½ÌåÏµ£©
- backend/README.md£¨ĞÂÔö£©£º¼Ü¹¹Í¼£¨Nacos ×¢²á/ÅäÖÃ£©¡¢12 ·şÎñÇåµ¥£¨¶Ë¿Ú/Êı¾İ¿â/Ö°Ôğ£©¡¢common Ä£¿éËµÃ÷¡¢Íø¹ØÂ·ÓÉ±í¡¢¹¹½¨ÓëÆô¶¯ 5 ²½¡¢·şÎñ·Ö²ã½á¹¹¡¢¼øÈ¨ËµÃ÷
- ĞŞÕı RUN.md ÒÅÁô´íÎó£ºÍø¹Ø¶Ë¿Ú 8099 ¡ú Êµ¼Ê 8080£¨gateway/application.yml Óë vite ´úÀí¡¢docker-compose Ò»ÖÂ£©
### 29.3 ÆäËûÇåÀí
- É¾³ı¿ÕÄ¿Â¼£ºsrc/composables£¨useApi/useAuth£©¡¢src/plugins