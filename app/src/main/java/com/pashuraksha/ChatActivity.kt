package com.pashuraksha

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.pashuraksha.ai.PashuAgent
import com.pashuraksha.data.OfflineDataRepository
import com.pashuraksha.databinding.ActivityChatBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Pashu Doctor chat — now powered by PashuAgent (Mini Manus AI).
 *
 * Every user turn runs a full 4-step agent pipeline:
 *   perceive → diagnose → reason → act
 *
 * Works both online (Gemini Flash grounded on CSV findings) and offline
 * (CSV rule engine alone). UI is identical either way.
 */
class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var adapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        OfflineDataRepository.ensureLoaded(this)

        adapter = ChatAdapter(messages)
        binding.messagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = this@ChatActivity.adapter
        }

        adapter.addMessage(
            ChatMessage(
                "bot",
                "🌿 Namaste! I'm Pashu Doctor.\n\nTell me what's wrong with your animal. I'll figure out the disease, give home care steps, and tell you when to call a vet.\n\nTry:\n• \"My cow has fever and mouth sores\"\n• \"गाय को बुखार है\"\n• \"Goat is limping\""
            )
        )

        updateConnectivityPill()
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSend.setOnClickListener { sendMessage() }
        binding.messageInput.setOnEditorActionListener { _, _, _ ->
            sendMessage(); true
        }
        binding.btnMic.setOnClickListener {
            binding.messageInput.hint = "Voice coming soon — type for now"
        }
    }

    private fun sendMessage() {
        val text = binding.messageInput.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return
        binding.messageInput.setText("")

        adapter.addMessage(ChatMessage("user", text))
        scrollToBottom()
        adapter.addMessage(ChatMessage("bot", "🌱 Thinking…"))
        binding.typingIndicator.visibility = View.VISIBLE
        scrollToBottom()

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                PashuAgent.run(
                    ctx = this@ChatActivity,
                    farmerQuery = text
                )
            }
            binding.typingIndicator.visibility = View.GONE
            adapter.updateLastBot(result.rawAnswer)
            scrollToBottom()
        }
    }

    private fun scrollToBottom() {
        binding.messagesRecyclerView.post {
            binding.messagesRecyclerView.smoothScrollToPosition(adapter.itemCount - 1)
        }
    }

    private fun updateConnectivityPill() {
        val online = isOnline()
        if (online) {
            binding.statusDot.setBackgroundColor(getColor(R.color.status_safe))
            binding.statusText.text = "Online • AI Agent"
        } else {
            binding.statusDot.setBackgroundColor(getColor(R.color.status_caution))
            binding.statusText.text = "Offline • Edge AI"
        }
    }

    private fun isOnline(): Boolean {
        return try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val n = cm.activeNetwork ?: return false
            cm.getNetworkCapabilities(n)
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } catch (_: Throwable) { false }
    }

    override fun onResume() { super.onResume(); updateConnectivityPill() }
}
