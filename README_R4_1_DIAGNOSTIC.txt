Junba AI Transcriber v3.8.2 APK R4.1 Diagnostic
================================================

目的
----
R4.1 專門修正 R4 診斷版的兩個盲點：
1. 不再以跨程序 SharedPreferences 作為主要錯誤傳遞方式，改用 App 私有實體檔案：
   - r41_last_stage.txt
   - r41_last_crash.txt
2. 完整轉錄 Activity 完全移除 attachBaseContext/fontScale 啟動覆寫。

三層診斷
--------
第 1 層：MainActivity 安全首頁（主程序）
第 2 層：TranscriberActivity Gate（:transcriber_gate），不碰 Whisper/Gemini
第 3 層：FullTranscriberActivity（:full_transcriber），才載入完整功能

如果第 3 層崩潰，即使 Java stack trace 沒留下，stage 檔仍會顯示最後走到哪一步。

測試順序
--------
1. 完整解除安裝舊版。
2. 安裝 Junba-R4.1-Diagnostic.apk。
3. 開啟 R4.1 Diagnostic Launcher。
4. 按「步驟 2：開啟獨立程序 Gate」。
5. Gate 正常後，按「下一步：載入完整轉錄功能」。
6. 若閃退/消失，回首頁按「重新讀取診斷」。
7. 按「複製診斷內容」，把完整 stage + crash 貼回 ChatGPT。

版本
----
versionCode: 389
versionName: 3.8.2-apk-r4.1-diagnostic
