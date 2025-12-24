package com.snapcompany.snapsafe.utilities

import android.util.Log
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.snapcompany.snapsafe.models.Orders




data class ExtensionStatusTypes(
    val accepted: String = "accepted",
    val rejected: String = "rejected",
    val pending: String = "pending",
)


data class Invitation(
    val gateId: String = "",
    val email: String = "",
    val fromName: String = "",
    val fromEmail: String = "",
    val message: String = "",
)

//TODO: Eliminar esta clase sharedExtensionAccess
data class SharedExtensionAccess(
    val gateId: String = "",
    var gateName: String = "",
    val mac: String = "",
    var password: String = "",
    var pPassword: String = "",
    val characteristic: String = "",
    val characteristicRx: String = "",
    val service: String = "",
    val gateType: String = "",
    val independent: Boolean = false,
    val version: String = "",
    var accessFrom: String = "",
    var boundExtension: Boolean = false,
    var daysRestricted: String = "",
    var deadline: String = "",
    var departureTime: String = "",
    var entryTime: String = "",
    var enabled: Boolean = false,
    var shareable: Boolean = false,
    var allowHistory: Boolean = false,
    var allowManageAccess: Boolean = false,
    var allowGateConfig: Boolean = false,
    var message: String = "",
    var name: String = "",
    var restrictions: Boolean = false,
    var privateRestrictions: Boolean = false,
    var status: String = "",
)

data class GateUser(
    var admin: Boolean = false,
    var independent: Boolean = false,
    var email: String = "",
    var boundExtension: Boolean = false,
    var daysRestricted: String = "",
    var deadline: String = "",
    var departureTime: String = "",
    var entryTime: String = "",
    var enabled: Boolean = false,
    var shareable: Boolean = false,
    var allowHistory: Boolean = false,
    var allowManageAccess: Boolean = false,
    var allowGateConfig: Boolean = false,
    var gateId: String = "",
    //var message: String = "",
    var name: String = "",
    var restrictions: Boolean = false,
    var privateRestrictions: Boolean = false,
    var status: String = "",
    var accessFrom: String = "",
)

data class HistoryData(
    val year: String = "",
    val month: String = "",
    val day: String = "",
    val time: String = "",
    val order: String = "",
    val user: String = ""
)

@Entity(tableName = "keys")
data class RoomKey(
    @PrimaryKey (autoGenerate = false) val roomKey: String = "",
    val value: String? = null
)

@Entity(tableName = "gates")
data class GateData(
    @PrimaryKey(autoGenerate = false) val gateId: String = "",
    val mac: String = "",
    var password: String = "",
    var pPassword: String = "",
    val characteristic: String = "",
    val characteristicRx: String = "",
    val service: String = "",
    val gateType: String = "",
    val independent: Boolean = false,
    var shareable: Boolean = false,
    var allowHistory: Boolean = false,
    var allowManageAccess: Boolean = false,
    var allowGateConfig: Boolean = false,
    val version: String = "",
    var gateName: String = "",
    var accessFrom: String = "",
    var admin: String = "false",

    var imageUri: String? = null,

)


@Entity(tableName = "users")
data class UserDataAppDb(
    @PrimaryKey(autoGenerate = false) var email: String = "",
    var name: String = "",
    var subscription: Boolean = false,
    var phone: String = "",
    var profileImageUrl: String? = ""
)

data class UserData(
    var email: String = "",
    var name: String = "",
    var lastName: String = "",
    var subscription: Boolean = false,
    var extensions: List<GateUser> = emptyList(),
    var sharedExtensions: List<SharedExtensionAccess> = emptyList(),
    var profileImageUrl: String? = null,
    var invitation: List<Invitation> = emptyList(),
    var phone: String = "",
)




class Firestore(val userEmail: String) {

    private val db = Firebase.firestore
    var userDataDb: UserData? = null
    private val dateUtilities = DateUtilities()
    private val extensionStatusTypes = ExtensionStatusTypes()


