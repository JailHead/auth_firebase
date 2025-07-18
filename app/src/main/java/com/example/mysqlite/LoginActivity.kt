package com.example.mysqlite

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsuario: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnInfo: Button
    private lateinit var sharedPreferences: SharedPreferences

    // Credenciales falsas para el login
    private val usuarioValido = "admin"
    private val passwordValido = "123456"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializar SharedPreferences para mantener sesión
        sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE)

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

        // Configurar eventos
        btnLogin.setOnClickListener {
            realizarLogin()
        }

        btnInfo.setOnClickListener {
            mostrarCredenciales()
        }
    }

    private fun realizarLogin() {
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
            guardarSesion()
            Toast.makeText(this, "¡Bienvenido $usuario!", Toast.LENGTH_SHORT).show()
            navegarAMainActivity()
        } else {
            // Login fallido
            Toast.makeText(this, "Usuario o contraseña incorrectos", Toast.LENGTH_LONG).show()
            limpiarCampos()
        }
    }

    private fun guardarSesion() {
        val editor = sharedPreferences.edit()
        editor.putBoolean("isLoggedIn", true)
        editor.putString("username", usuarioValido)
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