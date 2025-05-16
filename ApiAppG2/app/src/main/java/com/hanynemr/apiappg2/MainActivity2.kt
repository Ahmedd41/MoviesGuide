package com.hanynemr.apiappg2

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import org.json.JSONArray

class MainActivity2 : AppCompatActivity(){
    lateinit var mealsLV: ListView
    lateinit var bar: ProgressBar
    lateinit var queue: RequestQueue
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main2)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        mealsLV=findViewById(R.id.mealsLV)
        bar=findViewById(R.id.bar)
        queue=Volley.newRequestQueue(this)

        val url="https://560057.youcanlearnit.net/services/json/itemsfeed.php"
//        val request= JsonArrayRequest(url,this,this)
        bar.visibility=View.VISIBLE
        val request= JsonArrayRequest(url,{
            val gson= Gson()
            val meals=gson.fromJson(it.toString(), Array<Meal>::class.java)
            val names=meals.map { it.itemName }
            val adapter= ArrayAdapter(this,android.R.layout.simple_list_item_1,names)
            mealsLV.adapter=adapter
            bar.visibility=View.INVISIBLE

        },{
            Toast.makeText(this, "error", Toast.LENGTH_SHORT).show()
            bar.visibility=View.INVISIBLE
        })

        queue.add(request)
    }

    override fun onDestroy() {
        queue.stop()
        super.onDestroy()
    }

}