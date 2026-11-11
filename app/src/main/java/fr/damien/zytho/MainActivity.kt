package fr.damien.zytho

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import fr.damien.zytho.ui.ZythoApp
import fr.damien.zytho.ui.ZythoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZythoTheme {
                ZythoApp()
            }
        }
    }
}
