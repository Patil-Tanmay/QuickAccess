package utils

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

sealed class Resource<out T: Any> {
    object Loading : Resource<Nothing>()
    object Error : Resource<Nothing>()
    data class Success<T: Any>(val data: T?=null) : Resource<T>()
}

fun View.hideKeyboard() {
    val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
}

fun View.showKeyBoard(){
    val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
//    deprecated solution
//    inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
}

fun Activity.hideKeyboard() {
    val view = currentFocus ?: View(this)
    view.hideKeyboard()
}

fun Fragment.hideKeyboard(){
    view.let {
        requireActivity().hideKeyboard()
    }
}

fun View.startCircularReveal() {
    addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
        override fun onLayoutChange(v: View, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int,
                                    oldRight: Int, oldBottom: Int) {
            v.removeOnLayoutChangeListener(this)
            val cx = v.right
            val cy = v.top
            val radius = Math.hypot(v.width.toDouble(), v.height.toDouble()).toInt()
            ViewAnimationUtils.createCircularReveal(v, cx, cy, 0f, radius.toFloat()).apply {
//                interpolator = DecelerateInterpolator(2f)
                duration = 1000
                start()
            }
        }
    })
}

inline fun FragmentManager.open(block: FragmentTransaction.() -> Unit) {
    beginTransaction().apply {
        block()
        commit()
    }
}


//fun Activity.showKeyBoard() {
//    val view = currentFocus ?: View(this)
//    view.showKeyBoard()
//}



