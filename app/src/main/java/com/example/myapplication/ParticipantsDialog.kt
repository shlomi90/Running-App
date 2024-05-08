package com.example.myapplication

import ParticipantsAdapter
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ParticipantsDialog : DialogFragment() {

    private lateinit var adapter: ParticipantsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_participants_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get the list of participants from the arguments
        val participants = arguments?.getStringArrayList("participants") ?: emptyList()

        // Initialize RecyclerView
        val recyclerViewParticipants: RecyclerView = view.findViewById(R.id.recyclerViewParticipants)
        recyclerViewParticipants.layoutManager = LinearLayoutManager(context)

        // Set up adapter and data for RecyclerView
        adapter = ParticipantsAdapter(participants)
        recyclerViewParticipants.adapter = adapter

        // Set click listener for Close button
//        val buttonClose: Button = view.findViewById(R.id.buttonClose)
//        buttonClose.setOnClickListener {
//            dismiss() // Dismiss the dialog when Close button is clicked
//        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) // Transparent background

        // Set the layout parameters for the dialog
        val layoutParams = dialog.window?.attributes
        layoutParams?.width = WindowManager.LayoutParams.WRAP_CONTENT
        layoutParams?.height = WindowManager.LayoutParams.WRAP_CONTENT
        dialog.window?.attributes = layoutParams

        return dialog
    }

    companion object {
        fun newInstance(participants: List<String>): ParticipantsDialog {
            val dialog = ParticipantsDialog()
            val bundle = Bundle()
            bundle.putStringArrayList("participants", ArrayList(participants))
            dialog.arguments = bundle
            return dialog
        }
    }
}