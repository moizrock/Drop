package com.example

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.catalog.DeviceCatalogScanner
import com.example.core.crypto.CryptoPairingEngine
import com.example.core.deltasync.DeltaSyncChunker
import com.example.core.p2p.WifiDirect5GhzManager
import com.example.core.persistence.TransferCheckpointRepository
import com.example.core.persistence.VayuTransferDatabase
import com.example.core.streaming.TransferStreamEngine
import com.example.core.vault.DuplicateShieldVaultEngine
import com.example.core.voice.VayuVoiceAssistantEngine
import com.example.domain.model.NonDfs5GhzChannel
import com.example.domain.model.PickerMediaCategory
import com.example.domain.model.VayuTab
import com.example.ui.components.ShareMePickerTab
import com.example.ui.components.SystemMenuScreen
import com.example.ui.viewmodel.VayuDropViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `verify WarpDrop app_name official speed logo drawable and NEARBY_WIFI_DEVICES permission model`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("WarpDrop", appName)
        assertEquals("WarpDrop", context.getString(R.string.app_title))
        assertEquals("Ultra-fast file transfer without internet", context.getString(R.string.splash_tagline))
        assertEquals("Transfer", context.getString(R.string.transfer_tab))
        assertEquals("Phone Clone", context.getString(R.string.clone_tab))
        assertEquals("AI Shield", context.getString(R.string.shield_tab))
        assertEquals("Media Hub", context.getString(R.string.hub_tab))
        assertEquals("Diagnostics", context.getString(R.string.telemetry_tab))
        assertEquals("SEND", context.getString(R.string.btn_send))
        assertEquals("RECEIVE", context.getString(R.string.btn_receive))
        assertEquals("Switching to a New Phone?", context.getString(R.string.clone_banner))
        assertEquals(
            "Transfer your WhatsApp chats, contacts, photos, and apps wirelessly.",
            context.getString(R.string.clone_sub)
        )
        assertEquals("START 5 GHz PHONE CLONE", context.getString(R.string.btn_start_clone))
        assertEquals("com.aistudio.warpdrop", context.packageName)

        // Verify full-bleed adaptive icon background, scaled foreground, and official speed logo drawable
        val bgDrawable = context.getDrawable(R.drawable.ic_launcher_background)
        val fgDrawable = context.getDrawable(R.drawable.ic_launcher_foreground)
        val logoDrawable = context.getDrawable(R.drawable.ic_warpdrop_speed_logo)
        assertNotNull(bgDrawable)
        assertNotNull(fgDrawable)
        assertNotNull(logoDrawable)
        assertTrue(fgDrawable!!.intrinsicWidth > 0)
        assertTrue(fgDrawable.intrinsicHeight > 0)

        val splashWindowDrawable = context.getDrawable(R.drawable.bg_warpdrop_splash_window)
        assertNotNull(splashWindowDrawable)

        // Verify AndroidManifest.xml points application and MainActivity icon to @mipmap/ic_launcher adaptive icon
        val appInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
        assertEquals(R.mipmap.ic_launcher, appInfo.icon)
        val activityInfo = context.packageManager.getActivityInfo(
            android.content.ComponentName(context, MainActivity::class.java),
            0
        )
        assertEquals(R.mipmap.ic_launcher, activityInfo.icon)

        val tabLabels = VayuTab.entries.map { it.label }
        assertEquals(
            listOf("Transfer", "Phone Clone", "AI Shield", "Media Hub", "Diagnostics"),
            tabLabels
        )

        val pickerLabels = PickerMediaCategory.entries.map { it.label }
        assertEquals(
            listOf("Photos", "Videos", "Music", "Files", "Apps"),
            pickerLabels
        )

        val p2pManager = WifiDirect5GhzManager(context)
        assertEquals(Manifest.permission.NEARBY_WIFI_DEVICES, p2pManager.getRequiredRuntimePermission())
        assertTrue(p2pManager.isZeroGpsNearbyPermissionModel())
    }

    @Test
    fun `verify CryptoPairingEngine X25519 keypair SPAKE2 6-digit PIN mp2p URI and HKDF session key`() {
        val hostEngine = CryptoPairingEngine()
        val clientEngine = CryptoPairingEngine()

        val (ssid, psk) = hostEngine.generate5GhzCredentials(NonDfs5GhzChannel.CH_149)
        val hostSession = hostEngine.generateEphemeralPairingSession(
            ssid = ssid,
            passphrase = psk,
            channel = NonDfs5GhzChannel.CH_149
        )
        val clientSession = clientEngine.generateEphemeralPairingSession(
            ssid = ssid,
            passphrase = psk,
            channel = NonDfs5GhzChannel.CH_149
        )

        assertTrue(hostSession.mp2pUriPayload.startsWith("mp2p://connect"))
        assertEquals(6, hostSession.spake2PinCode.length)
        assertTrue(hostEngine.verifySpake2Pin(hostSession.spake2PinCode, hostSession))
        assertEquals(37, hostSession.qrMatrix.size)

        val parsed = clientEngine.parseAndVerifyMp2pUri(hostSession.mp2pUriPayload).getOrThrow()
        assertTrue(parsed.isSignatureValid)
        assertEquals(ssid, parsed.ssid)
        assertEquals(149, parsed.channelNumber)

        val hostDerivedKey = hostEngine.deriveSharedSessionKeyHex(clientSession.publicKeyBase64Url).getOrThrow()
        val clientDerivedKey = clientEngine.deriveSharedSessionKeyHex(hostSession.publicKeyBase64Url).getOrThrow()
        assertNotNull(hostDerivedKey)
        assertEquals(hostDerivedKey, clientDerivedKey)
    }

    @Test
    fun `verify DeviceCatalogScanner Photos 4 pill count and PhoneClone consumer checklists`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scanner = DeviceCatalogScanner(context)
        val storageMetrics = scanner.readStoragePartitionMetrics()
        assertTrue(storageMetrics.dataTotalBytes > 0L)
        assertTrue(storageMetrics.dataAvailableBytes >= 0L)

        val catalog = scanner.scanAllCategories()
        assertEquals(5, catalog.keys.size)
        val photosList = catalog[PickerMediaCategory.PHOTOS].orEmpty()
        assertEquals(4, photosList.size)
        assertEquals("Photos 4", "${PickerMediaCategory.PHOTOS.label} ${photosList.size}")
        assertFalse(catalog[PickerMediaCategory.VIDEOS].isNullOrEmpty())
        assertFalse(catalog[PickerMediaCategory.MUSIC].isNullOrEmpty())
        assertFalse(catalog[PickerMediaCategory.DOCUMENTS].isNullOrEmpty())

        val vaultEngine = DuplicateShieldVaultEngine(context)
        val cloneChecklists = vaultEngine.inspectDeviceCloneCategories()
        assertEquals(
            listOf("WhatsApp Chats", "Contacts", "Photos & Videos", "Installed Apps"),
            cloneChecklists.map { it.category.title }
        )
    }

    @Test
    fun `verify Multi-Language Voice Assistant TTS prompt WhatsApp Scoped Storage dialog and Live Transfer Overlay`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = VayuDropViewModel(app)

        // 1. Verify Default International English Voice Assistant prompt text & Hindi Locale fallback
        assertEquals(
            "Tap SEND to share files or scan QR code to connect.",
            VayuVoiceAssistantEngine.ENGLISH_SPEECH_PHRASE
        )
        assertEquals(
            "🔊 WarpDrop Voice Assistant: Tap SEND to share files or scan QR code to connect.",
            VayuVoiceAssistantEngine.DEFAULT_TRANSFER_GUIDE_TEXT
        )
        assertEquals(
            "🔊 WarpDrop Voice Assistant: फाइल भेजने के लिए Send दबाएं या दूसरे फोन का QR कोड स्कैन करें।",
            VayuVoiceAssistantEngine.HINDI_TRANSFER_GUIDE_TEXT
        )

        // Default English locale test
        viewModel.triggerHindiVoiceGuidance(java.util.Locale.US)
        assertTrue(viewModel.uiState.value.isVoiceAssistantSpeaking)
        assertEquals(VayuTab.TRANSFER, viewModel.uiState.value.activeTab)
        assertFalse(viewModel.uiState.value.isPairingModalVisible)
        assertEquals(
            "🔊 WarpDrop Voice Assistant: Tap SEND to share files or scan QR code to connect.",
            viewModel.uiState.value.voiceAssistantBannerText
        )

        // Hindi locale test
        viewModel.triggerHindiVoiceGuidance(java.util.Locale("hi", "IN"))
        assertEquals(
            "🔊 WarpDrop Voice Assistant: फाइल भेजने के लिए Send दबाएं या दूसरे फोन का QR कोड स्कैन करें।",
            viewModel.uiState.value.voiceAssistantBannerText
        )

        viewModel.dismissVoiceGuidanceBanner()
        assertFalse(viewModel.uiState.value.isVoiceAssistantSpeaking)
        assertEquals(null, viewModel.uiState.value.voiceAssistantBannerText)

        // 2. Verify Interactive WhatsApp Scoped Storage Permission Dialog & Authorize button flow
        assertEquals(
            "Android/media/com.whatsapp/WhatsApp/Databases",
            viewModel.uiState.value.whatsAppScopedStoragePath
        )
        viewModel.openWhatsAppPermissionDialog()
        assertTrue(viewModel.uiState.value.isWhatsAppPermissionSheetVisible)
        viewModel.authorizeAndStageWhatsAppBackup()
        assertFalse(viewModel.uiState.value.isWhatsAppPermissionSheetVisible)
        assertTrue(viewModel.uiState.value.isWhatsAppBackupAuthorized)
        assertEquals(
            "WhatsApp msgstore.db.crypt15 database staged for zero-loss transfer.",
            viewModel.uiState.value.statusBannerMessage
        )

        // 3. Verify [START 5 GHz PHONE CLONE] launches 2.60 GB 4-item Live Transfer Progress Overlay
        viewModel.startFullPhoneCloneMigration()
        assertTrue(viewModel.uiState.value.isTransferProgressOverlayVisible)
        assertEquals(4, viewModel.uiState.value.transferQueue.size)
        assertEquals(
            "2.60 GB",
            VayuDropViewModel.formatBytes(viewModel.uiState.value.transferQueue.sumOf { it.sizeBytes })
        )

        // 4. Verify Hero [SEND] immediately opens Full-Screen 2.60 GB Live Transfer Progress Overlay
        viewModel.setTransferProgressOverlayVisible(false)
        viewModel.triggerHeroSendAndOpenOverlay()
        assertTrue(viewModel.uiState.value.isTransferProgressOverlayVisible)
        assertEquals(4, viewModel.uiState.value.transferQueue.size)
        assertEquals(
            "2.60 GB",
            VayuDropViewModel.formatBytes(viewModel.uiState.value.transferQueue.sumOf { it.sizeBytes })
        )

        // 5. Verify Pause/Resume state toggle
        viewModel.pauseAllQueuedTransfers()
        assertTrue(viewModel.uiState.value.isTransferPaused)
        viewModel.startAllQueuedTransfers()
        assertFalse(viewModel.uiState.value.isTransferPaused)

        // 6. Verify Step A ShareMe-Style 6-Tab "Select items" Picker & Unified Master Front Page History Switcher
        assertEquals(
            listOf("Recent", "Files", "Videos", "Photos", "APPS", "Music"),
            ShareMePickerTab.entries.map { it.title }
        )
        viewModel.openSelectItemsPicker(ShareMePickerTab.APPS)
        assertTrue(viewModel.uiState.value.isSelectItemsPickerVisible)
        assertEquals(ShareMePickerTab.APPS, viewModel.uiState.value.activeSelectItemsTab)
        viewModel.closeSelectItemsPicker()
        assertFalse(viewModel.uiState.value.isSelectItemsPickerVisible)

        viewModel.setFrontPageHistoryMode(true)
        assertTrue(viewModel.uiState.value.isFrontPageHistoryMode)
        viewModel.setHistoryReceivedFilter(false)
        assertFalse(viewModel.uiState.value.isHistoryReceivedFilter)
        viewModel.setFrontPageHistoryMode(false)
        assertFalse(viewModel.uiState.value.isFrontPageHistoryMode)

        // 7. Verify Step B 5 Systematic "☰" Menu Screens (Personal Info, Share to PC + Additional Settings, Webshare, Share with iOS, Settings & About)
        viewModel.openSystemMenu()
        assertTrue(viewModel.uiState.value.isSystemMenuOverlayVisible)
        assertEquals(SystemMenuScreen.DRAWER_HOME, viewModel.uiState.value.activeSystemMenuScreen)

        viewModel.navigateSystemMenu(SystemMenuScreen.PERSONAL_INFO)
        assertEquals(SystemMenuScreen.PERSONAL_INFO, viewModel.uiState.value.activeSystemMenuScreen)
        viewModel.saveUserProfile("Pixel 9 Pro XL", 5)
        assertEquals("Pixel 9 Pro XL", viewModel.uiState.value.customProfileNickname)
        assertEquals(5, viewModel.uiState.value.selectedAvatarIndex)

        viewModel.navigateSystemMenu(SystemMenuScreen.SHARE_TO_PC)
        assertEquals(SystemMenuScreen.SHARE_TO_PC, viewModel.uiState.value.activeSystemMenuScreen)
        viewModel.togglePcFtpServer(true)
        assertTrue(viewModel.uiState.value.isFtpServerRunning)
        assertEquals("ftp://192.168.1.5:2121", viewModel.uiState.value.ftpServerAddress)

        viewModel.navigateSystemMenu(SystemMenuScreen.SHARE_TO_PC_SETTINGS)
        assertEquals(SystemMenuScreen.SHARE_TO_PC_SETTINGS, viewModel.uiState.value.activeSystemMenuScreen)
        viewModel.setFtpKeepDisplayAwake(false)
        assertFalse(viewModel.uiState.value.ftpKeepDisplayAwake)
        viewModel.setFtpTransferEncoding("GBK")
        assertEquals("GBK", viewModel.uiState.value.ftpTransferEncoding)
        viewModel.setFtpPortNumber(2222)
        assertEquals(2222, viewModel.uiState.value.ftpPortNumber)
        assertEquals("ftp://192.168.1.5:2222", viewModel.uiState.value.ftpServerAddress)
        viewModel.setFtpCredentials("moiz", "secret5g")
        assertFalse(viewModel.uiState.value.ftpAnonymousSignIn)
        assertEquals("moiz", viewModel.uiState.value.ftpUsername)

        viewModel.navigateSystemMenu(SystemMenuScreen.WEBSHARE)
        assertEquals(SystemMenuScreen.WEBSHARE, viewModel.uiState.value.activeSystemMenuScreen)

        viewModel.navigateSystemMenu(SystemMenuScreen.INVITE_SHARE_APP)
        assertEquals(SystemMenuScreen.INVITE_SHARE_APP, viewModel.uiState.value.activeSystemMenuScreen)

        viewModel.navigateSystemMenu(SystemMenuScreen.ABOUT_SETTINGS)
        assertEquals(SystemMenuScreen.ABOUT_SETTINGS, viewModel.uiState.value.activeSystemMenuScreen)
        viewModel.setDestinationFolder("/Internal storage/Download/WarpDrop")
        assertEquals("/Internal storage/Download/WarpDrop", viewModel.uiState.value.destinationFolder)
        viewModel.setLanguagePreference("English (Global)")
        assertEquals("English (Global)", viewModel.uiState.value.selectedLanguagePreference)
        viewModel.setUpdateDuringTransfers(false)
        assertFalse(viewModel.uiState.value.updateWarpDropDuringTransfers)

        viewModel.closeSystemMenu()
        assertFalse(viewModel.uiState.value.isSystemMenuOverlayVisible)
    }

    @Test
    fun `verify Phase 2 TransferStateDao WAL chunk bitmap pause and exact resume offset`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, VayuTransferDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = TransferCheckpointRepository(db.transferStateDao())
        val engine = TransferStreamEngine(repo)

        val tempTarget = File(context.cacheDir, "fat32_test.bin")
        val fat32Check = engine.validateAndPreallocateTarget(
            targetFile = tempTarget,
            expectedSizeBytes = 5_000_000_000L,
            forceSimulateFat32Mount = true
        )
        assertTrue(fat32Check is TransferStreamEngine.PreallocationCheckResult.Fat32SizeLimitExceeded)

        val totalBytes = 10L * 65536L
        val checkpoint = repo.createOrGetCheckpoint(
            transferId = "TEST-WAL-01",
            fileName = "payload.bin",
            sourceUriOrPath = "/tmp/src",
            targetFilePath = tempTarget.absolutePath,
            totalBytes = totalBytes,
            expectedFileSha256 = "ABC"
        )
        val bitmap = checkpoint.chunkBitmap.copyOf()
        for (idx in 0 until 4) {
            repo.recordVerifiedChunk("TEST-WAL-01", idx, "HASH_$idx", bitmap, 10)
        }
        repo.markPausedOrInterrupted("TEST-WAL-01", pausedByUser = true)

        val reloaded = repo.getCheckpoint("TEST-WAL-01")!!
        assertEquals(4, reloaded.verifiedChunks)
        assertEquals(4, reloaded.resumeChunkIndex)
        assertEquals(4L * 65536L, reloaded.resumeByteOffset)
        assertTrue(reloaded.isWithin30MinRecoveryWindow())
        db.close()
    }

    @Test
    fun `verify Phase 2 DeltaSyncChunker rolling hash CDC and Zstd Deflate delta reconstruction`() {
        val chunker = DeltaSyncChunker()
        val basis = ByteArray(128 * 1024) { i -> ((i * 23 + 7) and 0xFF).toByte() }
        val modified = basis.copyOf()
        for (i in 40_000 until 46_000) {
            modified[i] = ((i * 91 + 3) and 0xFF).toByte()
        }

        val report = chunker.generateAndVerifyDeltaPatch(
            fileName = "RealApkSlice.apk",
            basisBytes = basis,
            modifiedBytes = modified
        )
        assertTrue(report.reconstructedSha256Match)
        assertTrue(report.unchangedMatchedChunks > 0)
        assertTrue(report.bandwidthSavedPercent > 50f)
    }
}