    fun changeEnableStatus(gateId: String, extensionEmail: String, newStatus: Boolean, callback: (Boolean) -> Unit){

        val updateStatusMap = mapOf(
            "SharedExtensions" to mapOf(
                userEmail to mapOf(
                    gateId to mapOf(
                        "enabled" to newStatus
                    )
                )
            )
        )

        db.collection("Users")
            .document(extensionEmail)
            .set(updateStatusMap, SetOptions.merge())
            .addOnSuccessListener {
                callback(true)
            }
            .addOnFailureListener {
                callback(false)
            }

    }

    fun checkEnableStatus(gateId: String, callback: (status: Boolean, reason: String) -> Unit ){
        db.collection("Users")
            .document(userEmail)
            .get()
            .addOnSuccessListener {
                val sharedExtensions = it.data?.get("SharedExtensions") as? Map<*, *>
                if(sharedExtensions != null) {
                    val sharedExtensionsList = extensionsMapToSExtensionObject(sharedExtensions)
                    val sharedExtension =
                        sharedExtensionsList.find { sharedExtension -> sharedExtension.gateId == gateId }
                    if (sharedExtension != null) {
                        if (sharedExtension.enabled) callback(true, "")
                        else callback(false, "Estás inhabilitado.")
                    } else {
                        callback(false, "No se encontró la extensión compartida.")
                    }
                }
                else{
                    callback(false, "No se encontró la extensión compartida.")
                }

            }
            .addOnFailureListener {
                callback(false, "Error al realizar la consulta. Comprueba tu conexión.")
            }
    }


    fun setUserDataDbListener( callback: (snapshotData: Map<String, Any>?)  -> Unit ){

        if(userEmail == "") {
            Log.e("Firestore", "User email is empty")
            callback(null)

        } else {
            db.collection("Users")
                .document(userEmail)
                .addSnapshotListener() { snapshot, e ->
                    if (e != null) {
                        Log.w("Firestore", "Listen failed.", e)
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        Log.d("Firestore", "Current data: ${snapshot.data}")
                        callback(snapshot.data)
                    } else {
                        Log.d("Firestore", "Current data: null")
                    }
                }
        }
    }

    fun saveUserGoogleAuth(userData: UserData, callback: (success: Boolean) -> Unit){
        db.collection("Users")
            .document(userData.email)
            .set(
                mapOf(
                    "Data" to mapOf(
                        "name" to userData.name,
                        "subscription" to userData.subscription,
                        "profileImageUrl" to userData.profileImageUrl,
                        "phone" to userData.phone,
                    )
                ), SetOptions.mergeFields()
            )
            .addOnSuccessListener {
                callback(true)
            }
            .addOnFailureListener {
                callback(false)
                Log.e("Firestore", "Error saving user data", it)
            }
    }

