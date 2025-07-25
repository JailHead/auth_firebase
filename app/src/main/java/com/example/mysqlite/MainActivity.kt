package com.example.mysqlite

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.AsyncHttpResponseHandler
import cz.msebera.android.httpclient.Header
import org.json.JSONArray
import org.json.JSONException
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

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
    private lateinit var requestQueue: RequestQueue

    private lateinit var auth: FirebaseAuth

    private val url = "http://ec2-100-27-195-37.compute-1.amazonaws.com/api/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

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
        requestQueue = Volley.newRequestQueue(this)

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
                    "con conexión a base de datos MySQL\n" +
                    "en servidor AWS EC2.\n\n" +
                    "Soporte para autenticación múltiple:\n" +
                    "• Google Sign-In (Firebase)\n" +
                    "• Sistema tradicional\n\n" +
                    "$authInfo\n\n" +
                    "Desarrollado con Android Studio")
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

    private fun ejecutarWebService(url: String, msg: String) {
        Toast.makeText(applicationContext,msg, Toast.LENGTH_LONG).show()
        val stringRequest = StringRequest(Request.Method.GET, url, {
            fun onResponse(response: String?) {
                Toast.makeText(this@MainActivity, response.toString(), Toast.LENGTH_SHORT).show()
            }
        },
            {
                fun onErrorResponse(error: VolleyError) {
                    Toast.makeText(this@MainActivity, error.toString(), Toast.LENGTH_SHORT).show()
                }
            })

        val requestQueue: RequestQueue = Volley.newRequestQueue(this@MainActivity)
        requestQueue.add(stringRequest)
    }

    private fun agregarContacto() {
        ejecutarWebService(
            url + "androidInsercionMySql.php?nombre=" +
                    etNombre.text + "&apellidos=" + etApellidos.text +
                    "&telefono=" + etTelefono.text + "&email=" +
                    etCorreo.text,
            "Contacto registrado."
        )

        limpiarCampos()
    }

    private fun buscarContacto() {
        // Validar que los campos requeridos no estén vacíos
        if (etNombre.text.toString().trim().isEmpty() || etApellidos.text.toString().trim().isEmpty()) {
            Toast.makeText(this, "Ingrese nombre y apellidos para buscar", Toast.LENGTH_SHORT).show()
            return
        }

        //Instancia que recibe la información del servidor
        val cliente = AsyncHttpClient()

        // Construir URL con parámetros
        val urlBusqueda = url + "androidBusquedaMySql.php?nombre=" +
                etNombre.text.toString().trim() + "&apellidos=" + etApellidos.text.toString().trim()

        Log.d("MainActivity", "URL de búsqueda: $urlBusqueda") // Para debug

        //Llamada al archivo PHP
        cliente.get(urlBusqueda, object : AsyncHttpResponseHandler() {
            override fun onSuccess(
                statusCode: Int,
                headers: Array<out Header>?,
                responseBody: ByteArray?
            ) {
                //El código 200 indica que la petición fue exitosa
                if (statusCode == 200 && responseBody != null) {
                    try {
                        val responseString = String(responseBody, Charsets.UTF_8)
                        Log.d("MainActivity", "Response búsqueda: $responseString") // Para debug

                        //Si existen registros como resultado de la búsqueda
                        if (responseString.isNotEmpty() && responseString != "0") {

                            val contacto = JSONArray(responseString)

                            runOnUiThread {
                                if (contacto.length() > 0) {
                                    val contactoEncontrado = contacto.getJSONObject(0)

                                    // Llenar teléfono si existe en la respuesta
                                    if (contactoEncontrado.has("Telefono")) {
                                        etTelefono.setText(contactoEncontrado.getString("Telefono"))
                                    }

                                    // Llenar email
                                    if (contactoEncontrado.has("Email")) {
                                        etCorreo.setText(contactoEncontrado.getString("Email"))
                                    }

                                    Toast.makeText(
                                        this@MainActivity,
                                        "Contacto encontrado",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                        } else {
                            runOnUiThread {
                                // Limpiar campos si no se encuentra
                                etTelefono.setText("")
                                etCorreo.setText("")

                                Toast.makeText(
                                    this@MainActivity,
                                    "Contacto no encontrado.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                    } catch (e: JSONException) {
                        Log.e("MainActivity", "JSON Error en búsqueda: ${e.message}")
                        runOnUiThread {
                            Toast.makeText(
                                this@MainActivity,
                                "Error al procesar información: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error en búsqueda: ${e.message}")
                        runOnUiThread {
                            Toast.makeText(
                                this@MainActivity,
                                "Error: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Sin resultados en búsqueda.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            override fun onFailure(
                statusCode: Int,
                headers: Array<out Header>?,
                responseBody: ByteArray?,
                error: Throwable?
            ) {
                val errorMessage = if (responseBody != null) {
                    String(responseBody, Charsets.UTF_8)
                } else {
                    "Error de conexión"
                }

                Log.e("MainActivity", "HTTP Error en búsqueda $statusCode: $errorMessage")
                Log.e("MainActivity", "Exception: ${error?.message}")

                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Error de conexión: $statusCode - ${error?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        })
    }

    private fun actualizarContacto() {
        ejecutarWebService(
            url + "androidActualizacionMySql.php?nombre=" +
                    etNombre.text + "&apellidos=" + etApellidos.text +
                    "&telefono=" + etTelefono.text + "&email=" +
                    etCorreo.text,
            "Contacto actualizado."
        )
        limpiarCampos()
    }

    private fun eliminarContacto() {
        ejecutarWebService(
            url + "androidEliminacionMySql.php?nombre=" +
                    etNombre.text + "&apellidos=" + etApellidos.text,
            "Contacto eliminado."
        )
        limpiarCampos()
    }

    private fun limpiarCampos() {
        etNombre.setText("")
        etApellidos.setText("")
        etTelefono.setText("")
        etCorreo.setText("")
        etNombre.requestFocus()
    }
}