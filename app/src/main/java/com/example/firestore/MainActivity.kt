package com.example.firestore

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.firestore

class MainActivity : AppCompatActivity() {
    private lateinit var etNumEmp: EditText
    private lateinit var etNombre: EditText
    private lateinit var etApellidos: EditText
    private lateinit var etTelefono: EditText
    private lateinit var etCorreo: EditText
    private lateinit var btnAgregar: ImageButton
    private lateinit var btnBuscar: ImageButton
    private lateinit var btnActualizar: ImageButton
    private lateinit var btnEliminar: ImageButton
    private lateinit var btnLista: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        firestore = Firebase.firestore

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        configurarActionBar()

        etNombre = findViewById(R.id.txtNombre)
        etApellidos = findViewById(R.id.txtApellidos)
        etTelefono = findViewById(R.id.txtTelefono)
        etCorreo = findViewById(R.id.txtCorreo)
        btnAgregar = findViewById(R.id.btnAgregar)
        btnBuscar = findViewById(R.id.btnBuscar)
        btnActualizar = findViewById(R.id.btnActualizar)
        btnEliminar = findViewById(R.id.btnEliminar)
        btnLista = findViewById(R.id.btnLista)

        btnAgregar.setOnClickListener {
            agregarContacto()
        }
        btnBuscar.setOnClickListener {
            buscarContacto()
        }
        btnActualizar.setOnClickListener{
            actualizarContacto()
        }
        btnEliminar.setOnClickListener {
            eliminarContacto()
        }
        btnLista.setOnClickListener {
            val intent = Intent(this, ListadoActivity::class.java)
            startActivity(intent)
        }
    }

    private fun configurarActionBar() {
        val sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
        val authType = sharedPreferences.getString("authType", "traditional")
        val username = sharedPreferences.getString("username", "Usuario")

        supportActionBar?.title = "Gestión de Contactos"

        // Mostrar diferente subtítulo según el tipo de autenticación
        when (authType) {
            "google" -> {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    supportActionBar?.subtitle = "Bienvenido ${currentUser.displayName ?: currentUser.email}"
                } else {
                    supportActionBar?.subtitle = "Bienvenido $username"
                }
            }
            "traditional" -> {
                supportActionBar?.subtitle = "Bienvenido $username"
            }
            else -> {
                supportActionBar?.subtitle = "Bienvenido"
            }
        }
    }

    override fun onStart() {
        super.onStart()
        verificarEstadoAutenticacion()
    }

    private fun verificarEstadoAutenticacion() {
        val sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
        val authType = sharedPreferences.getString("authType", "traditional")

        if (authType == "google") {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                // Usuario de Google desconectado, regresar al login
                Toast.makeText(this, "Sesión de Google expirada", Toast.LENGTH_SHORT).show()
                LoginActivity.cerrarSesionGlobal(this)
                return
            } else {
                Log.d("MainActivity", "Usuario Google activo: ${currentUser.email}")
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                mostrarDialogoLogout()
                true
            }
            R.id.action_about -> {
                mostrarAcercaDe()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun mostrarDialogoLogout() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Está seguro que desea cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                realizarLogout()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun realizarLogout() {
        val sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
        val authType = sharedPreferences.getString("authType", "traditional")

        // Mostrar mensaje personalizado según el tipo de autenticación
        val message = when (authType) {
            "google" -> "Sesión de Google cerrada"
            "traditional" -> "Sesión cerrada"
            else -> "Sesión cerrada"
        }

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        LoginActivity.cerrarSesionGlobal(this)
    }

    private fun mostrarAcercaDe() {
        val sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
        val authType = sharedPreferences.getString("authType", "traditional")
        val username = sharedPreferences.getString("username", "Usuario")

        val authInfo = when (authType) {
            "google" -> {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    "Autenticado con: Google\nUsuario: ${currentUser.displayName ?: currentUser.email}\nID: ${currentUser.uid}"
                } else {
                    "Autenticado con: Google (sesión local)"
                }
            }
            "traditional" -> "Autenticado con: Sistema tradicional\nUsuario: $username"
            else -> "Autenticado con: Sistema desconocido"
        }

        AlertDialog.Builder(this)
            .setTitle("Acerca de la App")
            .setMessage("Gestión de Contactos v1.0\n\n" +
                    "Aplicación para administrar contactos\n" +
                    "con conexión a Firebase\n" +
                    "Soporte para autenticación múltiple:\n" +
                    "• Google Sign-In (Firebase)\n" +
                    "• Sistema tradicional\n\n" +
                    "$authInfo\n\n")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        AlertDialog.Builder(this)
            .setTitle("Salir de la App")
            .setMessage("¿Desea salir de la aplicación?")
            .setPositiveButton("Salir") { _, _ ->
                finishAffinity() // Cierra toda la app
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun agregarContacto() {
        // Verificar que el usuario esté autenticado
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        // Obtener datos de los campos (igual que antes)
        val nombre = etNombre.text.toString().trim()
        val apellidos = etApellidos.text.toString().trim()
        val telefono = etTelefono.text.toString().trim()
        val correo = etCorreo.text.toString().trim()

        // Validación básica (opcional, puedes agregar más validaciones)
        if (nombre.isEmpty() || apellidos.isEmpty()) {
            Toast.makeText(this, "Por favor ingrese nombre y apellidos", Toast.LENGTH_SHORT).show()
            return
        }

        // Crear el objeto contacto para Firebase
        val contacto = hashMapOf(
            "nombre" to nombre,
            "apellidos" to apellidos,
            "telefono" to telefono,
            "email" to correo,
            "userId" to currentUser.uid, // Para filtrar por usuario
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        // Guardar en Firebase Firestore
        firestore.collection("contactos")
            .add(contacto)
            .addOnSuccessListener { documentReference ->
                // Éxito - mismo mensaje que antes
                Toast.makeText(this, "Contacto registrado.", Toast.LENGTH_SHORT).show()
                Log.d("Firebase", "Contacto creado con ID: ${documentReference.id}")

                // Limpiar campos igual que antes
                limpiarCampos()
            }
            .addOnFailureListener { e ->
                // Error - mostrar mensaje de error
                Log.e("Firebase", "Error al crear contacto", e)

                // Mensaje de error más específico basado en el tipo
                val errorMessage = when (e) {
                    is FirebaseFirestoreException -> {
                        when (e.code) {
                            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                                "Error: Permisos insuficientes"
                            FirebaseFirestoreException.Code.UNAVAILABLE ->
                                "Error: Servicio no disponible, intente más tarde"
                            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                                "Error: Revise su conexión a internet"
                            else -> "Error al registrar contacto: ${e.message}"
                        }
                    }
                    else -> "Error al registrar contacto: ${e.message}"
                }

                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
    }

    private fun buscarContacto() {
        // Verificar que el usuario esté autenticado
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        // Validación igual que antes - mantener la misma lógica
        if (etNombre.text.toString().trim().isEmpty() || etApellidos.text.toString().trim().isEmpty()) {
            Toast.makeText(this, "Ingrese nombre y apellidos para buscar", Toast.LENGTH_SHORT).show()
            return
        }

        val nombre = etNombre.text.toString().trim()
        val apellidos = etApellidos.text.toString().trim()

        Log.d("MainActivity", "Buscando contacto: $nombre $apellidos")

        // Buscar en Firebase Firestore
        firestore.collection("contactos")
            .whereEqualTo("userId", currentUser.uid) // Solo contactos del usuario actual
            .whereEqualTo("nombre", nombre)
            .whereEqualTo("apellidos", apellidos)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Contacto encontrado - llenar campos igual que antes
                    val contacto = documents.documents[0]

                    // Llenar los campos teléfono y email (mismo comportamiento que antes)
                    etTelefono.setText(contacto.getString("telefono") ?: "")
                    etCorreo.setText(contacto.getString("email") ?: "")

                    // Mismo mensaje de éxito
                    Toast.makeText(this@MainActivity, "Contacto encontrado", Toast.LENGTH_SHORT).show()

                    Log.d("MainActivity", "Contacto encontrado: ${contacto.id}")
                } else {
                    // Contacto no encontrado - limpiar campos igual que antes
                    etTelefono.setText("")
                    etCorreo.setText("")

                    // Mismo mensaje que antes
                    Toast.makeText(this@MainActivity, "Contacto no encontrado.", Toast.LENGTH_SHORT).show()

                    Log.d("MainActivity", "Contacto no encontrado")
                }
            }
            .addOnFailureListener { e ->
                // Error en la búsqueda
                Log.e("MainActivity", "Error en búsqueda: ${e.message}", e)

                // Limpiar campos en caso de error
                etTelefono.setText("")
                etCorreo.setText("")

                // Mensaje de error más específico
                val errorMessage = when (e) {
                    is FirebaseFirestoreException -> {
                        when (e.code) {
                            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                                "Error: Sin permisos para buscar contactos"
                            FirebaseFirestoreException.Code.UNAVAILABLE ->
                                "Error: Servicio no disponible"
                            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                                "Error: Revise su conexión a internet"
                            else -> "Error de búsqueda: ${e.message}"
                        }
                    }
                    else -> "Error de búsqueda: ${e.message}"
                }

                Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_LONG).show()
            }
    }

    private fun actualizarContacto() {
        // Verificar que el usuario esté autenticado
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        // Obtener datos de los campos
        val nombre = etNombre.text.toString().trim()
        val apellidos = etApellidos.text.toString().trim()
        val telefono = etTelefono.text.toString().trim()
        val correo = etCorreo.text.toString().trim()

        // Validación - necesitamos nombre y apellidos para buscar el contacto
        if (nombre.isEmpty() || apellidos.isEmpty()) {
            Toast.makeText(this, "Ingrese nombre y apellidos para actualizar", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("MainActivity", "Actualizando contacto: $nombre $apellidos")

        // Primero buscar el contacto existente
        firestore.collection("contactos")
            .whereEqualTo("userId", currentUser.uid)
            .whereEqualTo("nombre", nombre)
            .whereEqualTo("apellidos", apellidos)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Contacto encontrado - proceder con la actualización
                    val documentId = documents.documents[0].id

                    // Crear el mapa de actualizaciones
                    val updates = hashMapOf<String, Any>(
                        "telefono" to telefono,
                        "email" to correo,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )

                    // Actualizar el documento en Firebase
                    firestore.collection("contactos")
                        .document(documentId)
                        .update(updates)
                        .addOnSuccessListener {
                            // Éxito - mismo mensaje que antes
                            Toast.makeText(this@MainActivity, "Contacto actualizado.", Toast.LENGTH_SHORT).show()
                            Log.d("MainActivity", "Contacto actualizado: $documentId")

                            // Limpiar campos igual que antes
                            limpiarCampos()
                        }
                        .addOnFailureListener { e ->
                            // Error al actualizar
                            Log.e("MainActivity", "Error al actualizar contacto", e)

                            val errorMessage = when (e) {
                                is FirebaseFirestoreException -> {
                                    when (e.code) {
                                        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                                            "Error: Sin permisos para actualizar contacto"
                                        FirebaseFirestoreException.Code.NOT_FOUND ->
                                            "Error: Contacto no encontrado"
                                        FirebaseFirestoreException.Code.UNAVAILABLE ->
                                            "Error: Servicio no disponible"
                                        else -> "Error al actualizar: ${e.message}"
                                    }
                                }
                                else -> "Error al actualizar: ${e.message}"
                            }

                            Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_LONG).show()
                        }
                } else {
                    // Contacto no encontrado para actualizar
                    Toast.makeText(this@MainActivity, "Contacto no encontrado para actualizar.", Toast.LENGTH_SHORT).show()
                    Log.d("MainActivity", "Contacto no encontrado para actualizar")
                }
            }
            .addOnFailureListener { e ->
                // Error al buscar el contacto
                Log.e("MainActivity", "Error al buscar contacto para actualizar", e)

                val errorMessage = when (e) {
                    is FirebaseFirestoreException -> {
                        when (e.code) {
                            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                                "Error: Sin permisos para buscar contactos"
                            FirebaseFirestoreException.Code.UNAVAILABLE ->
                                "Error: Servicio no disponible"
                            else -> "Error al buscar contacto: ${e.message}"
                        }
                    }
                    else -> "Error al buscar contacto: ${e.message}"
                }

                Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_LONG).show()
            }
    }

    private fun eliminarContacto() {
        // Verificar que el usuario esté autenticado
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        // Obtener datos de los campos
        val nombre = etNombre.text.toString().trim()
        val apellidos = etApellidos.text.toString().trim()

        // Validación - necesitamos nombre y apellidos para buscar el contacto
        if (nombre.isEmpty() || apellidos.isEmpty()) {
            Toast.makeText(this, "Ingrese nombre y apellidos para eliminar", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("MainActivity", "Eliminando contacto: $nombre $apellidos")

        // Buscar el contacto que se quiere eliminar
        firestore.collection("contactos")
            .whereEqualTo("userId", currentUser.uid)
            .whereEqualTo("nombre", nombre)
            .whereEqualTo("apellidos", apellidos)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Contacto encontrado - proceder con la eliminación
                    val documentId = documents.documents[0].id
                    val contactoData = documents.documents[0].data

                    Log.d("MainActivity", "Eliminando documento: $documentId")

                    // Eliminar el documento de Firebase
                    firestore.collection("contactos")
                        .document(documentId)
                        .delete()
                        .addOnSuccessListener {
                            // Éxito - mismo mensaje que antes
                            Toast.makeText(this@MainActivity, "Contacto eliminado.", Toast.LENGTH_SHORT).show()
                            Log.d("MainActivity", "Contacto eliminado exitosamente: $documentId")

                            // Limpiar campos igual que antes
                            limpiarCampos()
                        }
                        .addOnFailureListener { e ->
                            // Error al eliminar
                            Log.e("MainActivity", "Error al eliminar contacto", e)

                            val errorMessage = when (e) {
                                is FirebaseFirestoreException -> {
                                    when (e.code) {
                                        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                                            "Error: Sin permisos para eliminar contacto"
                                        FirebaseFirestoreException.Code.NOT_FOUND ->
                                            "Error: Contacto no encontrado"
                                        FirebaseFirestoreException.Code.UNAVAILABLE ->
                                            "Error: Servicio no disponible"
                                        else -> "Error al eliminar: ${e.message}"
                                    }
                                }
                                else -> "Error al eliminar: ${e.message}"
                            }

                            Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_LONG).show()
                        }
                } else {
                    // Contacto no encontrado para eliminar
                    Toast.makeText(this@MainActivity, "Contacto no encontrado para eliminar.", Toast.LENGTH_SHORT).show()
                    Log.d("MainActivity", "Contacto no encontrado para eliminar")
                }
            }
            .addOnFailureListener { e ->
                // Error al buscar el contacto
                Log.e("MainActivity", "Error al buscar contacto para eliminar", e)

                val errorMessage = when (e) {
                    is FirebaseFirestoreException -> {
                        when (e.code) {
                            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                                "Error: Sin permisos para buscar contactos"
                            FirebaseFirestoreException.Code.UNAVAILABLE ->
                                "Error: Servicio no disponible"
                            else -> "Error al buscar contacto: ${e.message}"
                        }
                    }
                    else -> "Error al buscar contacto: ${e.message}"
                }

                Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_LONG).show()
            }
    }

    private fun limpiarCampos() {
        etNombre.setText("")
        etApellidos.setText("")
        etTelefono.setText("")
        etCorreo.setText("")
        etNombre.requestFocus()
    }
}