package com.waypoint

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.waypoint.feature.chat.ChatRoute
import com.waypoint.feature.chat.itinerary.ITINERARY_ARG_ID
import com.waypoint.feature.chat.itinerary.ITINERARY_ROUTE_PATTERN
import com.waypoint.feature.chat.itinerary.ItineraryDetailRoute
import com.waypoint.feature.chat.itinerary.itineraryRoute
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = "chat"
                    ) {
                        composable("chat") {
                            ChatRoute(
                                onOpenItinerary = { id ->
                                    navController.navigate(itineraryRoute(id))
                                }
                            )
                        }
                        composable(
                            route = ITINERARY_ROUTE_PATTERN,
                            arguments = listOf(
                                navArgument(ITINERARY_ARG_ID) { type = NavType.StringType }
                            )
                        ) {
                            ItineraryDetailRoute(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}