package com.example.mysqlite

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.AsyncHttpResponseHandler
import cz.msebera.android.httpclient.Header
import org.json.JSONArray
import org.json.JSONException

class ListadoActivity : AppCompatActivity() {
    // Variables simplificadas
    private lateinit var txtDetalle: TextView
    private lateinit var txtContador: TextView
    private lateinit var btnRegresar: Button
    private lateinit var btnActualizar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listado)

        // Asociar componentes
        txtDetalle = findViewById(R.id.txtDetalle)
        txtContador = findViewById(R.id.txtContador)
        btnRegresar = findViewById(R.id.btnRegresar)
        btnActualizar = findViewById(R.id.btnActualizar)

        txtDetalle.setBackgroundColor(android.graphics.Color.WHITE)
        txtDetalle.setTextColor(android.graphics.Color.BLACK)
        txtDetalle.textSize = 14f

        // Configurar botones
        btnActualizar.setOnClickListener {
            listarContactos()
        }
        btnRegresar.setOnClickListener {
            finish()
        }

        listarContactos()
    }

    private fun listarContactos() {
        Log.d("DEBUG", "listarContactos - INICIADO")

        // Estado de carga visible
        txtContador.text = "Consultando servidor..."
        txtDetalle.text = "Conectando a la base de datos...\n\nEspere un momento..."

        val cliente = AsyncHttpClient()
        val url = "http://ec2-100-27-195-37.compute-1.amazonaws.com/api/androidConsultaMySql.php"

        cliente.get(url, object : AsyncHttpResponseHandler() {
            override fun onSuccess(
                statusCode: Int,
                headers: Array<Header>,
                responseBody: ByteArray
            ) {
                Log.d("DEBUG", "onSuccess - RECIBIDO")

                var responseString = ""

                try {
                    responseString = String(responseBody, Charsets.UTF_8)
                    Log.d("DEBUG", "Response: $responseString")

                    if (responseString.isNotEmpty() && responseString != "0") {
                        val contactos = JSONArray(responseString)
                        val totalContactos = contactos.length()

                        Log.d("DEBUG", "Total contactos: $totalContactos")
                        var listaContactos = "LISTA DE CONTACTOS\n"
                        listaContactos += "═════════════════════\n\n"

                        for (i in 0 until contactos.length()) {
                            val contacto = contactos.getJSONObject(i)

                            listaContactos += "👤 CONTACTO ${i + 1}\n"
                            listaContactos += "─────────────────\n"
                            listaContactos += "Nombre: ${contacto.getString("Nombre")}\n"
                            listaContactos += "Apellidos: ${contacto.getString("Apellidos")}\n"

                            if (contacto.has("Telefono")) {
                                listaContactos += "Teléfono: ${contacto.optString("Telefono", "No registrado")}\n"
                            }

                            if (contacto.has("Email")) {
                                listaContactos += "Email: ${contacto.optString("Email", "No registrado")}\n"
                            }

                            listaContactos += "\n"
                        }

                        listaContactos += "═════════════════════\n"
                        listaContactos += "Total: $totalContactos contactos"

                        Log.d("DEBUG", "Texto generado length: ${listaContactos.length}")
                        Log.d("DEBUG", "Primeros 50 chars: ${listaContactos.take(50)}")

                        runOnUiThread {
                            Log.d("DEBUG", "Actualizando UI...")

                            // Forzar propiedades del TextView
                            txtDetalle.setBackgroundColor(android.graphics.Color.WHITE)
                            txtDetalle.setTextColor(android.graphics.Color.BLACK)
                            txtDetalle.textSize = 14f

                            // Asignar texto
                            txtDetalle.text = listaContactos
                            txtContador.text = "✅ Cargados: $totalContactos contactos"
                            txtContador.setTextColor(ContextCompat.getColor(this@ListadoActivity, android.R.color.holo_green_dark))

                            Log.d("DEBUG", "UI actualizada. Texto en txtDetalle: ${txtDetalle.text.take(50)}")

                            txtDetalle.invalidate()
                            txtDetalle.requestLayout()

                            Toast.makeText(this@ListadoActivity, "$totalContactos contactos cargados", Toast.LENGTH_SHORT).show()
                        }

                    } else {
                        Log.d("DEBUG", "Sin contactos")
                        runOnUiThread {
                            txtDetalle.text = "SIN CONTACTOS\n\nNo hay contactos registrados en la base de datos."
                            txtContador.text = "Total: 0 contactos"
                        }
                    }

                } catch (e: JSONException) {
                    Log.e("DEBUG", "Error JSON: ${e.message}")
                    runOnUiThread {
                        txtDetalle.text = "❌ ERROR JSON\n\nResponse: $responseString\n\nError: ${e.message}"
                        txtContador.text = "❌ Error en consulta"
                        Toast.makeText(this@ListadoActivity, "Error JSON: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Log.e("DEBUG", "Error general: ${e.message}")
                    runOnUiThread {
                        txtDetalle.text = "❌ ERROR GENERAL\n\nDetalles: ${e.message}"
                        txtContador.text = "❌ Error"
                        Toast.makeText(this@ListadoActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun onFailure(
                statusCode: Int,
                headers: Array<Header>?,
                responseBody: ByteArray?,
                error: Throwable
            ) {
                Log.e("DEBUG", "onFailure: $statusCode - ${error.message}")

                runOnUiThread {
                    txtDetalle.text = "🌐 ERROR DE CONEXIÓN\n\nCódigo: $statusCode\nError: ${error.message}"
                    txtContador.text = "🌐 Sin conexión"
                    Toast.makeText(this@ListadoActivity, "Error de conexión: $statusCode", Toast.LENGTH_LONG).show()
                }
            }
        })
    }
}