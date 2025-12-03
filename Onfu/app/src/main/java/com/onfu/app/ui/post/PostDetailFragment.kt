package com.onfu.app.ui.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import coil.load
import com.onfu.app.R

class PostDetailFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_post_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val imageUrl = requireArguments().getString("imageUrl") ?: ""
        val description = requireArguments().getString("description") ?: ""

        val image = view.findViewById<ImageView>(R.id.iv_detail_image)
        val desc = view.findViewById<TextView>(R.id.tv_detail_description)

        image.load(imageUrl) {
            placeholder(android.R.drawable.sym_def_app_icon)
            error(android.R.drawable.sym_def_app_icon)
        }
        desc.text = description
    }

    companion object {
        fun newInstance(imageUrl: String, description: String): PostDetailFragment {
            val f = PostDetailFragment()
            f.arguments = Bundle().apply {
                putString("imageUrl", imageUrl)
                putString("description", description)
            }
            return f
        }
    }
}
