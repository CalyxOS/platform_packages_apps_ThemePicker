/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.customization.picker.clock.ui.view

import android.content.Context
import android.content.res.Resources
import android.graphics.Point
import android.graphics.Rect
import android.util.TypedValue
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.lifecycle.LifecycleOwner
import com.android.systemui.plugins.ClockController
import com.android.systemui.plugins.WeatherData
import com.android.systemui.shared.clocks.ClockRegistry
import com.android.wallpaper.R
import com.android.wallpaper.util.TimeUtils.TimeTicker
import java.util.concurrent.ConcurrentHashMap

/**
 * Provide reusable clock view and related util functions.
 *
 * @property screenSize The Activity or Fragment's window size.
 */
class ClockViewFactory(
    private val appContext: Context,
    val screenSize: Point,
    private val registry: ClockRegistry,
) {
    private val resources = appContext.resources
    private val timeTickListeners: ConcurrentHashMap<Int, TimeTicker> = ConcurrentHashMap()
    private val clockControllers: HashMap<String, ClockController> = HashMap()
    private val smallClockFrames: HashMap<String, FrameLayout> = HashMap()

    fun getController(clockId: String): ClockController {
        return clockControllers[clockId]
            ?: initClockController(clockId).also { clockControllers[clockId] = it }
    }

    fun getLargeView(clockId: String): View {
        return getController(clockId).largeClock.view
    }

    fun getSmallView(clockId: String): View {
        return smallClockFrames[clockId]
            ?: createSmallClockFrame().also {
                it.addView(getController(clockId).smallClock.view)
                smallClockFrames[clockId] = it
            }
    }

    private fun createSmallClockFrame(): FrameLayout {
        val smallClockFrame = FrameLayout(appContext)
        val layoutParams =
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                resources.getDimensionPixelSize(R.dimen.small_clock_height)
            )
        layoutParams.topMargin =
            getStatusBarHeight(resources) +
                resources.getDimensionPixelSize(R.dimen.small_clock_padding_top)
        smallClockFrame.layoutParams = layoutParams

        smallClockFrame.setPaddingRelative(
            resources.getDimensionPixelSize(R.dimen.clock_padding_start),
            0,
            0,
            0
        )
        smallClockFrame.clipChildren = false
        return smallClockFrame
    }

    fun updateColorForAllClocks(@ColorInt seedColor: Int?) {
        clockControllers.values.forEach { it.events.onSeedColorChanged(seedColor = seedColor) }
    }

    fun updateColor(clockId: String, @ColorInt seedColor: Int?) {
        return (clockControllers[clockId] ?: initClockController(clockId))
            .events
            .onSeedColorChanged(seedColor)
    }

    fun updateTimeFormat(clockId: String) {
        getController(clockId)
            .events
            .onTimeFormatChanged(android.text.format.DateFormat.is24HourFormat(appContext))
    }

    fun registerTimeTicker(owner: LifecycleOwner) {
        val hashCode = owner.hashCode()
        if (timeTickListeners.keys.contains(hashCode)) {
            return
        }

        timeTickListeners[hashCode] = TimeTicker.registerNewReceiver(appContext) { onTimeTick() }
    }

    fun onDestroy() {
        timeTickListeners.forEach { (_, timeTicker) -> appContext.unregisterReceiver(timeTicker) }
        timeTickListeners.clear()
        clockControllers.clear()
        smallClockFrames.clear()
    }

    private fun onTimeTick() {
        clockControllers.values.forEach {
            it.largeClock.events.onTimeTick()
            it.smallClock.events.onTimeTick()
        }
    }

    fun unregisterTimeTicker(owner: LifecycleOwner) {
        val hashCode = owner.hashCode()
        timeTickListeners[hashCode]?.let {
            appContext.unregisterReceiver(it)
            timeTickListeners.remove(hashCode)
        }
    }

    private fun initClockController(clockId: String): ClockController {
        val controller =
            registry.createExampleClock(clockId).also { it?.initialize(resources, 0f, 0f) }
        checkNotNull(controller)

        // Configure light/dark theme
        val isLightTheme = TypedValue()
        appContext.theme.resolveAttribute(android.R.attr.isLightTheme, isLightTheme, true)
        val isRegionDark = isLightTheme.data == 0
        controller.largeClock.events.onRegionDarknessChanged(isRegionDark)
        // Configure font size
        controller.largeClock.events.onFontSettingChanged(
            resources.getDimensionPixelSize(R.dimen.large_clock_text_size).toFloat()
        )
        controller.largeClock.events.onTargetRegionChanged(getLargeClockRegion())

        controller.smallClock.events.onRegionDarknessChanged(isRegionDark)
        controller.smallClock.events.onFontSettingChanged(
            resources.getDimensionPixelSize(R.dimen.small_clock_text_size).toFloat()
        )
        controller.smallClock.events.onTargetRegionChanged(getSmallClockRegion())

        // Use placeholder for weather clock preview in picker
        controller.events.onWeatherDataChanged(
            WeatherData(
                description = DESCRIPTION_PLACEHODLER,
                state = WEATHERICON_PLACEHOLDER,
                temperature = TEMPERATURE_PLACEHOLDER,
                useCelsius = USE_CELSIUS_PLACEHODLER,
            )
        )
        return controller
    }

    /**
     * Simulate the function of getLargeClockRegion in KeyguardClockSwitch so that we can get a
     * proper region corresponding to lock screen in picker and for onTargetRegionChanged to scale
     * and position the clock view
     */
    private fun getLargeClockRegion(): Rect {
        val largeClockTopMargin =
            resources.getDimensionPixelSize(R.dimen.keyguard_large_clock_top_margin)
        val targetHeight = resources.getDimensionPixelSize(R.dimen.large_clock_text_size) * 2
        val top = (screenSize.y / 2 - targetHeight / 2 + largeClockTopMargin / 2)
        return Rect(0, top, screenSize.x, (top + targetHeight))
    }

    /**
     * Simulate the function of getSmallClockRegion in KeyguardClockSwitch so that we can get a
     * proper region corresponding to lock screen in picker and for onTargetRegionChanged to scale
     * and position the clock view
     */
    private fun getSmallClockRegion(): Rect {
        val topMargin =
            getStatusBarHeight(resources) +
                resources.getDimensionPixelSize(R.dimen.small_clock_padding_top)
        val start = resources.getDimensionPixelSize(R.dimen.clock_padding_start)
        val targetHeight = resources.getDimensionPixelSize(R.dimen.small_clock_height)
        return Rect(start, topMargin, screenSize.x, topMargin + targetHeight)
    }

    companion object {
        const val DESCRIPTION_PLACEHODLER = ""
        const val TEMPERATURE_PLACEHOLDER = 58
        val WEATHERICON_PLACEHOLDER = WeatherData.WeatherStateIcon.MOSTLY_SUNNY
        const val USE_CELSIUS_PLACEHODLER = false

        private fun getStatusBarHeight(resource: Resources): Int {
            var result = 0
            val resourceId: Int = resource.getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId > 0) {
                result = resource.getDimensionPixelSize(resourceId)
            }
            return result
        }
    }
}
