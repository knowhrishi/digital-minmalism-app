//package com.example.digitalminimalism.Analysis
//
//import android.view.LayoutInflater
//import android.view.ViewGroup
//import androidx.recyclerview.widget.RecyclerView
//import com.example.digitalminimalism.databinding.ItemAnalysisBinding
//
//class TrendAnalysisAdapter(
//    private val analysisList: List<TrendAnalysisFragment.AnalysisType>,
//    private val onAnalysisSelected: (TrendAnalysisFragment.AnalysisType) -> Unit
//) : RecyclerView.Adapter<TrendAnalysisAdapter.AnalysisViewHolder>() {
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnalysisViewHolder {
//        val binding = ItemAnalysisBinding.inflate(LayoutInflater.from(parent.context), parent, false)
//        return AnalysisViewHolder(binding)
//    }
//
//    override fun onBindViewHolder(holder: AnalysisViewHolder, position: Int) {
//        holder.bind(analysisList[position])
//    }
//
//    override fun getItemCount(): Int = analysisList.size
//
//    inner class AnalysisViewHolder(private val binding: ItemAnalysisBinding) : RecyclerView.ViewHolder(binding.root) {
//        fun bind(analysisType: TrendAnalysisFragment.AnalysisType) {
//            binding.analysisName.text = analysisType.name
//            binding.analysisIcon.setImageResource(analysisType.icon)
//            binding.analysisDescription.text = analysisType.description
//            binding.root.setOnClickListener {
//                onAnalysisSelected(analysisType)
//            }
//        }
//    }
//}