    fun checkEmailExists(email: String, callback: (Boolean) -> Unit){
        db.collection("Users")
            .document(email)
            .get()
            .addOnSuccessListener {
                callback(it.exists())
                Log.d("Firestore", "Email exists: ${it.exists()}")
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    fun acceptSharedExtension(sharedExtensionAccess: SharedExtensionAccess, callback: (Boolean, String) -> Unit){


        val updateStatusMapFromExtensionEmail = mapOf(
            "Extensions" to mapOf(
                sharedExtensionAccess.gateId to mapOf(
                    userEmail to mapOf(
                        "status" to extensionStatusTypes.accepted
                    )
                )
            )
        )
        db.collection("Users")
            .document(sharedExtensionAccess.accessFrom)
            .set(updateStatusMapFromExtensionEmail, SetOptions.merge())
            .addOnSuccessListener {

                val updateStatusMapUserEmail = mapOf(
                    "SharedExtensions" to mapOf(
                        sharedExtensionAccess.accessFrom to mapOf(
                            sharedExtensionAccess.gateId to mapOf(
                                "status" to extensionStatusTypes.accepted
                            )
                        )
                    )
                )

                db.collection("Users")
                    .document(userEmail)
                    .set(updateStatusMapUserEmail, SetOptions.merge())
                    .addOnSuccessListener {
                        callback(true, "Shared extension status to accepted")

                       /* val deleteSharedExtensionAccess = mapOf(
                            "SharedExtensions" to mapOf(
                                sharedExtensionAccess.extensionFromEmail to FieldValue.delete()
                            )
                        )
                        db.collection("Users")
                            .document(userEmail)
                            .set(deleteSharedExtensionAccess, SetOptions.merge())
                            .addOnSuccessListener {
                                callback(true, "Shared extension status to accepted")
                            }
                            .addOnFailureListener {
                                callback(false, "Error changing shared extension status to accepted")
                            }
                        */
                    }
                    .addOnFailureListener {
                        callback(false, "Error changing shared extension status to accepted")
                    }
                
            }
            .addOnFailureListener {
                callback(false, "Error changing shared extension status to accepted")
            }




    }

    fun rejectSharedExtension(sharedExtensionAccess: SharedExtensionAccess, callback: (Boolean, String) -> Unit){

        val deleteSharedExtensionAccess = mapOf(
            "SharedExtensions" to mapOf(
                sharedExtensionAccess.accessFrom to mapOf(
                    sharedExtensionAccess.gateId to FieldValue.delete()
                )
            )
        )
        db.collection("Users")
            .document(userEmail)
            .set(deleteSharedExtensionAccess, SetOptions.merge())
            .addOnSuccessListener {

                val updateStatusMap = mapOf(
                    "Extensions" to mapOf(
                        sharedExtensionAccess.gateId to mapOf(
                            sharedExtensionAccess.accessFrom to mapOf(
                                "status" to extensionStatusTypes.rejected
                            )
                        )
                    )
                )
                db.collection("Users")
                    .document(sharedExtensionAccess.accessFrom)
                    .set(updateStatusMap, SetOptions.merge())
                    .addOnSuccessListener {
                        callback(true, "Shared extension status to rejected")
                    }
                    .addOnFailureListener {
                        callback(false, "Error changing shared extension status to rejected")
                    }
            }
            .addOnFailureListener {
                callback(false, "Error al rechazar la invitación")
            }

    }
    fun checkGateDataExists(gateId: String, callback: (Boolean) -> Unit){
        db.collection("Devices")
            .document(gateId)
            .get()
            .addOnSuccessListener {
                val gateData = it.data?.get("gateData") as? Map<*, *>
                if(gateData != null) callback(true)
                else callback(false)
                Log.d("Firestore", "Gate data exists: ${gateData != null}")
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    fun uploadGateData(gateData: GateData, callback: (Boolean) -> Unit){

        val gateDataMap = mapOf("gateData" to mapOf(
            "gateId" to gateData.gateId,
            "mac" to gateData.mac,
            "password" to gateData.password,
            "pPassword" to gateData.pPassword,
            "characteristic" to gateData.characteristic,
            "characteristicRx" to gateData.characteristicRx,
            "service" to gateData.service,
            "gateType" to gateData.gateType,
            "gateName" to gateData.gateName,
            "uploadedBy" to userEmail,
            )
        )

        db.collection("Devices")
            .document(gateData.gateId)
            .set(gateDataMap, SetOptions.merge())
            .addOnSuccessListener {
                callback(true)
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    fun sendInvitationExtension(newGateUser: GateUser, gateData: GateData, message: String, callback: (Boolean, String) -> Unit){

        /*
        * 1 - Enviar la invitacion al usuario
        * 2 - Crear el gate User en el Id porton Devices/GateId/Users/ExtensionEmail
        * 3 - En la ultima direccion habran los datos de restricciones y habilitacion del usuario etc o el gateUser
        * 4 - Si la invitacion es aceptada se descargan los datos del porton junto con los datos de restricciones del usuario
        * 5 - Si la invitacion es rechazada se elimina el gateUser del gateId y se elimina la invitacion del usuario
        * 6 - Verificar si existe la gateData en la base de datos
        * 7 - Si no existe se crea en la base de datos
        */
        val userData = userDataDb

        val invitationMap =
            mapOf("Invitation" to
                    mapOf( gateData.gateId
                            to mapOf(
                                    "gateId" to gateData.gateId,
                                    "fromName" to userData?.name,
                                    "fromEmail" to userData?.email,
                                    "message" to message
                                    )
                    )
            )


        if ( userData != null ){


            if(gateData.admin == "true") {
                Log.d("Firestore", "Gate data does not exist for this ID: ${gateData.gateId}")
                uploadGateData(gateData) {
                    if (it) Log.d("Firestore", "Gate data uploaded for this ID: ${gateData.gateId}")
                    else Log.e("Firestore", "Error uploading gate data for this ID: ${gateData.gateId}")
                }
            }


            db.collection("Users")
                .document(newGateUser.email)
                .set(invitationMap, SetOptions.merge())
                .addOnSuccessListener {
                    db.collection("Devices/${gateData.gateId}/GateUser")
                        .document(newGateUser.email)
                        .set(
                            mapOf(
                                "name" to newGateUser.name,
                                "boundExtension" to newGateUser.boundExtension,
                                "independent" to newGateUser.independent,
                                "accessFrom" to userData.email,
                                "admin" to false,
                                "daysAvailable" to newGateUser.daysRestricted,
                                "deadline" to newGateUser.deadline,
                                "departureTime" to newGateUser.departureTime,
                                "entryTime" to newGateUser.entryTime,
                                "enabled" to newGateUser.enabled,
                                "shareable" to newGateUser.shareable,
                                "allowHistory" to newGateUser.allowHistory,
                                "allowManageAccess" to newGateUser.allowManageAccess,
                                "allowGateConfig" to newGateUser.allowGateConfig,
                                "restrictions" to newGateUser.restrictions,
                                "privateRestrictions" to newGateUser.privateRestrictions,
                                "pending" to true,
                            ), SetOptions.merge()
                        )
                        .addOnSuccessListener {
                            callback(true, "Invitación enviada")
                        }
                        .addOnFailureListener {
                            callback(false, "Error al enviar la invitación")
                            Log.e("Firestore", "Error saving user data", it)
                        }
                }
                .addOnFailureListener {
                    callback(false, "Error al enviar la invitación")
                }
        }
        else{
            Log.e("Firestore", "User data is null")
            callback(false, "Error al enviar la invitación")
        }

    }



    fun saveOrderInHistory(userData: UserData?, deviceId: String, order: Orders){

        val dateArray = dateUtilities.millisToLocalDateArray(System.currentTimeMillis())

        if(userData == null) {
            Log.e("Firestore", "User data is null")
            return
        }
        when(order) {
            Orders.OPEN, Orders.CLOSE, Orders.STOP -> {
                db.collection("Devices/$deviceId/History")
                    .document(dateArray[0])
                    .get()
                    .addOnSuccessListener {
                        if(it.exists()){
                            db.collection("Devices/$deviceId/History/${dateArray[0]}/${dateArray[1]}")
                                .document(dateArray[2])
                                .set(mapOf( System.currentTimeMillis().toString() to mapOf("action" to order, "user" to userData.name + " " + userData.lastName)), SetOptions.merge())
                                .addOnSuccessListener {
                                    Log.d("Firestore", "Order saved in history")
                                }
                                .addOnFailureListener { e ->
                                    Log.w("Firestore", "Error saving order in history", e)
                                }

                        }
                        else{

                            db.collection("Devices/$deviceId/History")
                                .document(dateArray[0])
                                .set(mapOf("Note" to "Orders in ${dateArray[0]}"))
                                .addOnSuccessListener {
                                    db.collection("Devices/$deviceId/History/${dateArray[0]}/${dateArray[1]}")
                                        .document(dateArray[2])
                                        .set(mapOf( System.currentTimeMillis().toString() to mapOf("action" to order, "user" to userData.name + " " + userData.lastName)), SetOptions.merge())

                                }
                        }
                    }

            }
            else -> {

            }
        }
    }

    fun getHistoryYearMonth(deviceId: String, year: String, month: String, callback: (List<HistoryData>) -> Unit){

        Log.d("Firestore", "Getting history for year $year and month $month")
        val result = mutableListOf<HistoryData>()
        db.collection("Devices/$deviceId/History/$year/$month")
            .get()
            .addOnSuccessListener { days ->
                if(days.isEmpty){
                    callback(emptyList())
                }
                else{
                    days.forEach { day ->
                       day.data.keys.forEach { time ->
                           val timeData = day.data[time] as? Map<*,*> ?: emptyMap<String,String>()
                           result.add(
                                   HistoryData(
                                       year = year,
                                       month = month,
                                       day = day.id,
                                       time = time,
                                       order = timeData["action"].toString(),
                                       user = timeData["user"].toString(),
                                   )
                           )
                       }
                    }
                    Log.d("Firestore", "History retrieved: $result")
                    callback(result)
                }
            }
            .addOnFailureListener {
                callback(emptyList())
            }

    }

    fun getUserData(email: String, callback:(userData: UserData?) -> Unit) {

        var userData: UserData? = null

        if(email != "") {
            db.collection("Users")
                .document(email)
                .get()
                .addOnSuccessListener { documentSnapshot ->

                    val data = documentSnapshot.data?.get("Data") as? Map<*, *>
                    val extensions = documentSnapshot.data?.get("Extensions") as? Map<*, *>
                    val sharedExtensions =
                        documentSnapshot.data?.get("SharedExtensions") as? Map<*, *>
                    val invitations = documentSnapshot.data?.get("invitation") as? Map<*, *>
                    val invitationList = invitationMapToInvitationObject(invitations)
                    val extensionsList = extensionMapToExtensionObject(extensions)
                    val sharedExtensionsList = extensionsMapToSExtensionObject(sharedExtensions)

                    if (data != null) {
                        userData = UserData(
                            email = documentSnapshot.id,
                            name = data["name"].toString(),
                            lastName = data["lastName"].toString(),
                            subscription = data["subscription"].toString().toBoolean(),
                            phone = data["phone"].toString(),
                            extensions = extensionsList,
                            sharedExtensions = sharedExtensionsList
                        )
                        Log.d("Firestore", "User data retrieved: $userData")
                        callback(userData)

                    }
                    else{
                        Log.e("Firestore", "Data is null")
                        callback(null)
                    }

                }
                .addOnFailureListener {
                    callback(null)
                    Log.e("Firestore", "Error getting document: ", it)
                }
        }
        else{
            Log.e("Firestore", "Email is empty")
            callback(null)
        }

    }

    fun invitationMapToInvitationObject(invitations: Map<*,*>?): List<Invitation> {
        val result = mutableListOf<Invitation>()

        invitations?.keys?.forEach { email ->
            val invitation = invitations[email] as? Map<*, *>
            result += Invitation(
                email = email.toString(),
                gateId = invitation?.get("gateId").toString(),
                fromName = invitation?.get("fromName").toString(),
                fromEmail = invitation?.get("fromEmail").toString(),
                message = invitation?.get("message").toString(),
            )
        }
        return result
    }
    fun extensionMapToExtensionObject(extensionMap: Map<*, *>?): List<GateUser> {

        val result = mutableListOf<GateUser>()

        extensionMap?.keys?.forEach { gate ->
            val users = extensionMap[gate] as? Map<*, *>
            users?.keys?.forEach { user ->
                val userMap = users[user] as? Map<*, *>
                result += GateUser(
                    name = userMap?.get("name").toString(),
                    gateId = gate.toString(),
                    email = user.toString(),
                    boundExtension = userMap?.get("boundExtension") as? Boolean ?: false,
                    daysRestricted = userMap?.get("daysAvailable").toString(),
                    deadline = userMap?.get("deadline").toString(),
                    departureTime = userMap?.get("departureTime").toString(),
                    entryTime = userMap?.get("entryTime").toString(),
                    allowHistory = userMap?.get("allowHistory") as? Boolean ?: false,
                    allowManageAccess = userMap?.get("allowManageAccess") as? Boolean ?: false,
                    allowGateConfig = userMap?.get("allowGateConfig") as? Boolean ?: false,
                    enabled = userMap?.get("enabled") as? Boolean ?: false,
                    shareable = userMap?.get("shareable") as? Boolean ?: false,
                    independent = userMap?.get("independent") as? Boolean ?: false,
                    restrictions = userMap?.get("restrictions") as? Boolean ?: false,
                    privateRestrictions = userMap?.get("privateRestrictions") as? Boolean ?: false,
                    status = userMap?.get("status").toString()
                )

            }

        }

        return result
    }

    fun extensionsMapToSExtensionObject(sharedExtensionMap: Map<*, *>?): List<SharedExtensionAccess> {

        val result = mutableListOf<SharedExtensionAccess>()

        sharedExtensionMap?.keys?.forEach { email ->
            val emails = sharedExtensionMap[email] as? Map<*, *>
            emails?.keys?.forEach { gateId ->
                val userMap = emails[gateId] as? Map<*, *>
                result += SharedExtensionAccess(
                    name = userMap?.get("name").toString(),
                    gateId = gateId.toString(),
                    accessFrom = email.toString(),
                    boundExtension = userMap?.get("boundExtension") as? Boolean ?: false,
                    daysRestricted = userMap?.get("daysAvailable").toString(),
                    deadline = userMap?.get("deadline").toString(),
                    departureTime = userMap?.get("departureTime").toString(),
                    entryTime = userMap?.get("entryTime").toString(),
                    enabled = userMap?.get("enabled") as? Boolean ?: false,
                    shareable = userMap?.get("shareable") as? Boolean ?: false,
                    allowHistory = userMap?.get("allowHistory") as? Boolean ?: false,
                    allowManageAccess = userMap?.get("allowManageAccess") as? Boolean ?: false,
                    allowGateConfig = userMap?.get("allowGateConfig") as? Boolean ?: false,
                    message = userMap?.get("message").toString(),
                    restrictions = userMap?.get("restrictions") as? Boolean ?: false,
                    privateRestrictions = userMap?.get("privateRestrictions") as? Boolean ?: false,
                    status = userMap?.get("status").toString(),
                    mac = userMap?.get("mac").toString(),
                    password = userMap?.get("password").toString(),
                    pPassword = userMap?.get("pPassword").toString(),
                    characteristic = userMap?.get("characteristic").toString(),
                    characteristicRx = userMap?.get("characteristicRx").toString(),
                    service = userMap?.get("service").toString(),
                    gateType = userMap?.get("gateType").toString(),
                    independent = userMap?.get("shareEnable") as? Boolean ?: false,
                    version = userMap?.get("version").toString(),
                    gateName = userMap?.get("gateName").toString(),

                )

            }


        }

        return result


    }
    fun signIn(email: String, password: String, callback: (ok: Boolean, userData: UserData?, errorMessage: String?) -> Unit){
        db.collection("Users")
            .document(email)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val data = documentSnapshot.data?.get("Data") as? Map<*, *>
                val passwordDb = data?.get("password")?.toString() ?: ""
                if(passwordDb == password){
                    val userData = UserData(
                        email = documentSnapshot.id,
                        name = data?.get("name").toString() ,
                        phone = data?.get("phone").toString(),
                        subscription = data?.get("subscription").toString().toBoolean(),
                        profileImageUrl = data?.get("profileImageUrl").toString()
                    )
                    callback(true, userData, null)

                }
                else{
                    callback(false, null, "La contraseña no coincide")
                }

            }
            .addOnFailureListener {
                callback(false, null, "Error de conexión")
            }
    }

    fun signUp(name: String, phone: String, email: String, password: String, callback: (ok: Boolean, message: String, userData: UserData?) -> Unit) {

        checkEmailExists(email){ emailExists ->
            if(!emailExists){
                db.collection("Users")
                    .document(email)
                    .set(
                        mapOf(
                            "Data" to mapOf(
                                "name" to name,
                                "password" to password,
                                "phone" to phone,
                                "subscription" to false,
                                "profileImageUrl" to ""
                            )
                        )
                    )
                    .addOnSuccessListener {
                        val userData = UserData(
                            name = name,
                            phone = phone,
                            profileImageUrl = "",
                            subscription = false,
                            email = email
                        )
                        callback(true, "Usuario registrado correctamente.", userData)
                    }
                    .addOnFailureListener {
                        callback(false, "Error al registrar el usuario.", null)
                    }
            }
            else{
                callback(false, "El correo electrónico ya está registrado.", null)
            }
        }
    }
}


