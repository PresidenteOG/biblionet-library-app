package com.biblionet.ui.viewmodel.user

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.biblionet.data.model.auth.Usuario
import com.biblionet.data.repository.auth.UsuarioRepository
import com.biblionet.utils.GlobalState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UserViewModel : ViewModel() {

    private val repository = UsuarioRepository()

    // Estado del usuario reactivo para la UI
    var usuario by mutableStateOf<Usuario?>(null)
        private set

    // Control de carga para la UI
    var isLoading by mutableStateOf(true)
        private set

    private var userObservationJob: Job? = null

    // El AuthStateListener detecta cambios de sesión (Login, Logout, Registro)
    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val uid = firebaseAuth.currentUser?.uid
        if (uid != null) {
            startFirestoreListener(uid)
        } else {
            clearUserData()
        }
    }

    private val _userNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val userNames: StateFlow<Map<String, String>> = _userNames.asStateFlow()

    init {
        // Al iniciar, comprobamos el estado actual inmediatamente
        val currentUid = repository.getCurrentUid()
        if (currentUid != null) {
            startFirestoreListener(currentUid)
        } else {
            isLoading = false
        }
        
        // Registramos el escuchador para cambios de autenticación
        repository.addAuthStateListener(authStateListener)
    }

    /**
     * Escucha cambios en el documento del usuario a través del repositorio.
     */
    private fun startFirestoreListener(uid: String) {
        userObservationJob?.cancel()
        isLoading = true
        userObservationJob = viewModelScope.launch {
            repository.observeUsuario(uid).collect { userObj ->
                usuario = userObj
                
                // Lógica de forzar cierre de sesión
                if (userObj?.forceLogout == true) {
                    repository.resetForceLogout(uid)
                    logout()
                }
                isLoading = false
            }
        }
    }

    private fun clearUserData() {
        userObservationJob?.cancel()
        usuario = null
        isLoading = false
    }

    fun actualizarPerfil(
        nombre: String,
        telefono: String,
        fotoUrl: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = repository.getCurrentUid() ?: return

        val updates = mutableMapOf<String, Any>(
            "nombre" to nombre,
            "telefono" to telefono
        )

        if (!fotoUrl.isNullOrEmpty()) {
            updates["foto_url"] = fotoUrl
        }

        viewModelScope.launch {
            repository.actualizarPerfil(uid, updates)
                .onSuccess { onSuccess() }
                .onFailure { e -> onError(e.localizedMessage ?: "Error desconocido") }
        }
    }

    fun logout() {
        GlobalState.finalizarSesion()
        repository.logout()
    }

    override fun onCleared() {
        super.onCleared()
        repository.removeAuthStateListener(authStateListener)
        userObservationJob?.cancel()
    }

    private val nombresCache = mutableMapOf<String, String>()

    suspend fun getNombreUsuario(uidBuscado: String): String {
        val currentUid = repository.getCurrentUid()
        if (uidBuscado == currentUid) return "Yo"
        if (nombresCache.containsKey(uidBuscado)) {
            return nombresCache[uidBuscado]!!
        }
        
        val nombre = repository.getNombreUsuario(uidBuscado)
        nombresCache[uidBuscado] = nombre
        return nombre
    }

    fun cargarNombresUsuarios(uids: List<String>) {
        val idsToLoad = uids.filter { it.isNotEmpty() && !_userNames.value.containsKey(it) }.distinct()
        if (idsToLoad.isEmpty()) return
        
        viewModelScope.launch {
            val resultMap = mutableMapOf<String, String>()
            idsToLoad.forEach { uid ->
                val name = getNombreUsuario(uid)
                resultMap[uid] = name
            }
            _userNames.update { it + resultMap }
        }
    }

    suspend fun getFotoUsuario(uid: String): String {
        return repository.getFotoUsuario(uid)
    }
}
