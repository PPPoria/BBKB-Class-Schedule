package com.poria.base.adapter

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

class SingleBindingAdapter<VB : ViewBinding, T : Any>(
    private val itemLayoutId: Int,
    private val vbBind: (View) -> VB,
    private val itemId: ((T) -> Long)? = null,
    private val onBindView: ((DataParcel<VB, T>) -> Unit)? = null
) : RecyclerView.Adapter<SingleBindingAdapter.RVHolder<VB>>() {
    init {
        setHasStableIds(true)
    }

    @Volatile
    var data: List<T> = emptyList()
        set(value) {
            synchronized(this) {
                field = value
                update()
            }
        }

    @Volatile
    var position: Int = 0
        set(value) {
            synchronized(this) {
                field = value
                update()
            }
        }

    private val mh = Handler(Looper.getMainLooper())

    private fun update() {
        mh.post(this::notifyDataSetChanged)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RVHolder<VB> {
        val root = LayoutInflater.from(parent.context)
            .inflate(itemLayoutId, parent, false)
        return RVHolder(vbBind(root))
    }

    override fun getItemCount() = data.size

    override fun getItemId(position: Int): Long {
        return itemId?.invoke(data[position])
            ?: data[position].hashCode().toLong()
    }

    override fun onBindViewHolder(holder: RVHolder<VB>, position: Int) {
        onBindView?.invoke(
            DataParcel(
                binding = holder.binding,
                item = data[position],
                index = position,
            )
        )
    }

    class RVHolder<VB : ViewBinding>(
        val binding: VB
    ) : RecyclerView.ViewHolder(binding.root) {}

    data class DataParcel<VB : ViewBinding, T : Any>(
        val binding: VB,
        val item: T,
        val index: Int,
    )
}