package com.example.currency_converter

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.os.AsyncTask
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var convert: Button
    private lateinit var convertFrom: Spinner
    private lateinit var convertTo: Spinner
    private lateinit var input: EditText
    private lateinit var output: EditText
    private lateinit var tvIsConnected: TextView
    private lateinit var tvResult: TextView
    private lateinit var adapter: ArrayAdapter<CharSequence>
    private var rate: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        convert = findViewById(R.id.convertButton)
        convertFrom = findViewById(R.id.convertFromSpinner)
        convertTo = findViewById(R.id.convertToSpinner)
        input = findViewById(R.id.input)
        output = findViewById(R.id.result)
        tvIsConnected = findViewById(R.id.tvIsConnected)
        tvResult = findViewById(R.id.tvResult)

        adapter = ArrayAdapter.createFromResource(this, R.array.money_values, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        convertFrom.adapter = adapter
        convertTo.adapter = adapter

        convert.setOnClickListener {
            if (checkNetworkConnection()) {
                val url = "https://api.exchangerate.host/convert?from=${convertFrom.selectedItem}&to=${convertTo.selectedItem}&format=xml"
                HTTPAsyncTask().execute(url)
            }
        }
    }

    private fun checkNetworkConnection(): Boolean {
        val connMgr = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo: NetworkInfo? = connMgr.activeNetworkInfo
        val isConnected = networkInfo?.isConnected == true

        if (networkInfo != null) {
            tvIsConnected.text = if (isConnected) {
                tvIsConnected.setBackgroundColor(0xFF7CCC26.toInt())
                "Connected ${networkInfo.typeName}"
            } else {
                tvIsConnected.setBackgroundColor(0xFFFF0000.toInt())
                "Not Connected"
            }
        }
        return isConnected
    }

    private inner class HTTPAsyncTask : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg urls: String): String {
            return try {
                httpGet(urls[0])
            } catch (e: IOException) {
                "Unable to retrieve web page. URL may be invalid."
            }
        }

        override fun onPostExecute(result: String) {
            try {
                xmlParser(result)
                val currency = input.text.toString().toFloat()
                val finalResult = currency * rate
                output.setText(finalResult.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun httpGet(myURL: String): String {
        val url = URL(myURL)
        val conn = url.openConnection() as HttpURLConnection
        conn.connect()
        return conn.inputStream.use { convertInputStreamToString(it) }
    }

    private fun convertInputStreamToString(inputStream: InputStream): String {
        return BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
    }

    private fun xmlParser(result: String) {
        val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }
        val xpp = factory.newPullParser().apply { setInput(StringReader(result)) }
        var eventType = xpp.eventType
        var tag = ""
        var rateResult = ""

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> tag = xpp.name
                XmlPullParser.TEXT -> if (tag == "rate") {
                    rateResult = xpp.text
                    tag = ""
                }
            }
            eventType = xpp.next()
        }

        rate = rateResult.toFloat()
        tvResult.text = "Rate: ${1 / rate}"
        tvResult.setBackgroundColor(0xFF7CCC26.toInt())
    }
}