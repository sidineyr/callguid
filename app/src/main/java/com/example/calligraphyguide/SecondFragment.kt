package com.sidineyr.callguide

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.sidineyr.callguide.databinding.FragmentSecondBinding

class SecondFragment : Fragment() {
    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!
    private val exercises = listOf("a", "m", "s", "g", "Brasil")
    private var exerciseIndex = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        exerciseIndex = state?.getInt(KEY_EXERCISE) ?: 0
        showExercise()
        binding.traceView.setOnStrokesChangedListener { hasStrokes ->
            binding.clearButton.isEnabled = hasStrokes
            binding.undoButton.isEnabled = hasStrokes
        }
        binding.clearButton.setOnClickListener { binding.traceView.clear() }
        binding.undoButton.setOnClickListener { binding.traceView.undo() }
        binding.nextExerciseButton.setOnClickListener {
            exerciseIndex = (exerciseIndex + 1) % exercises.size
            binding.traceView.clear()
            showExercise()
        }
    }

    private fun showExercise() {
        binding.letterModel.text = exercises[exerciseIndex]
        binding.exerciseProgress.text = getString(R.string.exercise_progress, exerciseIndex + 1, exercises.size)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_EXERCISE, exerciseIndex)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object { private const val KEY_EXERCISE = "exercise" }
}
