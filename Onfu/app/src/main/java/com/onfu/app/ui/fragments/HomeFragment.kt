package com.onfu.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.onfu.app.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.onfu.app.ui.search.SearchFragment
import com.onfu.app.ui.messages.ChatListFragment

/**
 * HomeFragment: host para el Feed y navegación inferior simple.
 * - Muestra `FeedFragment` por defecto.
 * - Usa `PlaceholderFragment` para Search/Messages/Profile mientras se implementan.
 */
class HomeFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Show Home feed by default
        if (childFragmentManager.findFragmentById(R.id.home_child_container) == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.home_child_container, com.onfu.app.ui.home.HomeFeedFragment())
                .commit()
        }

        val nav = view.findViewById<BottomNavigationView>(R.id.home_bottom_nav)

        // Default selection
        nav.selectedItemId = R.id.nav_home

        // Ensure icons are centered: remove extra bottom insets padding and hide labels
        try {
            nav.labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_UNLABELED
        } catch (_: Throwable) { /* ignore for older libs */ }
        ViewCompat.setOnApplyWindowInsetsListener(nav) { v, insets ->
            // Prevent system bottom inset from adding extra padding that visually shifts icons upward
            v.setPaddingRelative(v.paddingStart, 0, v.paddingEnd, 0)
            insets
        }

        // DEBUG: enforce our drawables at runtime and disable tinting so we can verify
        // that the BottomNavigationView is using the correct resources.
        try {
            nav.itemIconTintList = null
            nav.menu.findItem(R.id.nav_home)?.icon = resources.getDrawable(R.drawable.ic_nav_home, null)
            nav.menu.findItem(R.id.nav_search)?.icon = resources.getDrawable(R.drawable.ic_nav_search, null)
            nav.menu.findItem(R.id.nav_add)?.icon = resources.getDrawable(R.drawable.ic_nav_add, null)
            nav.menu.findItem(R.id.nav_messages)?.icon = resources.getDrawable(R.drawable.ic_nav_messages, null)
            nav.menu.findItem(R.id.nav_profile)?.icon = resources.getDrawable(R.drawable.ic_nav_profile, null)
        } catch (e: Exception) {
            // ignore; debug only
        }

        nav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    childFragmentManager.beginTransaction()
                        .replace(R.id.home_child_container, com.onfu.app.ui.home.HomeFeedFragment())
                        .commit()
                    true
                }
                R.id.nav_search -> {
                    childFragmentManager.beginTransaction()
                        .replace(R.id.home_child_container, SearchFragment())
                        .commit()
                    true
                }
                R.id.nav_messages -> {
                    childFragmentManager.beginTransaction()
                        .replace(R.id.home_child_container, ChatListFragment())
                        .commit()
                    true
                }
                R.id.nav_profile -> {
                    childFragmentManager.beginTransaction()
                        .replace(R.id.home_child_container, com.onfu.app.ui.feed.FeedFragment())
                        .commit()
                    true
                }
                R.id.nav_add -> {
                    childFragmentManager.beginTransaction()
                        .replace(R.id.home_child_container, com.onfu.app.ui.upload.UploadFragment())
                        .addToBackStack("upload")
                        .commit()
                    true
                }
                else -> false
            }
        }
    }
}
