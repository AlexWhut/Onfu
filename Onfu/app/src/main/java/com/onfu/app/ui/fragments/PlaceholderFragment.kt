package com.onfu.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.widget.TextView
import com.onfu.app.R

class PlaceholderFragment : Fragment() {

    companion object {
        private const val ARG_TEXT = "arg_text"
        fun newInstance(text: String) = PlaceholderFragment().apply {
            arguments = Bundle().apply { putString(ARG_TEXT, text) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val tv = TextView(requireContext())
        tv.text = arguments?.getString(ARG_TEXT) ?: "Placeholder"
        tv.textSize = 18f
        tv.setPadding(24,24,24,24)
        return tv
    }
}
