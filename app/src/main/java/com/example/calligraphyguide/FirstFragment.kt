package com.sidineyr.callguide

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.sidineyr.callguide.databinding.FragmentFirstBinding

class FirstFragment : Fragment() {
    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        binding.practiceButton.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_practice)
        }
        binding.guideButton.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_guide)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
