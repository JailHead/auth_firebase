package com.example.mysqlite

import android.content.ContentValues.TAG
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

//firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import androidx.credentials.CustomCredential
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsuario: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnInfo: Button
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager
    private lateinit var btnGoogleSignIn: Button

    // Credenciales falsas para el login
    private val usuarioValido = "admin"
    private val passwordValido = "123456"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializar SharedPreferences para mantener sesión
        sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE)

        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)

        // Verificar si el usuario ya está logueado
        if (verificarSesionActiva()) {
            navegarAMainActivity()
            return
        }

        // Asociar componentes
        etUsuario = findViewById(R.id.etUsuario)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnInfo = findViewById(R.id.btnInfo)

        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)

        // Configurar eventos
        btnLogin.setOnClickListener {
            realizarLoginTradicional()
        }

        btnInfo.setOnClickListener {
            mostrarCredenciales()
        }

        btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun realizarLoginTradicional() {
        val usuario = etUsuario.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Validar campos vacíos
        if (usuario.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor ingrese usuario y contraseña", Toast.LENGTH_SHORT).show()
            return
        }

        // Validar credenciales
        if (usuario == usuarioValido && password == passwordValido) {
            // Login exitoso
            guardarSesion(usuario, password)
            Toast.makeText(this, "¡Bienvenido $usuario!", Toast.LENGTH_SHORT).show()
            navegarAMainActivity()
        } else {
            // Login fallido
            Toast.makeText(this, "Usuario o contraseña incorrectos", Toast.LENGTH_LONG).show()
            limpiarCampos()
        }
    }

    private fun signInWithGoogle() {
        Log.d(TAG, "Iniciando Google Sign-In")

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(false) // Permitir selección de cuentas
            .setAutoSelectEnabled(false) // Permitir al usuario elegir
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = this@LoginActivity
                )
                handleSignInResult(result)
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Error en Google Sign-In", e)
                Toast.makeText(
                    this@LoginActivity,
                    "Error al iniciar sesión con Google: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun handleSignInResult(result: GetCredentialResponse) {
        when (val credential = result.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        Log.d(TAG, "Google ID Token obtenido: ${googleCredential.id}")
                        firebaseAuthWithGoogle(googleCredential.idToken)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al procesar credencial de Google", e)
                        Toast.makeText(this, "Error al procesar credencial", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e(TAG, "Tipo de credencial inesperado: ${credential.type}")
                }
            }
            else -> {
                Log.e(TAG, "Tipo de credencial no soportado")
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Firebase authentication exitosa")
                    val user = auth.currentUser
                    if (user != null) {
                        guardarSesionFirebase(user)
                        Toast.makeText(this, "¡Bienvenido ${user.displayName}!", Toast.LENGTH_SHORT).show()
                        navegarAMainActivity()
                    }
                } else {
                    Log.e(TAG, "Firebase authentication falló", task.exception)
                    Toast.makeText(this, "Error de autenticación: ${task.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun guardarSesion(username: String, authType: String) {
        val editor = sharedPreferences.edit()
        editor.putBoolean("isLoggedIn", true)
        editor.putString("username", username)
        editor.putString("authType", authType) // ✅ NUEVO: Tipo de autenticación
        editor.putLong("loginTime", System.currentTimeMillis())
        editor.apply()
    }

    private fun guardarSesionFirebase(user: FirebaseUser) {
        val editor = sharedPreferences.edit()
        editor.putBoolean("isLoggedIn", true)
        editor.putString("username", user.displayName ?: user.email ?: "Usuario Google")
        editor.putString("userEmail", user.email ?: "")
        editor.putString("userId", user.uid)
        editor.putString("authType", "google") // ✅ NUEVO: Tipo de autenticación
        editor.putLong("loginTime", System.currentTimeMillis())
        editor.apply()
    }

    private fun verificarSesionActiva(): Boolean {
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)

        if (isLoggedIn) {
            // Opcional: verificar si la sesión no ha expirado (ejemplo: 24 horas)
            val loginTime = sharedPreferences.getLong("loginTime", 0)
            val currentTime = System.currentTimeMillis()
            val sessionDuration = 24 * 60 * 60 * 1000 // 24 horas en milisegundos

            if (currentTime - loginTime < sessionDuration) {
                return true
            } else {
                // Sesión expirada, limpiar datos
                cerrarSesion()
                return false
            }
        }
        return false
    }

    private fun navegarAMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        // Limpiar stack de activities para que no pueda regresar con back
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun limpiarCampos() {
        etUsuario.setText("")
        etPassword.setText("")
        etUsuario.requestFocus()
    }

    private fun mostrarCredenciales() {
        AlertDialog.Builder(this)
            .setTitle("Credenciales de Prueba")
            .setMessage("Usuario: $usuarioValido\nContraseña: $passwordValido")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // Función pública para cerrar sesión (llamada desde MainActivity)
    fun cerrarSesion() {
        // Cerrar sesión de Firebase si está activo
        if (auth.currentUser != null) {
            auth.signOut()
        }

        // Limpiar SharedPreferences
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }

    companion object {
        // Función estática para cerrar sesión desde cualquier activity
        fun cerrarSesionGlobal(activity: AppCompatActivity) {
            val sharedPreferences = activity.getSharedPreferences("LoginPrefs", MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.clear()
            editor.apply()

            val intent = Intent(activity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            activity.startActivity(intent)
            activity.finish()
        }
    }
}