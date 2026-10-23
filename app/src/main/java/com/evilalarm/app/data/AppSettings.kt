package com.evilalarm.app.data

import com.evilalarm.app.alarm.SoundCatalog

enum class Difficulty(val label: String) {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard"),
}

/** What you have to do to turn a ringing alarm off. */
enum class DismissMethod(val label: String) {
    MATH("Math"),
    QR("QR code"),
    SHAKE("Shake"),
}

/** What you have to do to end an app block early. */
enum class BlockerUnlockMethod(val label: String) {
    MATH("Math"),
    QR("QR code"),
}

data class AppSettings(
    val soundId: String = SoundCatalog.EVIL_ALARM_ID,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val dismissMethod: DismissMethod = DismissMethod.MATH,
    /** Contents of the QR code or barcode to scan when [dismissMethod] is [DismissMethod.QR]. */
    val qrCode: String? = null,
    val snoozeMinutes: Int = 5,
    /** After you turn the alarm off, check you're still awake a few minutes later. */
    val wakeCheck: Boolean = true,
    /** Hours before the day's first alarm to send a bedtime reminder; 0 means off. */
    val bedtimeHours: Int = 8,
    /** Stored now; enforced in phase 4. */
    val hardMode: Boolean = false,
    /** How long the first alarm of the day locks [blockedPackages]. */
    val blockMinutes: Int = 30,
    val blockedPackages: Set<String> = DEFAULT_BLOCKED_PACKAGES,
    val blockerUnlock: BlockerUnlockMethod = BlockerUnlockMethod.MATH,
    /** Difficulty of the problems that end a block early when [blockerUnlock] is [BlockerUnlockMethod.MATH]. */
    val blockerDifficulty: Difficulty = Difficulty.HARD,
    /**
     * Contents of the code to scan when [blockerUnlock] is [BlockerUnlockMethod.QR]. Never the same
     * as [qrCode], so one scan can't both stop the alarm and end the block.
     */
    val blockerCode: String? = null,
) {
    companion object {
        val DEFAULT_BLOCKED_PACKAGES = setOf(
            "com.instagram.android",
            "com.zhiliangtech.tiktok", // TikTok (most regions)
            "com.ss.android.ugc.trill", // TikTok (some Asian regions)
        )
    }
}
