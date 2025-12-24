package com.snapcompany.snapsafe.models

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.toMutableStateList
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
import androidx.core.content.ContextCompat.registerReceiver
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snapcompany.snapsafe.R
import com.snapcompany.snapsafe.utilities.AESEncryption
import com.snapcompany.snapsafe.utilities.AppDatabase
import com.snapcompany.snapsafe.utilities.Ble
import com.snapcompany.snapsafe.utilities.CacheUtilities
import com.snapcompany.snapsafe.utilities.SSEncryption
import com.snapcompany.snapsafe.utilities.GateUser
import com.snapcompany.snapsafe.utilities.Firestore
import com.snapcompany.snapsafe.utilities.GateData
import com.snapcompany.snapsafe.utilities.SharedExtensionAccess
import com.snapcompany.snapsafe.utilities.UserData
import com.snapcompany.snapsafe.utilities.UserDataAppDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID


val answers: Map<String, Int> = mapOf(
    "1" to R.string.request_recieved,
    "2" to R.string.limit_reached_manually,
    "3" to R.string.opening_by_control,
    "4" to R.string.closing_by_control,
    "5" to R.string.stopping_by_control,
    "7" to R.string.connected,
    "8" to R.string.door_open,
    "9" to R.string.door_lock,
    "10" to R.string.gate_open,
    "11" to R.string.already_open,
    "12" to R.string.already_openning,
    "13" to R.string.open_and_stop,
    "14" to R.string.opening_by_control,
    "15" to R.string.force_open_3_seconds,
    "16" to R.string.opening,
    "17" to R.string.opening_due_to_obstruction,
    "20" to R.string.gate_closed,
    "21" to R.string.already_closed,
    "22" to R.string.already_closing,
    "23" to R.string.on_waiting_to_close,
    "24" to R.string.closing_on_pass,
    "25" to R.string.force_close_3_seconds,
    "26" to R.string.close_on_pass_activated,
    "27" to R.string.close_on_pass_deactivated,
    "28" to R.string.closing,
    "29" to R.string.close_canceled_due_to_obstruction,
    "30" to R.string.gate_stopped,
    "31" to R.string.already_stopped,
    "32" to R.string.gate_stopped_due_to_open_and_stop,
    "33" to R.string.gate_stopped_due_always_stop_after_open,
    "34" to R.string.stop_always_after_open_enabled,
    "35" to R.string.stop_always_after_open_disabled,
    "36" to R.string.stopped_and_obstruction,
    "40" to R.string.obstruction_on_the_way,
    "41" to R.string.infrared_enabled,
    "42" to R.string.infrared_disabled,
    "43" to R.string.obstruction_bypassed_infrared_disabled,
    "44" to R.string.clear_path,
    "51" to R.string.report_received,
    "61" to R.string.brakes_enabled,
    "62" to R.string.brakes_disabled,
    "63" to R.string.brakes_intensity_to,
    "70" to R.string.time_adjusted_to,
    "71" to R.string.resetting_passwords,
    "72" to R.string.reset_aborted,
    "73" to R.string.new_password_saved,
    "74" to R.string.new_property_password_saved,
    "75" to R.string.password_result_confirmed,
    "76" to R.string.password_result_not_confirmed,
    "81" to R.string.reset_in_5_seconds,
    "82" to R.string.safety_shutdown_engine_on_for_25s,
    "83" to R.string.alert_unable_to_close,
    "84" to R.string.gate_open_for_no_reason_closing,
    "90" to R.string.order_could_not_be_executed,
    "91" to R.string.wrong_password,
    "92" to R.string.wrong_property_password,
    "94" to R.string.no_enough_permissions,
    "95" to R.string.both_limits_activated,
    "96" to R.string.report_denied_no_enough_permissions,
    "97" to R.string.closing_relay_could_not_be_deactivated,
    "98" to R.string.open_relay_could_not_be_deactivated,
    "99" to R.string.security_lock
)


enum class Orders() {
    OPEN,
    CLOSE,
    STOP,
    REPORT,
    ACTIVATE_INFRARED,
    DEACTIVATE_INFRARED,
    ACTIVATE_STOP_ON_OPEN,
    DEACTIVATE_STOP_ON_OPEN,
    ACTIVATE_CLOSE_ON_PASS,
    DEACTIVATE_CLOSE_ON_PASS,
    SET_TIME_TO_PASS,
    CHANGE_PASSWORD,
    CHANGE_PROPERTY_PASSWORD,
    BRAKE_LEVEL,
    RESET_COUNTDOWN,
    OPEN_DOOR,
    LOCK_DOOR,
    TEST_PASSWORD,

}

data class IndependentData(
    val gateId: String,
    val value: Long,
)

data class ControlUiState(
    val userIsLogged: Boolean = false,

    val globalAlertTitle: String = "",
    val globalAlertMessage: String = "",
    val globalAlertShow: Boolean = false,

    val loadingGates: Boolean = false,

    val loading: Boolean = false,
    val firestoreLoading: Boolean = false,
    val isConnectedUi: Boolean = false,
    val infoMessage: Int = R.string.welcome,
    val infoMessageString: String = "",
    val switchToInfoMessageString: Boolean = false,
    val showNewGateDialog: Boolean = false,
    val hasBlePermission: Boolean = false,
    val hasBleScanPermission: Boolean = false,
    val gateList: MutableList<GateData> = mutableStateListOf(),
    val firstLaunch: Boolean = true,
    val bluetoothConnectPermissionRequest: Boolean = false,
    val bluetoothScanPermissionRequest: Boolean = false,
    val bluetoothEnableRequest: Boolean = false,

    val infraredState: Boolean = false,
    val stopOnOpenState: Boolean = false,
    val closeOnPassState: Boolean = false,
    val timeToPass: Int = 5,
    val brakeLevel: Int = 50,
    val opens: Int = 0,
    val closes: Int = 0,
    val stops: Int = 0,
    val position: Int = 0,

    val showBleAlert: Boolean = false,
    val bleAlertMessage: String = "",
    val passwordNotMatch: Boolean = false,
    val passwordMatch: Boolean = false,
    val showTestPasswordSheet: Boolean = false,


    val emailExists: Boolean = false,
    val isCheckingEmail: Boolean = false,
    val deadlineSelected: String = "",

    val userEmail: String = "",
    val subscription: Boolean = false,
    val phone: String = "",
    val name: String = "",
    val profileImageUrl: String? = "",

    var extensions: List<GateUser> = mutableStateListOf(),
    var sharedExtensions: List<SharedExtensionAccess> = mutableStateListOf(),

    var userData: UserData = UserData(),

    var lastIndependentCheck: MutableMap<String, Long> = mutableStateMapOf(),
    val currentGateEnableStatus: Boolean = false,
    val currentDisableReason: String = "",

    )

