package com.example.voice

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

class FloatingBubbleService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var windowManager: WindowManager? = null
    private var rootLayout: LinearLayout? = null
    private var roundBubbleFrame: FrameLayout? = null
    private var popupWindow: LinearLayout? = null
    private var micButton: TextView? = null
    private var statusBadgeDot: View? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var snapAnimator: ValueAnimator? = null

    private var isPopupVisible = false
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private var touchStartTime = 0L

    companion object {
        const val CHANNEL_ID = "voice_overlay_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_SHOW = "ACTION_SHOW"
        const val ACTION_HIDE = "ACTION_HIDE"

        fun setOverlayVisible(context: Context, visible: Boolean) {
            if (!Settings.canDrawOverlays(context)) return
            val intent = Intent(context, FloatingBubbleService::class.java).apply {
                action = if (visible) ACTION_SHOW else ACTION_HIDE
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {
                try { context.startService(intent) } catch (_: Exception) {}
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, FloatingBubbleService::class.java)
                context.stopService(intent)
            } catch (_: Exception) {}
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        acquireWakeLock()
        startInForeground()

        if (Settings.canDrawOverlays(this)) {
            initMessengerFloatingBubble()
            observeMicState()
        } else {
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> {
                rootLayout?.visibility = View.VISIBLE
            }
            ACTION_HIDE -> {
                isPopupVisible = false
                popupWindow?.visibility = View.GONE
                rootLayout?.visibility = View.GONE
            }
        }
        return START_STICKY
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "VoiceNook:GamingVoiceLock"
            )?.apply {
                acquire(12 * 60 * 60 * 1000L)
            }
        } catch (_: Exception) {}
    }

    private fun startInForeground() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Ferox - Audio en jeu",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Bulle vocale ronde Messenger active en jeu"
                }
                val manager = getSystemService(NotificationManager::class.java)
                manager?.createNotificationChannel(channel)
            }

            val openIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                openIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Ferox")
                .setContentText("Session vocale active en arrière-plan")
                .setSmallIcon(R.drawable.ic_bubble_logo)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {}
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initMessengerFloatingBubble() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutParamsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutParamsType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dpToPx(8)
            y = dpToPx(140)
        }

        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            visibility = View.VISIBLE
        }

        // 1. Messenger-Style Round Bubble (56dp circle) with NEW LOGO
        val bubbleSize = dpToPx(56)
        roundBubbleFrame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(bubbleSize, bubbleSize)

            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#0B0E17"))
                setStroke(dpToPx(2.5f), Color.parseColor("#10B981"))
            }
            elevation = dpToPx(12).toFloat()
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }

            // User's New Uploaded Avatar Logo
            val avatarView = ImageView(this@FloatingBubbleService).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                setImageResource(R.drawable.ic_bubble_logo)
                scaleType = ImageView.ScaleType.FIT_CENTER
                val p = dpToPx(4)
                setPadding(p, p, p, p)
                clipToOutline = true
                outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setOval(0, 0, view.width, view.height)
                    }
                }
            }
            addView(avatarView)

            // Small mic status badge dot
            val dotSize = dpToPx(14)
            statusBadgeDot = View(this@FloatingBubbleService).apply {
                layoutParams = FrameLayout.LayoutParams(dotSize, dotSize).apply {
                    gravity = Gravity.BOTTOM or Gravity.END
                    setMargins(0, 0, dpToPx(2), dpToPx(2))
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#10B981"))
                    setStroke(dpToPx(1.5f), Color.parseColor("#0B0E17"))
                }
            }
            addView(statusBadgeDot)
        }
        rootLayout?.addView(roundBubbleFrame)

        // 2. Petite Fenêtre (Ultra-compact mini popover window)
        val popupWidth = dpToPx(125)
        popupWindow = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            val pad = dpToPx(4)
            setPadding(pad, pad, pad, pad)
            layoutParams = LinearLayout.LayoutParams(popupWidth, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dpToPx(4)
            }

            background = GradientDrawable().apply {
                setColor(Color.parseColor("#121726"))
                cornerRadius = dpToPx(12).toFloat()
                setStroke(dpToPx(1f), Color.parseColor("#10B981"))
            }
            elevation = dpToPx(12).toFloat()
        }

        // Action Button: "Couper micro" / "Activer micro"
        micButton = TextView(this).apply {
            text = "Couper micro"
            setTextColor(Color.WHITE)
            textSize = 11.5f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(dpToPx(6), dpToPx(8), dpToPx(6), dpToPx(8))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            background = GradientDrawable().apply {
                setColor(Color.parseColor("#EF4444"))
                cornerRadius = dpToPx(8).toFloat()
            }

            setOnClickListener {
                VoiceStateHolder.toggleMic(this@FloatingBubbleService)
            }
        }
        popupWindow?.addView(micButton)
        rootLayout?.addView(popupWindow)

        // Dragging & Edge-Snapping Touch Listener
        roundBubbleFrame?.setOnTouchListener { _, event ->
            val screenWidth = resources.displayMetrics.widthPixels
            val screenHeight = resources.displayMetrics.heightPixels

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    snapAnimator?.cancel()
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    touchStartTime = System.currentTimeMillis()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()

                    if (abs(deltaX) > 8 || abs(deltaY) > 8) {
                        isDragging = true
                        // Constrain dragging within screen bounds so it never gets lost or stuck
                        val newX = (initialX + deltaX).coerceIn(-dpToPx(4), screenWidth - bubbleSize + dpToPx(4))
                        val newY = (initialY + deltaY).coerceIn(dpToPx(40), screenHeight - dpToPx(120))

                        params.x = newX
                        params.y = newY
                        windowManager?.updateViewLayout(rootLayout, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val elapsed = System.currentTimeMillis() - touchStartTime
                    if (!isDragging && elapsed < 400) {
                        // Click tap: toggle the mini window!
                        isPopupVisible = !isPopupVisible
                        popupWindow?.visibility = if (isPopupVisible) View.VISIBLE else View.GONE
                    } else if (isDragging) {
                        // Auto snap to nearest screen edge (left or right, pile poil!)
                        val edgeMargin = dpToPx(8)
                        val targetX = if (params.x + bubbleSize / 2 < screenWidth / 2) {
                            edgeMargin // Snap to left edge
                        } else {
                            screenWidth - bubbleSize - edgeMargin // Snap to right edge
                        }

                        // Smoothly animate snap-to-edge
                        snapAnimator?.cancel()
                        snapAnimator = ValueAnimator.ofInt(params.x, targetX).apply {
                            duration = 220
                            interpolator = DecelerateInterpolator()
                            addUpdateListener { animator ->
                                params.x = animator.animatedValue as Int
                                try {
                                    windowManager?.updateViewLayout(rootLayout, params)
                                } catch (_: Exception) {}
                            }
                            start()
                        }
                    }
                    true
                }
                else -> false
            }
        }

        windowManager?.addView(rootLayout, params)
    }

    private fun observeMicState() {
        serviceScope.launch {
            VoiceStateHolder.isMicMuted.collectLatest { isMuted ->
                updateUiForMicState(isMuted)
            }
        }
    }

    private fun updateUiForMicState(isMuted: Boolean) {
        val bubble = roundBubbleFrame ?: return
        val button = micButton ?: return
        val popup = popupWindow ?: return
        val dot = statusBadgeDot ?: return

        val color = if (isMuted) Color.parseColor("#EF4444") else Color.parseColor("#10B981")
        val btnColor = if (isMuted) Color.parseColor("#10B981") else Color.parseColor("#EF4444")
        val btnText = if (isMuted) "Activer micro" else "Couper micro"

        (bubble.background as? GradientDrawable)?.setStroke(dpToPx(2.5f), color)
        (dot.background as? GradientDrawable)?.setColor(color)

        button.text = btnText
        (button.background as? GradientDrawable)?.setColor(btnColor)
        (popup.background as? GradientDrawable)?.setStroke(dpToPx(1f), color)
    }

    private fun dpToPx(dp: Float): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        snapAnimator?.cancel()
        rootLayout?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (_: Exception) {}
        }
        rootLayout = null

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
    }
}
