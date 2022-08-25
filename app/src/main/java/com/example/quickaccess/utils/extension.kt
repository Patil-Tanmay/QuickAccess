package com.example.quickaccess.utils

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.animation.addListener
import androidx.core.view.drawToBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import kotlin.math.hypot

sealed class Resource<out T : Any> {
    object Loading : Resource<Nothing>()
    object Error : Resource<Nothing>()
    data class Success<T : Any>(val data: T? = null) : Resource<T>()
}

fun View.hideKeyboard() {
    val inputMethodManager =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
}

fun View.showKeyBoard() {
    val inputMethodManager =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
//    deprecated solution
//    inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
}

fun Activity.hideKeyboard() {
    val view = currentFocus ?: View(this)
    view.hideKeyboard()
}

fun Fragment.hideKeyboard() {
    view.let {
        requireActivity().hideKeyboard()
    }
}

fun View.startCircularReveal() {
    addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
        override fun onLayoutChange(
            v: View, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int,
            oldRight: Int, oldBottom: Int
        ) {
            v.removeOnLayoutChangeListener(this)
            val cx = v.right
            val cy = v.top
            val radius = Math.hypot(v.width.toDouble(), v.height.toDouble()).toInt()
            ViewAnimationUtils.createCircularReveal(v, cx, cy, 0f, radius.toFloat()).apply {
//                interpolator = DecelerateInterpolator(0.5f)
                duration = 800
                start()
            }
        }
    })
}

fun FragmentManager.open(block: FragmentTransaction.() -> Unit) {
    beginTransaction().apply {
        block()
        commit()
    }
}


private fun FragmentManager.circularTransform(containerView: FragmentContainerView) {

    val viewBitmap = containerView.drawToBitmap()
    val overlayView = createEmptyImageView(containerView).apply {
        setImageBitmap(viewBitmap)
    }
    this.registerFragmentLifecycleCallbacks(object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentViewCreated(
            fm: FragmentManager,
            f: Fragment,
            v: View,
            savedInstanceState: Bundle?
        ) {
            super.onFragmentViewCreated(fm, f, v, savedInstanceState)
            fm.unregisterFragmentLifecycleCallbacks(this)

            containerView.addView(overlayView)

            if (!f.isRemoving) {
                if (f.requireView().isLaidOut) {
                    f.childFragmentManager.executePendingTransactions()
                    performRestCircularTransform(overlayView, containerView)
                } else {
                    f.requireView().doOnLaidOut {
                        f.childFragmentManager.executePendingTransactions()
                        performRestCircularTransform(overlayView, containerView)
                    }
                }
            } else containerView.removeView(overlayView)
        }
    }, false)
}

private fun performRestCircularTransform(
    overlayView: ImageView,
    containerView: FragmentContainerView
) {
    val overlayView2 = createEmptyImageView(containerView).apply {
        containerView.addView(this)
    }

    val cx = overlayView2.right
    val cy = overlayView2.top
    val radius = Math.hypot(overlayView2.width.toDouble(), overlayView2.height.toDouble()).toInt()

    val anim = ViewAnimationUtils.createCircularReveal(
        overlayView2,
        cx,
        cy,
        0f,
        hypot(containerView.width.toFloat(), containerView.height.toFloat())
    ).apply {
        addListener(
            onStart = {
                overlayView.visibility = View.INVISIBLE
                val secondBitmap = containerView.drawToBitmap()
                overlayView2.setImageBitmap(secondBitmap)
                overlayView.visibility = View.VISIBLE
            },
            onEnd = {
                containerView.removeView(overlayView)
                containerView.removeView(overlayView2)
            }
        )
        duration = 400
    }

    anim.start()
}

private fun createEmptyImageView(containerView: FragmentContainerView): ImageView {
    return ImageView(containerView.context).apply {
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
    }
}

internal fun View.doOnLaidOut(block: (View) -> Unit) {
    if (isLaidOut) {
        block.invoke(this)
    } else {
        viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener{
            override fun onPreDraw(): Boolean {
                if (isLaidOut) {
                    block.invoke(this@doOnLaidOut)
                    viewTreeObserver.removeOnPreDrawListener(this)
                }
                return true
            }
        })
    }
}


//fun Activity.showKeyBoard() {
//    val view = currentFocus ?: View(this)
//    view.showKeyBoard()
//}



