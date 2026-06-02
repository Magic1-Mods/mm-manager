package bin.mg.main.app.dex.plus.clickeffect

import android.animation.ValueAnimator
import android.graphics.drawable.ColorDrawable
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator

object T_a {

    fun interface ClickAction {
        fun onClick(view: View)
    }

    private const val OVERLAY_COLOR = 0xFF000000.toInt()
    private const val PRESSED_ALPHA = 28
    private const val PRESS_IN_DURATION_MS = 65L
    private const val RELEASE_DURATION_MS = 140L
    private const val TAP_HOLD_DURATION_MS = 16L

    private val PRESS_IN_INTERPOLATOR: Interpolator = DecelerateInterpolator()
    private val RELEASE_INTERPOLATOR: Interpolator = AccelerateDecelerateInterpolator()

    @JvmStatic
    fun apply(view: View) {
        apply(view, null)
    }

    @JvmStatic
    fun apply(view: View, clickAction: ClickAction?) {
        view.setOnTouchListener(touchListener(clickAction))
    }

    @JvmStatic
    fun touchListener(): View.OnTouchListener = touchListener(null)

    @JvmStatic
    fun touchListener(clickAction: ClickAction?): View.OnTouchListener =
        SmoothTouchListener(clickAction)

    private fun isInside(view: View, event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        return x >= 0 && x <= view.width && y >= 0 && y <= view.height
    }

    private class SmoothTouchListener(
        private val clickAction: ClickAction?
    ) : View.OnTouchListener {

        private val overlay = ColorDrawable(OVERLAY_COLOR)
        private var alphaAnimator: ValueAnimator? = null
        private var boundView: View? = null
        private var overlayAttached = false
        private var pressedInside = false

        private val releaseRunnable = Runnable {
            boundView?.let { view ->
                animateTo(view, 0, RELEASE_DURATION_MS, RELEASE_INTERPOLATOR)
            }
        }

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            ensureOverlay(v)

            return when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    pressedInside = true
                    v.removeCallbacks(releaseRunnable)
                    animateTo(v, PRESSED_ALPHA, PRESS_IN_DURATION_MS, PRESS_IN_INTERPOLATOR)
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    updatePressedState(v, isInside(v, event))
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val click = pressedInside && isInside(v, event)
                    release(v, click)
                    if (click) {
                        v.performClick()
                        clickAction?.onClick(v)
                    }
                    true
                }

                MotionEvent.ACTION_CANCEL -> {
                    release(v, false)
                    true
                }

                else -> false
            }
        }

        private fun ensureOverlay(view: View) {
            if (!overlayAttached || boundView !== view) {
                if (boundView != null && overlayAttached) {
                    boundView!!.overlay.remove(overlay)
                }
                boundView = view
                overlay.alpha = 0
                view.overlay.add(overlay)
                overlayAttached = true
            }
            overlay.setBounds(0, 0, view.width, view.height)
        }

        private fun updatePressedState(view: View, nextPressed: Boolean) {
            if (pressedInside == nextPressed) return
            pressedInside = nextPressed
            animateTo(
                view,
                if (nextPressed) PRESSED_ALPHA else 0,
                if (nextPressed) PRESS_IN_DURATION_MS else RELEASE_DURATION_MS,
                if (nextPressed) PRESS_IN_INTERPOLATOR else RELEASE_INTERPOLATOR
            )
        }

        private fun release(view: View, clicked: Boolean) {
            pressedInside = false
            view.removeCallbacks(releaseRunnable)

            if (!clicked) {
                animateTo(view, 0, RELEASE_DURATION_MS, RELEASE_INTERPOLATOR)
                return
            }
            view.postDelayed(releaseRunnable, TAP_HOLD_DURATION_MS)
        }

        private fun animateTo(
            view: View,
            targetAlpha: Int,
            duration: Long,
            interpolator: Interpolator
        ) {
            alphaAnimator?.cancel()

            val currentAlpha = overlay.alpha
            if (currentAlpha == targetAlpha) return

            alphaAnimator = ValueAnimator.ofInt(currentAlpha, targetAlpha).apply {
                this.duration = duration
                this.interpolator = interpolator
                addUpdateListener { animation ->
                    overlay.alpha = animation.animatedValue as Int
                    view.postInvalidateOnAnimation()
                }
                start()
            }
        }
    }
}