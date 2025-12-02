package com.onfu.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.onfu.app.R

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

        // Show an empty Home placeholder by default; Feed (profile feed) is opened from the profile button
        if (childFragmentManager.findFragmentById(R.id.home_child_container) == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.home_child_container, PlaceholderFragment.newInstance("Home"))
                .commit()
        }

        view.findViewById<View>(R.id.nav_home).setOnClickListener {
            childFragmentManager.beginTransaction()
                .replace(R.id.home_child_container, PlaceholderFragment.newInstance("Home"))
                .commit()
        }

        view.findViewById<View>(R.id.nav_search).setOnClickListener {
            childFragmentManager.beginTransaction()
                .replace(R.id.home_child_container, PlaceholderFragment.newInstance("Search"))
                .commit()
        }

        view.findViewById<View>(R.id.nav_messages).setOnClickListener {
            childFragmentManager.beginTransaction()
                .replace(R.id.home_child_container, PlaceholderFragment.newInstance("Messages"))
                .commit()
        }

        view.findViewById<View>(R.id.nav_profile).setOnClickListener {
            childFragmentManager.beginTransaction()
                .replace(R.id.home_child_container, com.onfu.app.ui.feed.FeedFragment())
                .commit()
        }

        // Add button (center '+') opens the post Upload screen (not avatar)
        view.findViewById<View>(R.id.nav_add).setOnClickListener {
            childFragmentManager.beginTransaction()
                .replace(R.id.home_child_container, com.onfu.app.ui.upload.UploadFragment())
                .addToBackStack("upload")
                .commit()
        }
    }
}
