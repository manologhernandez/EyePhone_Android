package com.example.eyephone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.fragment_camera_tutorial_container.*

class CameraTutorialFragment : DialogFragment() {
    override fun getTheme() = R.style.RoundedCornersDialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var rootView: View = inflater.inflate(R.layout.fragment_camera_tutorial_container, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //TODO make sure data sent to adapter is proper
        val data = listOf<String>(
            "To take an image with EyePhone, rotate your device to landscape mode.",
            "Use a mirror",
            "Or ask help from a friend",
            "Make sure your device is about 4 inches away from your face. Adjust the zoom if necessary.",
            "To ensure best image quality, having flash turned on is recommended."
        )
        val adapter = CameraTutorialAdapter(data)
        viewPager.adapter = adapter

        indicator.setViewPager(viewPager)
    }
}