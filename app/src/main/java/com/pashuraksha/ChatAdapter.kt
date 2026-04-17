package com.pashuraksha

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Single chat message. role = "user" or "bot".
 */
data class ChatMessage(val role: String, val text: String)

/**
 * RecyclerView adapter for the chat. Renders user (right) vs bot (left) bubbles
 * using two distinct item view types.
 */
class ChatAdapter(private val messages: MutableList<ChatMessage>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_USER = 1
        private const val VIEW_BOT = 2
    }

    override fun getItemViewType(position: Int): Int =
        if (messages[position].role == "user") VIEW_USER else VIEW_BOT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_USER) {
            UserVH(inflater.inflate(R.layout.item_message_user, parent, false))
        } else {
            BotVH(inflater.inflate(R.layout.item_message_bot, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]
        when (holder) {
            is UserVH -> holder.text.text = msg.text
            is BotVH -> holder.text.text = msg.text
        }
    }

    override fun getItemCount(): Int = messages.size

    fun addMessage(msg: ChatMessage) {
        messages.add(msg)
        notifyItemInserted(messages.size - 1)
    }

    fun updateLastBot(text: String) {
        if (messages.isEmpty()) return
        val lastIdx = messages.size - 1
        if (messages[lastIdx].role == "bot") {
            messages[lastIdx] = ChatMessage("bot", text)
            notifyItemChanged(lastIdx)
        }
    }

    class UserVH(v: View) : RecyclerView.ViewHolder(v) {
        val text: TextView = v.findViewById(R.id.messageText)
    }

    class BotVH(v: View) : RecyclerView.ViewHolder(v) {
        val text: TextView = v.findViewById(R.id.messageText)
    }
}
