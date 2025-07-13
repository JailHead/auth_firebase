package com.example.mysqlite

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ListadoActivity : AppCompatActivity() {
    //Instancias de componentes
    private lateinit var etListado: EditText
    private lateinit var btnregresar: Button
    //Instancia del controlador
    private lateinit var admin: ControladorBD

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listado)
        //Asociar la instancia con el componente
        etListado = findViewById(R.id.txtDetalle)
        btnregresar = findViewById(R.id.btnRegresar)
        /*
            Creación de la base de datos, de manera local, cuyo parametros son:
            contexto de la aplicación, nombre de la BD, versión
        */
        admin = ControladorBD(this, "empresapatito.db", null, 1)
        //Define el modo de acceso a la BD
        val bd = admin.readableDatabase
        //Instancia del apuntador al registro de busqueda
        val registro = bd.rawQuery("select * from empleado order by numemp", null)
        //Variable para la cantidad de registro obtenidos
        val n = registro.count
        //Variable para control de datos en el TextView
        var nr = 0

        if (n > 0) {
            //Mover el cursor al inicio de los registro obtenidos
            registro.moveToFirst()
            etListado.text.insert(0,"")
            //Ciclo repetitivo para colocar la información dentro del TextView
            do {
                etListado.text.insert(nr,"Numero: ${registro.getString(0)} "
                        +
                        "\nNombre: ${registro.getString(1)} " +
                        "\nApellidos: ${registro.getString(2)} " +
                        "\nSueldo: ${registro.getString(3)}\n")
                nr++
            } while (registro.moveToNext()) //Si existen más registros
        } else {
            //Mensaje informativo que no hay campos
            Toast.makeText(this, "Sin registro de empleados.", Toast.LENGTH_SHORT).show()
        }
        //Cerrando la BD
        bd.close()
        //Evento onClick
        btnregresar.setOnClickListener {
            //Objeto para conectar a otra Activity
            val intent = Intent(this, MainActivity::class.java)
            //Iniciar la Activity
            startActivity(intent)
        }
    }
}