var bluetoothGatt: BluetoothGatt? = null
var currentDeviceAddress = ""// iglesia v3 was this: "A0:B7:65:4B:C4:D6"
var currentDevice: BluetoothDevice? = null


class ControlModel(
    val context: Context,
    val appDatabase: AppDatabase,
    val gateListFromAppDb: List<GateData>,
    val bluetoothAdapter: BluetoothAdapter?,
    val bleScanner: BluetoothLeScanner?,
    val userIsLogged: Boolean,
    val userEmail: String = "",
    val gateImagesListAppDb: MutableList<Uri?>
) : ViewModel()  {



        private val _uiState = MutableStateFlow(ControlUiState())
        val uiState: StateFlow<ControlUiState> = _uiState.asStateFlow()

    lateinit var firestore: Firestore

    val appDao = appDatabase.appDao()

    var gateImagesList: MutableList<Uri?> = gateImagesListAppDb


    val cacheUtilities = CacheUtilities()
    private val SSEncryption = SSEncryption()

    var isConnected: Boolean = false

    var answerCode: Int = 0


    private var open = ""
    private var close = ""
    private var stop = ""
    private var report = ""
    private var infrared = ""
    private var stopOnOpen = ""
    private var closeOnPass = ""
    private var setTime = ""
    private var changePassword = ""
    private var changePropertyPassword = ""
    private var brakeLevel = ""
    private var resetCountdown = ""
    private var openDoor = ""
    private var lockDoor = ""
    private var testPassword = ""

    private var stopOpenDoorCountDown = true

    var newGateData: GateData = GateData()
    var currentGateData: GateData = GateData()

    val aesEncryption = AESEncryption()
    val iv: ByteArray = aesEncryption.generateIv()
    var secretKey: ByteArray = "0123456789012345".toByteArray() //16 or 32 bytes

    //var pendingSaveOrderInHistory = false
    var orderToSaveInHistory: Orders? = null


    var bleState = ""

    private val ble = Ble(context)



    private val scanFilters = ArrayList<ScanFilter>()

    private fun startScanning() {

        endScan()
        scanFilters.add(
            ScanFilter.Builder()
                .setDeviceAddress(currentDeviceAddress)
                .build()
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                updateBluetoothScanPermissionRequest(true)
                return
            }
        }
        if (_uiState.value.gateList.isNotEmpty()) {
            bleScanner?.startScan(bleScanCallback)
            updateIntInfoMessage(R.string.connecting)

//            Handler(Looper.getMainLooper()).postDelayed({
//                if(_uiState.value.gateList.isNotEmpty()) {
//                    endScan()
//                    if (!isConnected) {
//                        updateIntInfoMessage(R.string.unable_to_connect)
//                    }
//                }
//
//            }, 60000)
        }

    }



    fun encryptQrData(): String? {

        val tag = "encryptGateData"

        val gateData = currentGateData

        val plainText = gateData.gateId + " " + gateData.mac + " " + gateData.password + " " + gateData.service + " " + gateData.characteristic + " " + gateData.characteristicRx + " " + gateData.gateType + " " + gateData.independent + " " + gateData.pPassword + " " + gateData.version + " " + gateData.shareable + " " + gateData.allowHistory + " " + gateData.allowManageAccess + " " + gateData.allowGateConfig + " " + gateData.gateName + " " + gateData.accessFrom + " " + gateData.admin
        Log.d(tag, "plainText: $plainText")
        Log.d(tag, "iv: $iv")
        Log.d(tag, "secretKey: $secretKey")

        val encryptedQrData =aesEncryption.encryptStringWithSharedKey(plainText, secretKey, iv)
        Log.d(tag, "encryptedText: $encryptedQrData")

        val encryptedTextWithIv = encryptedQrData + " " + aesEncryption.byteArrayToString(iv)

        Log.d(tag, "encryptedTextWithIv: $encryptedTextWithIv")
        return encryptedTextWithIv
    }

    fun decryptQrData(encryptedQrData: String): String? {

        val splitEncryptedQrData = encryptedQrData.split(" ")
        Log.d("decryptQrData", "splitEncryptedQrData: $splitEncryptedQrData")

        val gateDataString = splitEncryptedQrData.getOrNull(0).toString()
        Log.d("decryptQrData", "gateDataString: $gateDataString")
        val ivString = splitEncryptedQrData.getOrNull(1).toString()
        Log.d("decryptQrData", "ivString: $iv")
        val iv = aesEncryption.stringToByteArray(ivString)
        Log.d("decryptQrData", "iv: $iv")

        val decryptedQrData = aesEncryption.decryptStringWithSharedKey(gateDataString, secretKey, iv)

        Log.d("decryptQrData", "decryptedQrData: $decryptedQrData")

        return decryptedQrData

    }

    fun updateFirstGate(gateList: List<GateData>){
        if (gateList.isNotEmpty()) {

            currentGateData = gateList.first()
            currentDeviceAddress = currentGateData.mac
            if(currentGateData.independent) updateCurrentGateEnableStatus(true, "")
            updateLoadingGates(false)

        } else {
            currentGateData = GateData()
            currentDeviceAddress = currentGateData.mac
            updateLoadingGates(false)
        }
    }

    fun loadGatesFromAppDb(loadDataFromAppDb: Boolean){
        if(loadDataFromAppDb){
            viewModelScope.launch(Dispatchers.IO) {
                val gateListDeferred = async { appDao.getAllGates() }
                val gateList = gateListDeferred.await()
                _uiState.update { it.copy(gateList = gateList.toMutableStateList()) }

                gateImagesList.clear()
                _uiState.value.gateList.forEachIndexed { index, gateData ->
                    gateImagesList.add(index, gateData.imageUri?.toUri())
                }
                updateFirstGate(gateList)
            }
        }
        else {
            _uiState.update { it.copy(gateList = gateListFromAppDb.toMutableStateList()) }
            updateFirstGate(_uiState.value.gateList)
        }

    }

    init {
        updateUserIsLogged(userIsLogged)

        loadGatesFromAppDb(loadDataFromAppDb = false)

        viewModelScope.launch(Dispatchers.IO) {
            firestore = Firestore(userEmail)

            val deferred = async { appDao.getUserById(userEmail) }
            val userDataAppDb = deferred.await()

            if (userDataAppDb != null) {
                _uiState.value.userData = UserData(
                    name = userDataAppDb.name,
                    email = userDataAppDb.email,
                    profileImageUrl = userDataAppDb.profileImageUrl,
                    phone = userDataAppDb.phone,
                    subscription = userDataAppDb.subscription
                )
            } else {
                Log.e("Main Activity", "Error getting userDataAppDb")
            }

            updateFirestoreLoading(true)

            if (firestore.userEmail != "") {
                firestore.getUserData(firestore.userEmail) {
                    firestore.userDataDb = it
                    Log.d("Firestore", "User data retrieved: ${firestore.userDataDb}")
                    updateExtensions(it?.extensions ?: emptyList())
                    updateSharedExtensions(it?.sharedExtensions ?: emptyList())
                    if(!currentGateData.independent) { // the device is not independent
                        checkEnableStatus(currentGateData.gateId) { status, reason ->
                            updateCurrentGateEnableStatus(status, reason)
                        }
                    }
                    updateFirestoreLoading(false)
                }
            } else {
                Log.e("Firestore", "User email is empty")
            }

            firestore.setUserDataDbListener() { snapshotData ->

                Log.d("Firestore", "User data updated: $snapshotData")
                val userDataDbMap = snapshotData?.get("Data") as? Map<*, *>
                val extensionsMap = snapshotData?.get("Extensions") as? Map<*, *>
                val sharedExtensionsMap = snapshotData?.get("SharedExtensions") as? Map<*, *>

                val userDataDb = UserData(
                    email = firestore.userEmail,
                    name = userDataDbMap?.get("name") as? String ?: "",
                    phone = userDataDbMap?.get("phone") as? String ?: "",
                    subscription = userDataDbMap?.get("subscription").toString().toBoolean(),
                    profileImageUrl = userDataDbMap?.get("profileImageUrl") as? String ?: "",
                )
                if (_uiState.value.userData != userDataDb) {
                    updateFirestoreLoading(true)
                    viewModelScope.launch(Dispatchers.IO) {
                        appDao.updateUser(
                            userDataAppDb = UserDataAppDb(
                                name = userDataDb.name,
                                email = userDataDb.email,
                                phone = userDataDb.phone,
                                subscription = userDataDb.subscription,
                                profileImageUrl = userDataDb.profileImageUrl
                            )
                        )
                    }
                    updateUserData(userDataDb)
                }

                if (_uiState.value.userEmail != userDataDb.email) {
                    updateEmail(userDataDb.email)
                }

                if (_uiState.value.phone != userDataDb.phone) {
                    updatePhone(userDataDb.phone)
                }

                if (_uiState.value.name != userDataDb.name) {
                    updateName(userDataDb.name)
                }

                if (_uiState.value.subscription != userDataDb.subscription) {
                    updateSubscription(userDataDb.subscription)
                }

                if (_uiState.value.profileImageUrl != userDataDb.profileImageUrl) {
                    updateProfileImageUrl(userDataDb.profileImageUrl)
                }

                val extensions = firestore.extensionMapToExtensionObject(extensionsMap)
                val sharedExtensions = firestore.extensionsMapToSExtensionObject(sharedExtensionsMap)

                if (_uiState.value.extensions != extensions) {
                    updateFirestoreLoading(true)
                    updateExtensions(extensions)
                }
                if (_uiState.value.sharedExtensions != sharedExtensions) {
                    updateFirestoreLoading(true)
                    updateSharedExtensions(sharedExtensions)
                }
                updateFirestoreLoading(false)

            }

            updateLoadingGates(true)

        }

    }


    private fun updateLastCheckMillisValue(gateId: String, value: Long) {
        _uiState.value.lastIndependentCheck[gateId] = value
    }

    fun checkEnableStatus(gateId: String, callback: (status: Boolean, reason: String) -> Unit){

        viewModelScope.launch(Dispatchers.IO) {
            val lastCheckMillisValue = _uiState.value.lastIndependentCheck[gateId]
            val currentTime = System.currentTimeMillis()
            val sharedExtensionAccess = _uiState.value.sharedExtensions.find { it.gateId == gateId }

            if (sharedExtensionAccess?.independent == true) callback(
                true,
                "OK"
            )
            else if (lastCheckMillisValue != null) {

                 if (currentTime - lastCheckMillisValue > 60000) {
                    firestore.checkEnableStatus(gateId) { status, reason ->
                        if (status) {

                            callback(true, "OK")
                        } else {
                            callback(false, reason)
                        }
                        updateLastCheckMillisValue(gateId, System.currentTimeMillis())
                        _uiState.value.sharedExtensions.find { it.gateId == gateId }?.enabled = status
                    }
                }
                else{
                    callback(
                        sharedExtensionAccess?.enabled ?: false,
                        if (sharedExtensionAccess?.enabled == true) "OK" else "Estás inhabilitado."
                    )
                }
            } else {
                    firestore.checkEnableStatus(gateId) { status, reason ->
                        if (status) {
                            callback(true, "OK")
                        } else {
                            callback(false, reason)
                        }
                        updateLastCheckMillisValue(gateId, System.currentTimeMillis())
                        _uiState.value.sharedExtensions.find { it.gateId == gateId }?.enabled = status
                    }

            }
        }

    }

    private val bluetoothGattCallback = object : BluetoothGattCallback() {

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            Log.d("onChangeMtu", "Mtu changed to: $mtu")
        }

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            isConnected = false
            updateIsConnectedUi(isConnected)

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // successfully connected to the GATT Server
                if (gatt == null) {
                    Log.w("Ble", "gatt = null")
                }
                Log.d("Ble", "Successfully connected to the GATT Server")
                isConnected = false
                updateIsConnectedUi(isConnected)


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
                        updateBluetoothConnectPermissionRequest(true)
                        return
                    }
                }

                gatt?.discoverServices()

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // disconnected from the GATT Server
                updateIntInfoMessage(R.string.connection_lost)
                Log.d("Ble", "disconnected from the GATT Server")
                isConnected = false
                updateIsConnectedUi(isConnected)
            }

            if (newState == BluetoothProfile.STATE_CONNECTING) {
                updateIntInfoMessage(R.string.connecting)
                isConnected = false
                updateIsConnectedUi(isConnected)
            }

            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                isConnected = false
                updateIsConnectedUi(isConnected)
            }


        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            super.onCharacteristicChanged(gatt, characteristic, value)


            Log.d("Ble", "B- Characteristic write executed...")

            val value = characteristic.getStringValue(0)
            Log.d("Ble", "B-Value of characteristic change: $value")
            if (value.subSequence(0, 1).toString() == "51") {

                updateGateReport51(
                    opens = value.subSequence(2, 6).toString().toInt(),
                    closes = value.subSequence(7, 11).toString().toInt(),
                    stops = value.subSequence(12, 16).toString().toInt(),
                )

            } else if (value.subSequence(0, 1).toString() == "52") {
                updateGateReport52(
                    timeToPass = value.subSequence(2, 3).toString().toInt(),
                    infraredState = value.subSequence(3, 3).toString().toBoolean(),
                    position = value.subSequence(4, 4).toString().toInt(),
                    closeOnPassState = value.subSequence(5, 5).toString().toBoolean(),
                    brakeLevel = value.subSequence(6,9).toString().toInt(),
                    stopOnOpenState = value.subSequence(9, 9).toString().toBoolean(),
                )

            } else {

                val answerCodeString = value.subSequence(12, 14) as String
                Log.d("Ble", "answerCodeString: $answerCodeString")
                answers[answerCodeString]?.let { updateIntInfoMessage(it) }
                    ?: updateIntInfoMessage(R.string.undefined_answer)
                Log.d("Ble", "value: $value")
                Log.d("Ble", "B- IntInfoMessage: ${_uiState.value.infoMessage}")

                orderToSaveInHistory.let {
                    when (orderToSaveInHistory) {
                        Orders.OPEN -> {
                            orderToSaveInHistory = null
                            firestore.saveOrderInHistory(
                                firestore.userDataDb,
                                currentGateData.gateId,
                                Orders.OPEN
                            )
                        }

                        Orders.CLOSE -> {
                            orderToSaveInHistory = null
                            firestore.saveOrderInHistory(
                                firestore.userDataDb,
                                currentGateData.gateId,
                                Orders.CLOSE
                            )
                        }

                        Orders.STOP -> {
                            orderToSaveInHistory = null
                            firestore.saveOrderInHistory(
                                firestore.userDataDb,
                                currentGateData.gateId,
                                Orders.STOP
                            )
                        }

                        else -> {

                        }
                    }
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val value = characteristic.getStringValue(0)
            Log.d("Ble", "C-Value of characteristic change : $value")
            Log.d("Ble", "subsequence: ${value.subSequence(0, 2)}")
            val answerStartSequence = value.subSequence(0, 2).toString()

            if (answerStartSequence == "75") {
                updateIntInfoMessage(R.string.password_result_confirmed)
                updatePasswordMatch(true)
            }

            if (answerStartSequence == "76") {
                updateIntInfoMessage(R.string.password_result_not_confirmed)
                updatePasswordNotMatch(true)
            }

            if (answerStartSequence == "51") {

                updateGateReport51(
                    opens = value.subSequence(2, 7).toString().toInt(),
                    closes = value.subSequence(7, 12).toString().toInt(),
                    stops = value.subSequence(12, 17).toString().toInt(),
                )

            }
            if (answerStartSequence == "52") {
                Log.d("Ble", "timeToPass: ${value.subSequence(2, 4)}")
                Log.d("Ble", "infraredState: ${value.subSequence(4, 5)}")
                Log.d("Ble", "position: ${value.subSequence(5, 6)}")
                Log.d("Ble", "closeOnPassState: ${value.subSequence(6, 7)}")
                Log.d("Ble", "stopOnOpenState: ${value.subSequence(8, 9)}")

                updateGateReport52(
                    timeToPass = value.subSequence(2, 4).toString().toInt(),
                    infraredState = if (value.subSequence(4, 5) == "1") true else false,
                    position = value.subSequence(5, 6).toString().toInt(),
                    closeOnPassState = if (value.subSequence(6, 7) == "1") true else false,
                    brakeLevel = value.subSequence(7,10).toString().toInt(),
                    stopOnOpenState = if (value.subSequence(10, 11) == "1") true else false,

                )

            }
            if (answerStartSequence != "51" && answerStartSequence != "52") {

                val answerCodeString = value.subSequence(12, 14) as String
                Log.d("Ble", "answerCodeString: $answerCodeString")
                if(answerCode == 23) {
                    updateSwitchToInfoMessageString(true)
                    val countDown = value.subSequence(18,20).toString().toInt()
                    updateInfoMessageString("Cerrando en $countDown s")
                }
                else{
                    updateSwitchToInfoMessageString(false)
                    answers[answerCodeString]?.let { updateIntInfoMessage(it) }
                        ?: updateIntInfoMessage(R.string.undefined_answer)
                }

                Log.d("Ble", "A- IntInfoMessage: ${_uiState.value.infoMessage}")

                orderToSaveInHistory.let {
                    when (orderToSaveInHistory) {

                        Orders.OPEN -> {
                            orderToSaveInHistory = null
                            firestore.saveOrderInHistory(
                                firestore.userDataDb,
                                currentGateData.gateId,
                                Orders.OPEN
                            )
                        }

                        Orders.CLOSE -> {
                            orderToSaveInHistory = null
                            firestore.saveOrderInHistory(
                                firestore.userDataDb,
                                currentGateData.gateId,
                                Orders.CLOSE
                            )
                        }

                        Orders.STOP -> {
                            orderToSaveInHistory = null
                            firestore.saveOrderInHistory(
                                firestore.userDataDb,
                                currentGateData.gateId,
                                Orders.STOP
                            )
                        }

                        else -> {

                        }
                    }


                }

            }


            // Do something with the value
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {

            Log.d("Ble", "B- Characteristic write executed...")

        }


        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)

            val bluetoothGattCharacteristicRX: BluetoothGattCharacteristic =
                gatt!!.getService(UUID.fromString(currentGateData.service)).getCharacteristic(
                    UUID.fromString(
                        currentGateData.characteristicRx
                    )
                )

            for (service in gatt.getServices()) {
                for (characteristic in service.characteristics) {
                    enableCharacteristicNotifications(characteristic)
                }
            }
            enableCharacteristicNotifications(bluetoothGattCharacteristicRX)

            Log.d("Ble", "Services discovered....")
            bluetoothGatt = gatt
            isConnected = true
            updateIsConnectedUi(isConnected)
            updateIntInfoMessage(R.string.connected)
        }

    }

    fun updateCurrentGateEnableStatus(status: Boolean, reason: String){
        _uiState.update { it.copy(currentGateEnableStatus = status, currentDisableReason = reason) }
    }

    fun updateLoadingGates(value: Boolean) {
        _uiState.update { it.copy(loadingGates = value) }
    }

    fun showGlobalAlert(title: String, message: String) {
        _uiState.update {
            it.copy(
                globalAlertTitle = title,
                globalAlertMessage = message,
                globalAlertShow = true
            )
        }
    }

    fun hideGlobalAlert() {
        _uiState.update { it.copy(globalAlertShow = false) }
    }

    fun updateEmail(email: String) {
        _uiState.update { it.copy(userEmail = email) }
    }

    fun updateSwitchToInfoMessageString(value: Boolean){
        _uiState.update { it.copy(switchToInfoMessageString = value) }
    }
    fun updateInfoMessageString(value: String){
        _uiState.update { it.copy(infoMessageString = value) }
    }

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun updatePhone(phone: String) {
        _uiState.update { it.copy(phone = phone) }
    }

    fun updateSubscription(subscription: Boolean) {
        _uiState.update { it.copy(subscription = subscription) }
    }

    fun updateProfileImageUrl(profileImageUrl: String?) {
        _uiState.update { it.copy(profileImageUrl = profileImageUrl) }
    }

    fun updateSharedExtensions(sharedExtensions: List<SharedExtensionAccess>) {
        _uiState.update { it.copy(sharedExtensions = sharedExtensions) }
    }

    fun updateExtensions(extensions: List<GateUser>) {
        _uiState.update { it.copy(extensions = extensions) }
    }

    fun updateExtensionEnable(extensionEmail: String, state: Boolean){
        val extensions = _uiState.value.extensions.toMutableList()
        extensions.find { it.email == extensionEmail }?.enabled = state
        _uiState.update { it.copy(extensions = extensions) }
    }


    fun updateFirestoreLoading(value: Boolean) {
        _uiState.update { it.copy(firestoreLoading = value) }
    }

    fun updateUserData(userData: UserData) {
        _uiState.update { it.copy(userData = userData) }
    }


    fun updateUserIsLogged(value: Boolean) {
        _uiState.update { it.copy(userIsLogged = value) }
    }

    fun updateIsConnectedUi(value: Boolean) {
        _uiState.update { it.copy(isConnectedUi = value) }
    }

    fun updateBleAlert(show: Boolean, message: String) {
        _uiState.update {
            it.copy(
                showBleAlert = show,
                bleAlertMessage = message
            )
        }
    }

    fun updatePasswordNotMatch(value: Boolean) {
        _uiState.update { it.copy(passwordNotMatch = value) }
    }

    fun updatePasswordMatch(value: Boolean) {
        _uiState.update { it.copy(passwordMatch = value) }
    }

    fun updateBluetoothEnableRequest(value: Boolean) {
        _uiState.update {
            it.copy(
                bluetoothEnableRequest = value
            )
        }
    }

    fun updateGateReport51(
        opens: Int,
        closes: Int,
        stops: Int,
    ) {
        _uiState.update {
            it.copy(
                opens = opens,
                closes = closes,
                stops = stops,
            )
        }
    }

    fun updateGateReport52(
        timeToPass: Int,
        infraredState: Boolean,
        position: Int,
        closeOnPassState: Boolean,
        stopOnOpenState: Boolean,
        brakeLevel: Int
    ) {

        _uiState.update {
            it.copy(
                timeToPass = timeToPass,
                infraredState = infraredState,
                position = position,
                closeOnPassState = closeOnPassState,
                stopOnOpenState = stopOnOpenState,
                brakeLevel = brakeLevel
            )
        }
    }


    fun updateDeadlineSelected(value: String) {
        _uiState.update { it.copy(deadlineSelected = value) }
    }

    fun updateIsCheckingEmail(value: Boolean) {
        _uiState.update { it.copy(isCheckingEmail = value) }
    }

    fun updateEmailExists(value: Boolean) {
        _uiState.update { it.copy(emailExists = value) }
    }

    fun updateInfraredState(value: Boolean) {
        _uiState.update { it.copy(infraredState = value) }
    }

    fun updateStopOnOpenState(value: Boolean) {
        _uiState.update { it.copy(stopOnOpenState = value) }
    }

    fun updateCloseOnPassState(value: Boolean) {
        _uiState.update { it.copy(closeOnPassState = value) }
    }

    fun updateBrakeLevel(value: Int){
        _uiState.update { it.copy(brakeLevel = value) }
    }
    fun updateTimeToPass(value: Int) {
        _uiState.update { it.copy(timeToPass = value) }
    }

    fun updateBluetoothScanPermissionRequest(value: Boolean) {
        _uiState.update { it.copy(bluetoothScanPermissionRequest = true) }
    }

    fun updateBluetoothConnectPermissionRequest(value: Boolean) {
        _uiState.update { it.copy(bluetoothConnectPermissionRequest = value) }
    }

    fun updateFirstLaunch(value: Boolean) {
        _uiState.update { it.copy(firstLaunch = value) }
    }


    fun deleteAllGatesInAppDatabase() {
        viewModelScope.launch { appDao.deleteAllGates() }
    }


    fun updateShowNewGateDialog(show: Boolean) {
        _uiState.update { it.copy(showNewGateDialog = show) }
    }



    fun onAcceptSharedExtension(
        sharedExtensionAccess: SharedExtensionAccess,
        callback: (result: Boolean) -> Unit = {}
    ) {

        newGateData = GateData(
            gateId = sharedExtensionAccess.gateId,
            mac = sharedExtensionAccess.mac,
            gateType = sharedExtensionAccess.gateType,
            independent = sharedExtensionAccess.independent,
            pPassword = sharedExtensionAccess.pPassword,
            password = sharedExtensionAccess.password,
            service = sharedExtensionAccess.service,
            characteristic = sharedExtensionAccess.characteristic,
            characteristicRx = sharedExtensionAccess.characteristicRx,
            gateName = sharedExtensionAccess.gateName,
            imageUri = null,
            version = sharedExtensionAccess.version,
            shareable = sharedExtensionAccess.shareable,
            allowHistory = sharedExtensionAccess.allowHistory,
            allowManageAccess = sharedExtensionAccess.allowManageAccess,
            allowGateConfig = sharedExtensionAccess.allowGateConfig
        )

        saveNewGate()

        firestore.acceptSharedExtension(sharedExtensionAccess) { result, message ->
            Log.d("Firestore", "Accept shared extension result: $result, message: $message")
            callback(result)
        }
    }

    fun onRejectSharedExtension(
        sharedExtensionAccess: SharedExtensionAccess,
        callback: (result: Boolean) -> Unit = {}
    ) {
        firestore.rejectSharedExtension(sharedExtensionAccess) { result, message ->
            Log.d("Firestore", "Reject shared extension result: $result, message: $message")
            callback(result)
        }
    }

    fun qrCodeToGateData(qrCode: String): GateData? {

        val qrCodeDecrypted = decryptQrData(qrCode)
        val splitQrData = qrCodeDecrypted?.split(" ") ?: return null

        Log.d("Qr", "splitQrData: $splitQrData")

        var gateData: GateData? = null

            gateData = GateData(
                gateId = splitQrData.getOrNull(0) ?: "gateIdNull",
                mac = splitQrData.getOrNull(1) ?: "macNull",
                password = splitQrData.getOrNull(2) ?: "passwordNull",
                service = splitQrData.getOrNull(3) ?: "serviceNull",
                characteristic = splitQrData.getOrNull(4) ?: "characteristicNull",
                characteristicRx = splitQrData.getOrNull(5) ?: "characteristicRxNull",
                gateType = splitQrData.getOrNull(6) ?: "gateTypeNull",
                independent = (splitQrData.getOrNull(7) ?: "false").toBoolean(),
                pPassword = splitQrData.getOrNull(8) ?: "pPasswordNull",
                version = splitQrData[9],
                shareable = (splitQrData.getOrNull(10) ?: "false").toBoolean(),
                allowHistory = (splitQrData.getOrNull(11) ?: "false").toBoolean(),
                allowManageAccess = (splitQrData.getOrNull(12) ?: "false").toBoolean(),
                allowGateConfig = (splitQrData.getOrNull(13) ?: "false").toBoolean(),
                gateName = splitQrData.getOrNull(14) ?: "Agregar nombre",
                accessFrom = splitQrData.getOrNull(15) ?: "accessFromNull",
                admin = splitQrData.getOrNull(16) ?: "false"
            )

        return gateData
    }

    fun gateDataExists(gateData: GateData?): Boolean {
        val gateId = gateData?.gateId
        return _uiState.value.gateList.any { it.gateId == gateId }
    }


    fun updateNewGateData(gateData: GateData?) {
        if (gateData != null) newGateData = gateData
        else Toast.makeText(context, "Qr invalido", Toast.LENGTH_SHORT).show()
    }


    fun saveNewGate(gateName: String? = null, callback: (result: Boolean) -> Unit = {}) {
        if (newGateData != GateData()) {
            newGateData.gateName = gateName?.trim() ?: "Agregar nombre"

            val newGateDataHold = newGateData
            viewModelScope.launch(Dispatchers.IO) {
                appDao.insertGate(newGateDataHold)
                newGateData = GateData()
                loadGatesFromAppDb(true)
                callback(true)
            }

        } else {
            callback(false)
        }
    }

    fun onBleOrder(order: Orders, newPassword: String = "", newBrakeLevel: Int = 0) {
        Log.d("BluetoothAdapter", "${bluetoothAdapter?.state}")


        if (isConnected && bleState == "on") {
            val request = when (order) {
                Orders.OPEN -> {
                    open = SSEncryption.encrypt("010000000001", currentGateData.password)
                    orderToSaveInHistory = order
                    open
                }

                Orders.CLOSE -> {
                    close = SSEncryption.encrypt("020000000001", currentGateData.password)
                    orderToSaveInHistory = order
                    close
                }

                Orders.STOP -> {
                    stop = SSEncryption.encrypt("030000000001", currentGateData.password)
                    orderToSaveInHistory = order
                    stop
                }

                Orders.REPORT -> {
                    report = SSEncryption.encrypt("500000000001", currentGateData.password); report
                }

                Orders.ACTIVATE_INFRARED -> {
                    infrared = SSEncryption.encrypt("200000000001", currentGateData.password); infrared
                }

                Orders.DEACTIVATE_INFRARED -> {
                    infrared = SSEncryption.encrypt("200000000000", currentGateData.password); infrared
                }

                Orders.ACTIVATE_STOP_ON_OPEN -> {
                    stopOnOpen = SSEncryption.encrypt("120000000001", currentGateData.password); stopOnOpen
                }

                Orders.DEACTIVATE_STOP_ON_OPEN -> {
                    stopOnOpen = SSEncryption.encrypt("120000000000", currentGateData.password); stopOnOpen
                }

                Orders.ACTIVATE_CLOSE_ON_PASS -> {
                    closeOnPass = SSEncryption.encrypt("230000000001", currentGateData.password); closeOnPass
                }

                Orders.DEACTIVATE_CLOSE_ON_PASS -> {
                    closeOnPass = SSEncryption.encrypt("230000000000", currentGateData.password); closeOnPass
                }

                Orders.SET_TIME_TO_PASS -> {
                    setTime = SSEncryption.encrypt("0800000000${_uiState.value.timeToPass}", currentGateData.password); setTime
                }

                Orders.CHANGE_PASSWORD -> {
                    changePassword = SSEncryption.encrypt("62${newPassword}00", currentGateData.password); changePassword
                }

                Orders.CHANGE_PROPERTY_PASSWORD -> {
                    changePropertyPassword = SSEncryption.encrypt("63${newPassword}00", currentGateData.password); changePropertyPassword
                }
                Orders.BRAKE_LEVEL ->{
                    val newBrakeLevelString: String  = when (newBrakeLevel) {
                        in 0 .. 9 -> {
                            "00$newBrakeLevel"
                        }
                        in 10 .. 99 -> {
                            "0$newBrakeLevel"
                        }
                        100 -> {
                            newBrakeLevel.toString()
                        }
                        else -> {
                            "000"
                        }
                    }
                    brakeLevel = SSEncryption.encrypt("25${newBrakeLevelString}0000000", currentGateData.password); brakeLevel
                }
                Orders.RESET_COUNTDOWN ->{
                    resetCountdown = SSEncryption.encrypt("210000000000", currentGateData.password); resetCountdown
                }
                Orders.OPEN_DOOR ->{
                    stopOpenDoorCountDown = false
                    openDoor = SSEncryption.encrypt("140000000001", currentGateData.password); openDoor
                }
                Orders.LOCK_DOOR -> {
                    stopOpenDoorCountDown = true
                    lockDoor = SSEncryption.encrypt("150000000001", currentGateData.password); lockDoor
                }
                Orders.TEST_PASSWORD -> {
                    testPassword = SSEncryption.encrypt("650000000001", currentGateData.password); testPassword
                }
            }
            if(order == Orders.OPEN_DOOR){

                viewModelScope.launch {
                    while (true) {
                        // Your function to be repeated goes here
                        if(!stopOpenDoorCountDown) {
                            ble.writeRequest(
                                currentGateData.characteristic,
                                currentGateData.service,
                                request
                            ) {
                                updateIntInfoMessage(it)
                            }
                        }

                        // Suspend the coroutine for the specified delay
                        delay(7000)
                    }
                }

            }
            else {
                ble.writeRequest(
                    currentGateData.characteristic,
                    currentGateData.service,
                    request
                ) {
                    updateIntInfoMessage(it)
                }
            }

        } else if (bleState == "off") {
            updateIntInfoMessage(R.string.please_turn_on_bluetooth)

        } else {
            Toast.makeText(
                context,
                "No estabas conectado",
                Toast.LENGTH_SHORT
            ).show()
            currentDevice?.let {
                //connectBle()
                startScanning()
            }
        }
    }

    fun checkBlePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            updateHasBlePermission(
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            )
        } else {
            updateHasBlePermission(
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.BLUETOOTH
                ) == PackageManager.PERMISSION_GRANTED
            )
        }
    }

    fun updateHasBleScanPermission(hasPermission: Boolean) {
        _uiState.update { it.copy(hasBleScanPermission = hasPermission) }
    }

    fun updateHasBlePermission(hasPermission: Boolean) {
        _uiState.update { it.copy(hasBlePermission = hasPermission) }
    }

    fun checkBluetooth() {
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(context, mReceiver, filter, RECEIVER_NOT_EXPORTED)
        if (bluetoothAdapter?.state == BluetoothAdapter.STATE_OFF) {
            bleState = "off"
            updateIntInfoMessage(R.string.please_turn_on_bluetooth)
        } else bleState = "on"
    }

    fun endScan() {
        Log.w("Ble", "Scan stopped...")

        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_SCAN
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    updateBluetoothConnectPermissionRequest(true)
                    Toast.makeText(context, "Permissions needed", Toast.LENGTH_SHORT).show()
                    return
                }
            }
            bleScanner?.stopScan(bleScanCallback)

        } catch (exception: IllegalStateException) {
            // Illegal state exception!
        } finally {
            //scanHandler.removeCallbacksAndMessages(null)
        }
    }

    fun updateCurrentDeviceAndGatt() {

        if (ble.isValidMacAddress(currentDeviceAddress)) {
            currentDevice = bluetoothAdapter?.getRemoteDevice(currentDeviceAddress)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
                    updateBluetoothConnectPermissionRequest(true)
                    Toast.makeText(context, "Permissions needed", Toast.LENGTH_SHORT).show()
                    return
                }
            }
            bluetoothGatt = currentDevice?.connectGatt(context, true, bluetoothGattCallback)
            startScanning()

        } else {
            Toast.makeText(context, "Device address in incorrect", Toast.LENGTH_SHORT).show()
        }

    }


    fun updateIntInfoMessage(newInfo: Int) {
        _uiState.update { it.copy(infoMessage = newInfo) }
    }


    private fun connectBle() {

        updateIntInfoMessage(R.string.connecting)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
                updateBluetoothConnectPermissionRequest(true)
                return
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.d("Ble", "Connecting gatt >= M ")
            currentDevice?.connectGatt(
                context,
                true,
                bluetoothGattCallback,
                BluetoothDevice.TRANSPORT_LE

            )


        } else {
            Log.d("Ble", "Connecting gatt < M ")
            currentDevice?.connectGatt(
                context,
                true, bluetoothGattCallback
            );
        }

        /*val bluetoothGattCharacteristicRX: BluetoothGattCharacteristic =
            bluetoothGatt!!.getService(UUID.fromString(service)).getCharacteristic(
                UUID.fromString(
                    characteristicRx
                )
            )

        bluetoothGatt.let {
            if(it != null) {
                for (service in it.getServices()) {
                    for (characteristic in service.characteristics) {
                        enableCharacteristicNotifications(characteristic)
                    }
                }
            }
        }*/


    }


    private fun enableCharacteristicNotifications(
        characteristic: BluetoothGattCharacteristic
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
                updateBluetoothConnectPermissionRequest(true)
                return
            }
        }
        bluetoothGatt?.setCharacteristicNotification(characteristic, true)
        Log.d("Ble", "Enable characteristic notification...")
    }

    private val bleScanCallback: ScanCallback = object : ScanCallback() {
        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            updateIntInfoMessage(R.string.searching)
            Log.d("Ble", "Results onBatchScan $results")
        }

        override fun onScanResult(callbackType: Int, result: ScanResult) {

            val device = result.getDevice()
            Log.d("Ble", "Ble device: $device")
            Log.d("Ble", "Scan result: $result")
            //EndScan()
            if (device != null) connectBle(device)

        }

        override fun onScanFailed(errorCode: Int) {
            Log.w("Ble", "OOPS, there is something wrong at the scanning error: $errorCode")
            // OOPS, there is something wrong.
        }
    }

    private fun connectBle(device: BluetoothDevice) {

        updateIntInfoMessage(R.string.connecting)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
                updateBluetoothConnectPermissionRequest(true)
                return
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.d("Ble", "Connecting gatt >= M ")
            device.connectGatt(
                context,
                true,
                bluetoothGattCallback,
                BluetoothDevice.TRANSPORT_LE

            )


        } else {
            Log.d("Ble", "Connecting gatt < M ")
            device.connectGatt(
                context,
                true, bluetoothGattCallback
            );
        }

        /*val bluetoothGattCharacteristicRX: BluetoothGattCharacteristic =
            bluetoothGatt!!.getService(UUID.fromString(service)).getCharacteristic(
                UUID.fromString(
                    characteristicRx
                )
            )

        bluetoothGatt.let {
            if(it != null) {
                for (service in it.getServices()) {
                    for (characteristic in service.characteristics) {
                        enableCharacteristicNotifications(characteristic)
                    }
                }
            }
        }*/


    }


    val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(
                    BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR
                )
                when (state) {
                    BluetoothAdapter.STATE_OFF -> {
                        Log.d("BluetoothAdapter changed", "BluetoothAdapter is off")
                        updateIntInfoMessage(R.string.bluetooth_disabled)
                        isConnected = false
                        updateIsConnectedUi(isConnected)
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
                            updateBluetoothConnectPermissionRequest(true)
                            return
                        }
                        bluetoothGatt?.disconnect()
                        bluetoothGatt?.close()
                        bleState = "off"


                    }

                    BluetoothAdapter.STATE_TURNING_OFF -> {
                        Log.d("BluetoothAdapter changed", "BluetoothAdapter is turning off")
                        isConnected = false
                        updateIsConnectedUi(isConnected)

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
                            updateBluetoothConnectPermissionRequest(true)
                            return
                        }
                        bluetoothGatt?.disconnect()
                        bluetoothGatt?.close()
                    }

                    BluetoothAdapter.STATE_ON -> {
                        Log.d("BluetoothAdapter changed", "BluetoothAdapter is on")
                        //onCreate(Bundle.EMPTY)
                        /*
                        device.let {
                             if (it != null) connectBle(it)
                             Log.d("State on", "Conectando de nuevo")
                         }
                         */
                        bleState = "on"

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
                            updateBluetoothConnectPermissionRequest(true)
                            return
                        }

                        currentDevice = bluetoothAdapter?.getRemoteDevice(currentDeviceAddress)
                        bluetoothGatt =
                            currentDevice?.connectGatt(context, true, bluetoothGattCallback)

                    }

                    BluetoothAdapter.STATE_TURNING_ON -> {
                        updateIntInfoMessage(R.string.activating_bluetooth)
                        Log.d("BluetoothAdapter changed", "BluetoothAdapter is turning on")
                    }
                }
            }
        }
    }

}






