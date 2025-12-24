package com.snapcompany.snapsafe.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.snapcompany.snapsafe.models.ControlModel
import com.snapcompany.snapsafe.utilities.Firestore
import com.snapcompany.snapsafe.views.AccountView
import com.snapcompany.snapsafe.views.ControlView
import com.snapcompany.snapsafe.views.EditGate
import com.snapcompany.snapsafe.views.GateHistory
import com.snapcompany.snapsafe.views.GateSettings
import com.snapcompany.snapsafe.views.MainView
import com.snapcompany.snapsafe.views.ManageExtensions
import com.snapcompany.snapsafe.views.NewExtension
import com.snapcompany.snapsafe.views.NewGateView
import com.snapcompany.snapsafe.views.QrView
import com.snapcompany.snapsafe.views.SignInView


data class Views(
    val control: String = "ControlView",
    val signIn: String = "SignInView",
    val newGate: String = "NewGateView",
    val qrView: String = "QrView",
    val gateSettings: String = "GateSettings",
    val account: String = "Account",
    val editGate: String = "EditGate",
    val gateHistory: String = "GateHistory",
    val manageExtensions: String = "ManageExtensions",
    val newExtension: String = "NewExtension",
)

@Composable
fun NavigationGraph(
    controlModel: ControlModel,
) {
    val controlUiState by controlModel.uiState.collectAsState()
    val navController = rememberNavController()
    val views = Views()

    NavHost(
        navController = navController,
        startDestination = views.control,
    ) {
        composable( route = views.control, enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {


                if(controlUiState.userIsLogged) {
                    ControlView(
                        controlModel = controlModel,
                        navController = navController
                    )
                }
                else{
                    MainView(
                        navController = navController,
                        controlModel = controlModel
                    )
                }



        }
        composable(views.signIn) {
            SignInView(navController = navController, controlModel = controlModel)
        }

        composable(views.newGate) {
            NewGateView(
                navController = navController,
                controlModel = controlModel
            )
        }
        composable(views.qrView,
            exitTransition = { fadeOut() }
        ) {
            QrView(
                navController = navController,
                controlModel = controlModel
            )
        }
        composable(views.gateSettings) {
            GateSettings(
                navController = navController,
                controlModel = controlModel
            )
        }
        composable(views.account) {
            AccountView(
                navController = navController,
                controlModel = controlModel
            )
        }
        composable(views.editGate) {
            EditGate(
                navController = navController,
                controlModel = controlModel,
            )
        }
        composable(views.gateHistory) {
            GateHistory(
                navController = navController,
                gateData = controlModel.currentGateData,
                firestore = controlModel.firestore
            )
        }
        composable(views.manageExtensions) {
            ManageExtensions(
                navController = navController,
                controlModel = controlModel
            )
        }
        composable(views.newExtension){
            NewExtension(
                navController = navController,
                controlModel = controlModel
            )
        }
    }
}

