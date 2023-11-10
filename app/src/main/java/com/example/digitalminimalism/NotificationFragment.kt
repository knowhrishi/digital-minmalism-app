package com.example.digitalminimalism

import android.content.Context
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
class NotificationFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var firestoreDB: FirebaseFirestore
    private lateinit var uniqueID: String
    private lateinit var adapter: NotificationAdapter

    override fun onAttach(context: Context) {
        super.onAttach(context)
        firestoreDB = FirebaseFirestore.getInstance()
        uniqueID = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_notification, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView(view)
        fetchDataAndUpdateUI()
    }

    private fun setupRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.recycler_view_notification)
        adapter = NotificationAdapter(emptyList(), requireContext())
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun fetchDataAndUpdateUI() {
        firestoreDB.collection("userTracking").document(uniqueID)
            .collection("appUsageInfo")
            .get()
            .addOnSuccessListener { documents ->
                val usages = documents.mapNotNull { it.toObject(UsageMonitoringFragment.AppUsage::class.java) }
                adapter.updateData(usages) // Make sure to implement this method in your adapter
            }
            .addOnFailureListener { exception ->
                // Handle any errors here, possibly show a message to the user
            }
    }
}
