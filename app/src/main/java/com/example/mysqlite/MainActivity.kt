package com.example.mysqlite

import android.content.ContentValues
import android.content.Intent
import android.database.SQLException
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    //Objetos de componentes gráficos
    private lateinit var etNumEmp: EditText
    private lateinit var etNombre: EditText
    private lateinit var etApellidos: EditText
    private lateinit var etSueldo: EditText
    private lateinit var btnAgregar: ImageButton
    private lateinit var btnBuscar: ImageButton
    private lateinit var btnActualizar: ImageButton
    private lateinit var btnEliminar: ImageButton
    private lateinit var btnLista: Button
    //Objeto para gestión de la BD
    private lateinit var admin: ControladorBD

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Asociar los objetos con componentes gráficos
        etNumEmp = findViewById(R.id.txtNumEmp)
        etNombre = findViewById(R.id.txtNombre)
        etApellidos = findViewById(R.id.txtApellidos)
        etSueldo = findViewById(R.id.txtSueldo)
        btnAgregar = findViewById(R.id.btnAgregar)
        btnBuscar = findViewById(R.id.btnBuscar)
        btnActualizar = findViewById(R.id.btnActualizar)
        btnEliminar = findViewById(R.id.btnEliminar)
        btnLista = findViewById(R.id.btnLista)

        /*
            Creación de objeto de clase ControladorBD para crear la base de datos
            Sus parámetros son: contexto, nombre de la Base de Datos, null y versión de la BD
            La ruta de creación de la base de datos en el dispositivo es:
            android/data/<paquete>/databases/<nombre_bd>
        */
        admin = ControladorBD(this, "empresapatito.db", null, 1)
        //Eventos onClick
        btnAgregar.setOnClickListener { registrarEmpleado() }
        btnBuscar.setOnClickListener { buscarEmpleado() }
        btnActualizar.setOnClickListener { actualizarEmpleado() }
        btnEliminar.setOnClickListener { eliminarEmpleado() }
        btnLista.setOnClickListener { listarRegistros() }
    }

    private fun listarRegistros() {
        val intent = Intent(this, ListadoActivity::class.java)
        //Iniciar la Activity
        startActivity(intent)
    }

    private fun eliminarEmpleado() {
        val bd = admin.writableDatabase
        //Variable para busqueda de dato para eliminar
        val nump = etNumEmp.text.toString()
        //Validar que exista el campo no este vacío
        if (nump.isNotEmpty()) {
            val cantidad = bd.delete("empleado", "numemp = $nump", null)
            bd.close()

            etNumEmp.setText("")
            etNombre.setText("")
            etApellidos.setText("")
            etSueldo.setText("")
            etNumEmp.requestFocus()

            if (cantidad > 0) Toast.makeText(this, "Empleado eliminado.",
                Toast.LENGTH_SHORT)
                .show() else Toast.makeText(
                this,
                "El número de empleado no existe.",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(this, "Ingresa número de empleado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun actualizarEmpleado() {
        val bd = admin.writableDatabase

        //Variables para obtener los valores de componentes gráficos
        val nump = etNumEmp.text.toString()
        val nomp = etNombre.text.toString()
        val apep = etApellidos.text.toString()
        val suep = etSueldo.text.toString()

        //Validar que exista información registrada
        if (nump.isNotEmpty() && nomp.isNotEmpty() && apep.isNotEmpty() && suep.isNotEmpty()) {
            //Objeto que almacena los valores para enviar a la tabla
            val registro = ContentValues()

            registro.put("numemp", nump)
            registro.put("nombre", nomp)
            registro.put("apellidos", apep)
            registro.put("sueldo", suep)

            val cantidad = bd.update("empleado", registro, "numemp=$nump", null)
            bd.close()

            etNumEmp.setText("")
            etNombre.setText("")
            etApellidos.setText("")
            etSueldo.setText("")
            etNumEmp.requestFocus()
            //Validar si existieron registros a borrar
            if (cantidad > 0) Toast.makeText(
                this,
                "Datos del empleado actualizados.",
                Toast.LENGTH_SHORT
            ).show() else Toast.makeText(
                this,
                "El número de empleado no existe.",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(this, "Debes registrar primero los datos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buscarEmpleado() {
        val bd = admin.readableDatabase

        //Variable para busqueda de dato para obtener información
        val nump = etNumEmp.text.toString()
        //Validar que el campo no este vacio
        if (nump.isNotEmpty()) {
            //Objeto apunta al registro donde localice el dato, se le envia la instrucción sql de busqueda
            val fila = bd.rawQuery(
                "select nombre, apellidos, sueldo from empleado " +
                        "where numemp=" + nump, null
            )
            //Valida
            if (fila.moveToFirst()) {
                //Se coloca en los componentes los valores encontrados
                etNombre.setText(fila.getString(0))
                etApellidos.setText(fila.getString(1))
                etSueldo.setText(fila.getString(2))
                //Se cierra la base de datos
                bd.close()
            } else {
                Toast.makeText(this, "Número de empleado no existe.", Toast.LENGTH_SHORT).show()
                etNumEmp.setText("")
                etNumEmp.requestFocus()
                bd.close()
            } //else-if fila
        } else {
            Toast.makeText(this, "Ingresa número de empleado", Toast.LENGTH_SHORT).show()
            etNumEmp.requestFocus()
        } //else
    }

    private fun registrarEmpleado() {
        //Establecer el modo de apertura de la base de datos en modo Escritura
        val bd = admin.writableDatabase
        //Variables para obtener los valores de componentes gráficos
        val nump = etNumEmp.text.toString()
        val nomp = etNombre.text.toString()
        val apep = etApellidos.text.toString()
        val suep = etSueldo.text.toString()

        //Validar que exista información registrada
        if (nump.isNotEmpty() && nomp.isNotEmpty() && apep.isNotEmpty() &&
            suep.isNotEmpty()) {
            //Objeto que almacena los valores para enviar a la tabla
            val registro = ContentValues()
            //Referencias a los datos que pasar a la BD
            //indicando como parámetos de put el nombre del campo y el valor a insertar
            //El segundo proviene de los campos de texto
            registro.put("numemp", nump)
            registro.put("nombre", nomp)
            registro.put("apellidos", apep)
            registro.put("sueldo", suep)
            if (bd != null) {
                //Almacenar los valores en la tabla
                try {
                    val x: Long = bd.insert("empleado", null, registro)
                } catch (e: SQLException) {
                    Log.e("Exception", "Error: " + e.message.toString())
                }
                //Cerrar la base de datos
                bd.close()
            }
            //Limpiar los campos de texto
            etNumEmp.setText("")
            etNombre.setText("")
            etApellidos.setText("")
            etSueldo.setText("")
            etNumEmp.requestFocus()
            //Confirmar la operación realizada
            Toast.makeText(this, "Empleado registrado.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Debes registrar primero los datos", Toast.LENGTH_SHORT).show()
        } //else
    }
}