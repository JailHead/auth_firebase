package com.example.mysqlite

import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.widget.Toast

class ControladorBD (context: Context?, name: String?, factory: SQLiteDatabase.CursorFactory?, version: Int) : SQLiteOpenHelper(context, name, factory, version) {
    override fun onCreate(db: SQLiteDatabase) {
        //Instrucción DDL (Create)
        val sql = "create table empleado (numemp int primary key, nombre text, " + "apellidos text, sueldo real)"
        //Creaciónd de la BD
        try {
            db.execSQL(sql)
        } catch (e: SQLException) {
            Toast.makeText(
                null, "Error al crear la base de datos",
                Toast.LENGTH_SHORT
            ).show()
        }
    }//onCreate

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
}