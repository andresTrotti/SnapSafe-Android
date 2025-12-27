package com.snapcompany.snapsafe

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.snapcompany.snapsafe.models.ControlModel
import com.snapcompany.snapsafe.navigation.NavigationGraph
import com.snapcompany.snapsafe.ui.theme.SnapSafeTheme
import com.snapcompany.snapsafe.utilities.AppDatabase
import com.snapcompany.snapsafe.utilities.Firestore
import com.snapcompany.snapsafe.utilities.GateData
import com.snapcompany.snapsafe.utilities.RoomKey
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val REQUEST_BLUETOOTH_CONNECT = 2

const val casa = "mi casa"


class MainActivity : ComponentActivity() {

    private val context = this
    private var userIsLogged = false


    private var userEmailLogged: String? = null
    private var gateList: List<GateData> = listOf()
    private val gateImagesList = mutableListOf<Uri?>()

    private var keepSplashScreen = true



    override fun onCreate(savedInstanceState: Bundle?) {

        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }

        super.onCreate(savedInstanceState)

         val appDatabase = AppDatabase.getDatabase(context)
         val appDao = appDatabase.appDao()


         val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
         val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
         val bleScanner = bluetoothAdapter?.bluetoothLeScanner

        enableEdgeToEdge()

        lifecycleScope.launch(Dispatchers.Main) {

            val job = lifecycleScope.launch(Dispatchers.Default) {

                val userEmailDeferred = async { appDao.getKey("userEmailLogged") }
                userEmailLogged = userEmailDeferred.await()?.value
                val gateListDeferred = async { appDao.getAllGates() }
                gateList = gateListDeferred.await()

                gateList.forEachIndexed { index, gateData ->
                    gateImagesList.add(index, gateData.imageUri?.toUri())
                }

                if (userEmailLogged != null && userEmailLogged != "") {
                    userIsLogged = true
                    Log.d("Main Activity", "User is logged as: $userEmailLogged")

                } else {
                    userIsLogged = false
                    Log.e("Main Activity", "Error getting userDataAppDb")
                }
            }


            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            } else {
                arrayOf(
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }

            if (bluetoothAdapter == null) {
                Log.w("Ble", "The device do not support BLE")
            } else {
                Log.d("Ble", "BLE adapter obtained")
                if (!bluetoothAdapter.isEnabled) {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    val REQUEST_ENABLE_BT = 1
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.


                        Toast.makeText(context, "Activa el bluetooth", Toast.LENGTH_SHORT).show()
                    }
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)

                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            ActivityCompat.requestPermissions(
                                context,
                                permissions,
                                REQUEST_BLUETOOTH_CONNECT
                            )
                        }
                    } else {
                        // Permission already granted
                        Toast.makeText(context, "Permiso concedido", Toast.LENGTH_SHORT).show()
                    }

                }
            }


            job.join()

            val controlModel = ControlModel(
                context = context,
                userEmail = userEmailLogged ?: "",
                appDatabase = appDatabase,
                bluetoothAdapter = bluetoothAdapter,
                bleScanner = bleScanner,
                gateListFromAppDb = gateList,
                userIsLogged = userIsLogged,
                gateImagesListAppDb = gateImagesList
            )

            setContent {

                SnapSafeTheme {
                    NavigationGraph(
                        controlModel = controlModel
                    ).run {
                        keepSplashScreen = false
                    }
                }
            }
        }



    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)} passing\n      in a {@link RequestMultiplePermissions} object for the {@link ActivityResultContract} and\n      handling the result in the {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH_CONNECT) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
            } else {
                // Permission denied
            }
        }
    }
}

