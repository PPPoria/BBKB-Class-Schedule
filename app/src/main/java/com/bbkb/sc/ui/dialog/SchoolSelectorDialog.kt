package com.bbkb.sc.ui.dialog

import android.content.Intent
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bbkb.sc.R
import com.bbkb.sc.ui.activity.AuthActivity
import com.bbkb.sc.databinding.DialogSchoolSelectorBinding
import com.bbkb.sc.databinding.ItemSchoolSelectorBinding
import com.bbkb.sc.schedule.School
import com.poria.base.base.BaseDialog

class SchoolSelectorDialog : BaseDialog<DialogSchoolSelectorBinding>() {
    override fun onViewBindingCreate() = DialogSchoolSelectorBinding.inflate(layoutInflater)

    var schoolName: String = "Unknow"

    override fun initView() {
        binding.rv.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = Adapter(
                ArrayList(School.dataList),
                position = School.dataList.let { list ->
                    list.indexOfFirst { it.name == schoolName }
                },
                this@SchoolSelectorDialog::onClick
            )
        }
    }

    private fun onClick(sd: School.SchoolData) {
        Intent(requireActivity(), AuthActivity::class.java).apply {
            putExtra(AuthActivity.KEY_UPDATE_ZC, 0)
            putExtra(AuthActivity.KEY_SCHOOL_ID, sd.id)
            requireActivity().startActivity(this)
        }
        dismiss()
    }

    class Adapter(
        private val list: ArrayList<School.SchoolData> = ArrayList(),
        private var position: Int = 0,
        var listener: (School.SchoolData) -> Unit = {}
    ) : RecyclerView.Adapter<Adapter.Holder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_school_selector, parent, false)
                .let { Holder(it) }


        override fun getItemCount() = list.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.binding.apply {
                val context = holder.itemView.context
                if (position == this@Adapter.position) {
                    bg.backgroundTintList = ColorStateList
                        .valueOf(context.getColor(R.color.primary))
                    name.setTextColor(context.getColor(R.color.white))
                } else {
                    bg.backgroundTintList = ColorStateList
                        .valueOf(context.getColor(R.color.gray_shade))
                    name.setTextColor(context.getColor(R.color.gray))
                }
                val sd = list[position]
                name.text = sd.name
                icon.setImageResource(sd.iconId)
                bg.setOnClickListener { listener(sd) }
            }
        }

        class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val binding = ItemSchoolSelectorBinding.bind(itemView)
        }
    }
}