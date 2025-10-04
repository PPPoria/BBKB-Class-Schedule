package com.bbkb.sc.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bbkb.sc.R
import com.bbkb.sc.databinding.ActivityNoteCategoryListBinding
import com.bbkb.sc.databinding.ItemNoteCategoryBinding
import com.bbkb.sc.schedule.database.NoteCategory
import com.bbkb.sc.schedule.database.NoteCategoryDB
import com.poria.base.adapter.SingleBindingAdapter
import com.poria.base.base.BaseActivity
import com.poria.base.ext.setOnClickListenerWithClickAnimation
import com.poria.base.ext.toDateFormat
import com.poria.base.viewmodel.SingleVM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("DefaultLocale")
class NoteCategoryListActivity : BaseActivity<ActivityNoteCategoryListBinding>() {
    override fun onViewBindingCreate() = ActivityNoteCategoryListBinding.inflate(layoutInflater)
    private val vm by viewModels<SingleVM<MData>>()
    private val adapter by lazy {
        SingleBindingAdapter<ItemNoteCategoryBinding, NoteCategory>(
            itemLayoutId = R.layout.item_note_category,
            vbBind = ItemNoteCategoryBinding::bind,
            itemId = { it.id }
        ) { binding, _, item, _ ->
            val curDate = System.currentTimeMillis().toDateFormat()
            binding.title.text = item.name
            binding.date.text = item.timeStamp.toDateFormat().run {
                if (year == curDate.year &&
                    month == curDate.month &&
                    day == curDate.day) "${String.format("%02d", hour)}:${String.format("%02d", minute)}"
                else if (year == curDate.year) "${month}月${day}日"
                else "${year}年${month}月${day}日"
            }
            // 关键字高亮
            vm.latest?.keywords?.also {
                if (it.isNotEmpty()) {
                    val spanStr = SpannableString(item.name)
                    val start = item.name.indexOf(it)
                    val end = start + it.length
                    spanStr.setSpan(
                        ForegroundColorSpan(getColor(R.color.quaternary)),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    binding.title.text = spanStr
                }
            }
            binding.bg.setOnClickListener {
                Intent(
                    this@NoteCategoryListActivity,
                    NoteItemListActivity::class.java
                ).also {
                    it.putExtra("category_id", item.id)
                    startActivity(it)
                }
            }
        }
    }

    override fun initView() {
        setLightStatusBar(true)
        setLightNavigationBar(true)
        with(binding.rv) {
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            adapter = this@NoteCategoryListActivity.adapter
        }
    }

    override fun initListener() = with(binding) {
        searchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
            override fun afterTextChanged(et: Editable?) {
                val old = vm.latest ?: return
                old.copy(
                    keywords = et.toString()
                ).also { vm.update(it) }
            }
        })
        addBtn.setOnClickListenerWithClickAnimation {
            lifecycleScope.launch {
                Intent(
                    this@NoteCategoryListActivity,
                    NoteItemListActivity::class.java
                ).also {
                    withContext(Dispatchers.IO) {
                        NoteCategory(
                            name = getString(R.string.note_name_unname),
                            timeStamp = System.currentTimeMillis(),
                            courseNames = emptyList()
                        ).let { new ->
                            NoteCategoryDB.get().dao().insert(new)
                        }
                    }.also { id -> it.putExtra("category_id", id) }
                    startActivity(it)
                }
            }
        }
    }.let { }

    override suspend fun refreshDataInScope() {
        val old = vm.latest ?: MData("", flow { })
        old.copy(
            categoriesFlow = NoteCategoryDB.get().dao().getAll()
        ).also { vm.update(it) }
    }

    override suspend fun observeDataInScope() {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch {
                vm.flow.collect { data ->

                }
            }
            launch {
                vm.latest?.categoriesFlow?.collect { list ->
                    showList(list, vm.latest?.keywords ?: "")
                }
            }
        }
    }

    private fun showList(origin: List<NoteCategory>, keywords: String) {
        adapter.data = origin.asSequence().filter {
            it.name.contains(
                keywords,
                ignoreCase = true
            )
        }.toList()
    }

    data class MData(
        val keywords: String,
        val categoriesFlow: Flow<List<NoteCategory>>
    )
}