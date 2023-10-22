package cz.pyramidak.kalendar

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import kotlin.math.abs

fun View.keyboardHide() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}
fun View.keyboardToggle() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.toggleSoftInputFromWindow(windowToken, 1,0)
}

fun Activity.screenSize(): Int {
    return when (this.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) {
        Configuration.SCREENLAYOUT_SIZE_LARGE -> 3
        Configuration.SCREENLAYOUT_SIZE_NORMAL -> 2
        Configuration.SCREENLAYOUT_SIZE_SMALL -> 1
        else -> 0
    }
}
fun Activity.screenOrientation(): Int {
    return if (this.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) 1 else 2
}

class PagerAdapter(activity: FragmentActivity): FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> DatabaseFragment()
            1 -> SearchFragment()
            3 -> EditFragment()
            4 -> ExchangeFragment()
            else -> MainFragment()
        }
    }
}

class NoScrollLayoutManager(context: Context?) : LinearLayoutManager(context) {
    private var isScrollEnabled = true
    fun setScrollEnabled(flag: Boolean) {
        isScrollEnabled = flag
    }

    override fun canScrollVertically(): Boolean {
        //Similarly you can customize "canScrollHorizontally()" for managing horizontal scroll
        return isScrollEnabled && super.canScrollVertically()
    }
}

class CustomScrollListener(private val view: View?) : RecyclerView.OnScrollListener() {
    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        when (newState) {
            //RecyclerView.SCROLL_STATE_IDLE -> println("The RecyclerView is not scrolling")
            RecyclerView.SCROLL_STATE_DRAGGING -> view?.keyboardHide() //The RecyclerView is scrolling
            // RecyclerView.SCROLL_STATE_SETTLING -> println("Scroll Settling")
        }
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        when {
          dx > 0 -> {} // println("Scrolled Right")
          dx < 0 -> {} // println("Scrolled Left")
          else -> {}
        }
        when {
            dy > 0 -> {} // println("Scrolled Right")
            dy < 0 -> {} // println("Scrolled Left")
            else -> {}
        }
    }
}

open class OnSwipeTouchListener(c: Context?, val activity: MainActivity) : View.OnTouchListener {
    private val gestureDetector: GestureDetector

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(motionEvent)
    }
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        private val swipeTHRESHOLD: Int = 100
        private val swipeVelocityTHRESHOLD: Int = 100
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            onClick()
            return super.onSingleTapUp(e)
        }
        override fun onDoubleTap(e: MotionEvent): Boolean {
            onDoubleClick()
            return super.onDoubleTap(e)
        }
        override fun onLongPress(e: MotionEvent) {
            onLongClick()
            super.onLongPress(e)
        }
        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            try {
                val diffY = e2.y - e1.y
                val diffX = e2.x - e1.x
                if (abs(diffX) > abs(diffY)) {
                    if (abs(diffX) > swipeTHRESHOLD && abs(
                            velocityX
                        ) > swipeVelocityTHRESHOLD
                    ) {
                        if (diffX > 0) {
                            onSwipeRight()
                        }
                        else {
                            onSwipeLeft()
                        }
                    }
                }
                else {
                    if (abs(diffY) > swipeTHRESHOLD && abs(
                            velocityY
                        ) > swipeVelocityTHRESHOLD
                    ) {
                        if (diffY < 0) {
                            onSwipeUp()
                        }
                        else {
                            onSwipeDown()
                        }
                    }
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
            return false
        }
    }

    open fun onSwipeRight(frag: Int = 1) {}
    open fun onSwipeLeft(frag: Int = 3) {}
    open fun onSwipeUp() {}
    open fun onSwipeDown() {}
    open fun onClick() {}
    private fun onDoubleClick() {}
    private fun onLongClick() {}

    init {
        gestureDetector = GestureDetector(c, GestureListener())
    }
}
