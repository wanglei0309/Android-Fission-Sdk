// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: IntroductionAdapter.kt manages and displays multiple onboarding or intro screens in a ViewPager.
 * It inflates layout pages, adds them to the container, and removes them when no longer visible.
 **/
package com.infineon.secora.wallet.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter

/**
 * Adapter class for handling multiple introduction in a ViewPager.
 * It inflates and displays the layouts provided in the `layouts` array.
 *
 * @param layouts Array of layout resource IDs to be shown in the ViewPager.
 */
class IntroductionAdapter(
    private val layouts: IntArray,
) : PagerAdapter() {

    /**
     * Returns the total number of pages to be displayed.
     *
     * @return The number of layouts in the ViewPager.
     */
    override fun getCount(): Int {
        return layouts.size
    }

    /**
     * Determines whether a page view is associated with a specific key object.
     * Used internally by the ViewPager to verify a view’s association.
     *
     * @param view The current page view.
     * @param object The key object returned by instantiateItem().
     * @return True if the view corresponds to the object, false otherwise.
     */
    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    /**
     * Creates and adds a new page (layout) to the ViewPager.
     * Inflates the layout resource corresponding to the current position.
     *
     * @param container The ViewPager that will contain the page.
     * @param position The index of the page to instantiate.
     * @return The newly created page view.
     */
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = LayoutInflater.from(container.context)
            .inflate(layouts[position], container, false)
        container.addView(view)
        return view
    }

    /**
     * Removes a page (layout) from the ViewPager.
     * Called when a page is no longer visible to the user.
     *
     * @param container The ViewPager containing the page.
     * @param position The index of the page being destroyed.
     * @param object The same object that was returned by instantiateItem().
     */
    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        val v: View = `object` as View
        container.removeView(v)
    }
}
