package overview.map

import android.content.Intent
import android.graphics.Point
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.widget.AutoCompleteTextView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.CoordinatesProvider
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Tap
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import org.hamcrest.Matchers.not
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


// TODO: Remove all SystemClock.sleep(2000)
@RunWith(AndroidJUnit4::class)
class InstrumentedTests {

    @Rule @JvmField
    val activityTestRule: ActivityTestRule<MapsActivity> = ActivityTestRule(MapsActivity::class.java)

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("overview.map", appContext.packageName)
    }

    @Test
    fun isPopupHiddenByDefault() {
        onView(ViewMatchers.withId(R.id.popupLayout))
            .check(matches(not(isCompletelyDisplayed())))
    }

    @Test
    fun isPopupHiddenAfterRestart() {
        onView(ViewMatchers.withId(R.id.popupLayout))
            .check(matches(not(isCompletelyDisplayed())))

        showPopup()

        activityTestRule.activity.finish()
        SystemClock.sleep(2000)
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(appContext, MapsActivity::class.java)
        activityTestRule.launchActivity(intent)
        SystemClock.sleep(2000)
        onView(ViewMatchers.withId(R.id.popupLayout))
            .check(matches(not(isCompletelyDisplayed())))
    }

    private fun showPopup() {
        onView(ViewMatchers.withId(R.id.search_fab))
            .perform(click())
        onView(isAssignableFrom(AutoCompleteTextView::class.java)).perform(replaceText("тетяна"))
        SystemClock.sleep(1000)
        onView(withId(R.id.search_results_rv))
            .perform(RecyclerViewActions.actionOnItemAtPosition<SearchRecyclerViewAdapter.ViewHolder>(0, click()))
        SystemClock.sleep(2000)
        val display = activityTestRule.activity.windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val widthCenter = size.x / 2
        val heightCenter = size.y / 2
        onView(withId(R.id.map))
            .perform(clickXY(widthCenter, heightCenter))
    }

    @Test
    fun isPopupShowsOnTap() {
        showPopup()

        SystemClock.sleep(2000)
        onView(ViewMatchers.withId(R.id.popupLayout))
            .check(matches(isCompletelyDisplayed()))
    }

    private fun clickXY(x: Int, y: Int): ViewAction {
        return GeneralClickAction(
            Tap.SINGLE,
            object: CoordinatesProvider {
                override fun calculateCoordinates(view: View): FloatArray {
                    val screenPos = IntArray(2)
                    view.getLocationOnScreen(screenPos)
                    val screenX = (screenPos[0] + x).toFloat()
                    val screenY = (screenPos[1] + y).toFloat()
                    return floatArrayOf(screenX, screenY)
                }
            },
            Press.FINGER,
            InputDevice.SOURCE_MOUSE,
            MotionEvent.BUTTON_PRIMARY)
    }
}
