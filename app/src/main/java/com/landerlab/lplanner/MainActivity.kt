package com.landerlab.lplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.landerlab.lplanner.ui.LplannerTheme
import com.landerlab.lplanner.ui.PlannerScreen

/**
 * MainActivity.kt — Lplanner Android v1.0.0
 * Equivalent of LplannerApp.swift.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LplannerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val model: PlannerModel = viewModel()
                    PlannerScreen(model)
                }
            }
        }
    }
}
