package at.aau.serg.websocketbrokerdemo

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.myapplication.R
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity()  {

    lateinit var response: TextView
    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.fragment_fullscreen)

        findViewById<Button>(R.id.connectbtn).setOnClickListener { MyStompManager.connect() }
        findViewById<Button>(R.id.hellobtn).setOnClickListener { MyStompManager.sendHello() }
        findViewById<Button>(R.id.jsonbtn).setOnClickListener { MyStompManager.sendJson() }
        response = findViewById(R.id.response_view)
        observeStompResponses()
    }

    private fun observeStompResponses() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                MyStompManager.responses.collect { res ->
                    response.text = res
                }
            }
        }
    }



}

