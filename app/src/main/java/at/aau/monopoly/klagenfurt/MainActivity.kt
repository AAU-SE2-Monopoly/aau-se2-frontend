package at.aau.monopoly.klagenfurt

import MyStompManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
<<<<<<< HEAD
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import at.aau.serg.websocketbrokerdemo.ui.MainViewModel
import com.example.myapplication.R
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(stompManager = MyStompManager(ServiceLocator.provideStompClient()))
    }

    private lateinit var response: TextView
=======
>>>>>>> a31b100 (fix: resolve unresolved reference 'example' after package rename)

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
                    response.text = res
                }
            }
        }
    }
}
