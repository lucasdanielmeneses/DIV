package com.example.mch

import android.os.Bundle
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var repository: MchRepository

    private lateinit var etMarca: AutoCompleteTextView
    private lateinit var etModelo: AutoCompleteTextView
    private lateinit var btnBuscar: Button
    private lateinit var btnAcercaDe: Button
    private lateinit var cardResultados: CardView
    private lateinit var tvMotor: TextView
    private lateinit var tvChasis: TextView

    private lateinit var marcaAdapter: ContainsArrayAdapter
    private lateinit var modeloAdapter: ContainsArrayAdapter

    // Marca actualmente seleccionada (nombre -> código)
    private var marcasByName: Map<String, String> = emptyMap()
    private var selectedMarcaCodigo: String? = null

    private val uiScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repository = MchRepository(this)

        etMarca = findViewById(R.id.etMarca)
        etModelo = findViewById(R.id.etModelo)
        btnBuscar = findViewById(R.id.btnBuscar)
        btnAcercaDe = findViewById(R.id.btnAcercaDe)
        cardResultados = findViewById(R.id.cardResultados)
        tvMotor = findViewById(R.id.tvMotor)
        tvChasis = findViewById(R.id.tvChasis)

        marcaAdapter = ContainsArrayAdapter(this, android.R.layout.simple_list_item_1, emptyList())
        modeloAdapter = ContainsArrayAdapter(this, android.R.layout.simple_list_item_1, emptyList())

        etMarca.setAdapter(marcaAdapter)
        etModelo.setAdapter(modeloAdapter)

        etMarca.threshold = 1
        etModelo.threshold = 1

        loadMarcas()
        setupListeners()
    }

    private fun setupListeners() {
        // Cuando el usuario elige una marca de la lista desplegable
        etMarca.setOnItemClickListener { _, _, position, _ ->
            val nombreMarca = marcaAdapter.getItem(position) ?: return@setOnItemClickListener
            onMarcaSelected(nombreMarca)
        }

        // Si el usuario borra o edita el texto de marca a mano, invalidamos la selección
        // hasta que vuelva a elegir una opción válida de la lista.
        etMarca.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val texto = etMarca.text.toString()
                if (marcasByName.containsKey(texto)) {
                    onMarcaSelected(texto)
                } else if (texto.isNotBlank()) {
                    selectedMarcaCodigo = null
                    etModelo.setText("")
                    etModelo.isEnabled = false
                    hideResultados()
                }
            }
        }

        etModelo.setOnItemClickListener { _, _, _, _ ->
            hideResultados()
        }

        btnBuscar.setOnClickListener {
            realizarBusqueda()
        }

        btnAcercaDe.setOnClickListener {
            mostrarAcercaDe()
        }
    }

    private fun mostrarAcercaDe() {
        AlertDialog.Builder(this)
            .setTitle(R.string.about_title)
            .setMessage(R.string.about_body)
            .setPositiveButton(R.string.btn_cerrar, null)
            .show()
    }

    private fun onMarcaSelected(nombreMarca: String) {
        val codigo = marcasByName[nombreMarca] ?: return
        if (codigo == selectedMarcaCodigo) return

        selectedMarcaCodigo = codigo
        etModelo.setText("")
        etModelo.isEnabled = true
        hideResultados()
        loadModelos(codigo)
    }

    private fun loadMarcas() {
        uiScope.launch {
            val marcas = withContext(Dispatchers.IO) { repository.getAllMarcas() }
            marcasByName = marcas.associate { it.nombre to it.codigo }
            marcaAdapter.updateItems(marcas.map { it.nombre })
        }
    }

    private fun loadModelos(codigoMarca: String) {
        uiScope.launch {
            val modelos = withContext(Dispatchers.IO) { repository.getModelosForMarca(codigoMarca) }
            modeloAdapter.updateItems(modelos)
        }
    }

    private fun realizarBusqueda() {
        val nombreMarca = etMarca.text.toString().trim()
        val modelo = etModelo.text.toString().trim()

        val codigoMarca = marcasByName[nombreMarca]
        if (codigoMarca == null) {
            Toast.makeText(this, R.string.error_marca_invalida, Toast.LENGTH_SHORT).show()
            return
        }

        if (modelo.isBlank()) {
            Toast.makeText(this, R.string.error_modelo_invalido, Toast.LENGTH_SHORT).show()
            return
        }

        uiScope.launch {
            val localizacion = withContext(Dispatchers.IO) {
                repository.getLocalizacion(codigoMarca, modelo)
            }

            if (localizacion == null) {
                Toast.makeText(this@MainActivity, R.string.error_sin_datos, Toast.LENGTH_SHORT).show()
                hideResultados()
            } else {
                tvMotor.text = localizacion.motor
                tvChasis.text = localizacion.chasis
                cardResultados.visibility = View.VISIBLE
            }
        }
    }

    private fun hideResultados() {
        cardResultados.visibility = View.GONE
    }
}
