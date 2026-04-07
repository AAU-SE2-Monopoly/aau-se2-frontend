package at.aau.monopoly.klagenfurt

import MyStompManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import at.aau.monopoly.klagenfurt.ui.MainViewModel
import com.example.myapplication.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(stompManager = MyStompManager(ServiceLocator.provideStompClient()))
    }

    private lateinit var response: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.fragment_fullscreen)

        findViewById<Button>(R.id.connectbtn).setOnClickListener { viewModel.connect() }
        findViewById<Button>(R.id.hellobtn).setOnClickListener { viewModel.sendHello() }
        findViewById<Button>(R.id.jsonbtn).setOnClickListener { viewModel.sendJson() }
        response = findViewById(R.id.response_view)

        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.responses.collect { res ->
                    // FIX: Zwingt das UI-Update sicher auf den Main-Thread
                    withContext(Dispatchers.Main) {
                        response.text = res
                    }
                }
            }
        }
    }
}