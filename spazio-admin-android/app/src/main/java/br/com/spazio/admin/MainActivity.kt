package br.com.spazio.admin

import android.app.*
import android.os.*
import android.graphics.Color
import android.graphics.Typeface
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.*
import android.text.InputType
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val base = "https://uqsgrsggvlwjceawmlbv.supabase.co"
    private val apiKey = "sb_publishable_9Ugvf23EysBC3sQ27s40cQ_YppnjdkP"
    private val prefs by lazy { getSharedPreferences("spazio_admin", MODE_PRIVATE) }
    private val io = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private var access = ""
    private var refresh = ""
    private var userId = ""
    private var activeTab = "orders"
    private var knownOrders: Set<String>? = null
    private var lastError = ""
    private lateinit var root: LinearLayout
    private lateinit var body: LinearLayout
    private lateinit var message: TextView
    private val gold = Color.rgb(217,174,117)
    private val ink = Color.rgb(25,18,14)
    private val paper = Color.rgb(250,247,242)
    private val red = Color.rgb(190,48,40)
    private val money = NumberFormat.getCurrencyInstance(Locale("pt","BR"))
    private val poll = object : Runnable {
        override fun run() {
            if (access.isNotBlank() && activeTab == "orders") loadOrders()
            handler.postDelayed(this, 15000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = ink
        window.navigationBarColor = ink
        access = prefs.getString("access","") ?: ""
        refresh = prefs.getString("refresh","") ?: ""
        createChannel()
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 20)
        if (access.isNotBlank()) verifySession() else showLogin()
    }

    private fun dp(v:Int)= (v*resources.displayMetrics.density).toInt()
    private fun text(s:String,size:Float=14f,bold:Boolean=false,color:Int=ink):TextView = TextView(this).apply {
        text=s; textSize=size; setTextColor(color); if(bold) setTypeface(null,Typeface.BOLD)
        setPadding(dp(2),dp(5),dp(2),dp(5))
    }
    private fun button(s:String, action:()->Unit, primary:Boolean=true):Button = Button(this).apply {
        text=s; isAllCaps=false; setTextColor(if(primary) Color.WHITE else ink)
        setBackgroundTintList(android.content.res.ColorStateList.valueOf(if(primary) red else Color.rgb(232,222,210)))
        setOnClickListener { action() }
    }
    private fun field(hint:String,value:String="",type:Int=InputType.TYPE_CLASS_TEXT):EditText = EditText(this).apply {
        this.hint=hint; setText(value); inputType=type; textSize=15f
        setPadding(dp(12),dp(7),dp(12),dp(7))
        setBackgroundTintList(android.content.res.ColorStateList.valueOf(gold))
    }
    private fun column():LinearLayout=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
    private fun row():LinearLayout=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
    private fun card(parent:LinearLayout):LinearLayout {
        val c=column()
        c.setPadding(dp(15),dp(12),dp(15),dp(12))
        c.setBackgroundColor(Color.WHITE)
        val p=LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(7),0,dp(7));parent.addView(c,p)
        return c
    }
    private fun shell(title:String) {
        root=column().apply{setBackgroundColor(paper)}
        val top=row().apply {
            setPadding(dp(18),dp(12),dp(12),dp(12));setBackgroundColor(ink)
            addView(text("SPAZIO",22f,true,gold),LinearLayout.LayoutParams(0,-2,1f))
            addView(button("Sair",{logout()},false))
        }
        root.addView(top)
        root.addView(text(title,19f,true,ink).apply{setPadding(dp(18),dp(15),dp(18),dp(5))})
        message=text("",13f,false,red).apply{setPadding(dp(18),dp(3),dp(18),dp(5))}
        root.addView(message)
        val scroll=ScrollView(this)
        body=column().apply{setPadding(dp(14),0,dp(14),dp(18))}
        scroll.addView(body);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root)
    }
    private fun showLogin() {
        shell("Painel administrativo")
        body.addView(text("Entre com a conta autorizada da Spazio.",14f,false))
        val email=field("E-mail", "",InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        val pass=field("Senha","",InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        body.addView(email);body.addView(pass)
        body.addView(button("Entrar no painel",{
            val e=email.text.toString().trim();val p=pass.text.toString()
            if(e.isBlank()||p.isBlank()){notice("Informe e-mail e senha.");return@button}
            runIo({ auth(e,p) }) { verifySession() }
        }))
        body.addView(text("Acesso protegido pelo Supabase. Somente usuários cadastrados como administradores podem entrar.",12f,false,Color.DKGRAY))
    }
    private fun verifySession() {
        runIo({
            val u=request("GET","/auth/v1/user")
            userId=u.getString("id")
            // O PostgREST devolve listas como JSON array, mesmo quando há apenas um registro.
            val admins=requestArray("GET","/rest/v1/admin_users?select=user_id&user_id=eq."+enc(userId))
            if(admins.length()==0) throw Exception("Esta conta não está autorizada como administradora.")
            true
        }) { showMain() }
    }
    private fun auth(email:String,password:String) {
        val data=JSONObject().put("email",email).put("password",password)
        val t=request("POST","/auth/v1/token?grant_type=password",data)
        access=t.getString("access_token");refresh=t.optString("refresh_token")
        prefs.edit().putString("access",access).putString("refresh",refresh).apply()
    }
    private fun logout() {
        runIo({ try{request("POST","/auth/v1/logout")}catch(_:Exception){};true }) {
            access="";refresh="";userId="";knownOrders=null
            prefs.edit().clear().apply();handler.removeCallbacks(poll);showLogin()
        }
    }
    private fun showMain() {
        shell("Controle da pizzaria")
        val nav=row()
        listOf("orders" to "Pedidos","products" to "Pizzas","borders" to "Bordas","settings" to "Loja").forEach { (key,label)->
            val b=button(label,{activeTab=key;renderTab()},activeTab==key)
            nav.addView(b,LinearLayout.LayoutParams(0,-2,1f))
        }
        body.addView(nav)
        renderTab()
        handler.removeCallbacks(poll);handler.postDelayed(poll,15000)
    }
    private fun renderTab() {
        body.removeViews(1,body.childCount-1)
        when(activeTab){
            "orders" -> { body.addView(button("Atualizar pedidos agora",{loadOrders()},false));loadOrders() }
            "products" -> { body.addView(button("+ Novo produto",{productDialog(null)}));loadProducts() }
            "borders" -> { body.addView(button("+ Nova borda recheada",{borderDialog(null)}));loadBorders() }
            "settings" -> { body.addView(button("+ Nova configuração",{settingDialog(null,null)}));loadSettings() }
        }
    }
    private fun loadOrders() {
        runIo({
            val arr=requestArray("GET","/rest/v1/orders?select=*&order=created_at.desc&limit=150")
            val ids=(0 until arr.length()).map{arr.getJSONObject(it).getString("id")}.toSet()
            val old=knownOrders
            val fresh=if(old==null) emptySet() else ids-old
            knownOrders=ids
            Pair(arr,fresh.size)
        }) { result ->
            if(result.second>0){notice(if(result.second==1)"Novo pedido recebido!" else "Novos pedidos recebidos!");notifyNewOrders(result.second)}
            body.removeViews(1,body.childCount-1)
            body.addView(button("↻ Atualizar pedidos",{loadOrders()},false))
            if(result.first.length()==0) body.addView(text("Nenhum pedido registrado até agora."))
            for(i in 0 until result.first.length()) orderCard(result.first.getJSONObject(i))
        }
    }
    private fun orderCard(o:JSONObject) {
        val c=card(body);val id=o.optString("id");val status=o.optString("status")
        c.addView(text("#"+id.take(8).uppercase(Locale.ROOT)+" · "+o.optString("customer_name")+" · "+brl(o.optDouble("total")),17f,true))
        c.addView(text(date(o.optString("created_at"))+" · "+if(o.optString("fulfillment")=="delivery")"Entrega" else "Retirada"+" · "+o.optString("payment"),12f,false,Color.DKGRAY))
        c.addView(text("CLIENTE",12f,true,red))
        c.addView(text("Nome: "+o.optString("customer_name")))
        if(o.optString("fulfillment")=="delivery"){
            c.addView(text("Endereço: "+listOf(o.optString("street"),o.optString("street_number"),o.optString("district"),o.optString("reference")).filter{it.isNotBlank()}.joinToString(", ")))
        } else c.addView(text("Retirada na loja"))
        c.addView(text("Pagamento: "+o.optString("payment")+(if(o.optString("change_for").isNotBlank())" · Troco para "+o.optString("change_for") else "")))
        val items=o.optJSONArray("items")?:JSONArray()
        for(i in 0 until items.length()){
            val item=items.getJSONObject(i)
            c.addView(text(item.optInt("qty").toString()+"x "+item.optString("name")+" · "+brl(item.optDouble("unit_price")*item.optInt("qty"))+
                listOf(item.optString("crust"),item.optString("cream"),item.optString("notes")).filter{it.isNotBlank()}.joinToString(" · ").let{if(it.isBlank())"" else " · "+it},13f))
        }
        c.addView(text("Subtotal: "+brl(o.optDouble("subtotal"))+" · Entrega: "+brl(o.optDouble("delivery_fee"))+" · Total: "+brl(o.optDouble("total")),13f,true))
        val statuses=listOf("received","preparing","delivering","completed","cancelled")
        val labels=listOf("Recebido","Em preparo","Saiu para entrega","Concluído","Cancelado")
        val sp=Spinner(this);sp.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,labels)
        sp.setSelection(statuses.indexOf(status).coerceAtLeast(0));c.addView(sp)
        c.addView(button("Salvar andamento",{val s=statuses[sp.selectedItemPosition];updateOrder(id,s)}))
        if(status=="received"){
            val actions=row()
            actions.addView(button("✓ Aceitar",{updateOrder(id,"preparing")}))
            actions.addView(button("✕ Rejeitar",{AlertDialog.Builder(this).setMessage("Rejeitar este pedido?").setNegativeButton("Voltar",null).setPositiveButton("Rejeitar"){_,_->updateOrder(id,"cancelled")}.show()},false))
            c.addView(actions)
        }
    }
    private fun updateOrder(id:String,status:String) {
        runIo({request("PATCH","/rest/v1/orders?id=eq."+enc(id),JSONObject().put("status",status),true);true}){notice("Andamento atualizado.");loadOrders()}
    }
    private fun loadProducts() {
        runIo({requestArray("GET","/rest/v1/products?select=*&order=sort_order.asc,id.asc")}) { a ->
            body.removeViews(1,body.childCount-1);body.addView(button("+ Novo produto",{productDialog(null)}))
            for(i in 0 until a.length()){val p=a.getJSONObject(i);itemCard(p.optString("name"),p.optString("category")+" · "+brl(p.optDouble("price"))+if(p.optBoolean("active"))"" else " · INATIVO"){productDialog(p)}}
        }
    }
    private fun productDialog(p:JSONObject?) {
        val box=column();val name=field("Nome",p?.optString("name")?:"");val desc=field("Descrição",p?.optString("description")?:"")
        val price=field("Preço (ex.: 49,90)",p?.optDouble("price")?.toString()?:"",InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
        val order=field("Ordem no cardápio",p?.optInt("sort_order",0)?.toString()?:"0",InputType.TYPE_CLASS_NUMBER)
        val cat=Spinner(this);cat.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,listOf("Tradicionais","Especiais","Bebidas"));cat.setSelection(listOf("Tradicionais","Especiais","Bebidas").indexOf(p?.optString("category")).coerceAtLeast(0))
        val active=CheckBox(this).apply{text="Produto ativo";isChecked=p?.optBoolean("active",true)?:true}
        box.addView(name);box.addView(desc);box.addView(text("Categoria",12f,true));box.addView(cat);box.addView(price);box.addView(order);box.addView(active)
        AlertDialog.Builder(this).setTitle(if(p==null)"Novo produto" else "Editar produto").setView(box)
            .setNegativeButton("Cancelar",null).setPositiveButton("Salvar"){_,_->
                val n=name.text.toString().trim();val pr=price.text.toString().replace(",",".").toDoubleOrNull()
                if(n.isBlank()||pr==null||pr<0){notice("Confira nome e preço.");return@setPositiveButton}
                val data=JSONObject().put("name",n).put("description",desc.text.toString()).put("category",cat.selectedItem.toString()).put("price",pr).put("sort_order",order.text.toString().toIntOrNull()?:0).put("active",active.isChecked)
                runIo({if(p==null)request("POST","/rest/v1/products",data,true) else request("PATCH","/rest/v1/products?id=eq."+p.optLong("id"),data,true);true}){notice("Produto salvo.");loadProducts()}
            }.show()
    }
    private fun loadBorders() {
        runIo({requestArray("GET","/rest/v1/borders?select=*&order=id.asc")}) { a ->
            body.removeViews(1,body.childCount-1);body.addView(button("+ Nova borda recheada",{borderDialog(null)}))
            for(i in 0 until a.length()){val b=a.getJSONObject(i);itemCard(b.optString("name"),"Borda recheada · "+brl(b.optDouble("price"))+if(b.optBoolean("active"))"" else " · INATIVA"){borderDialog(b)}}
        }
    }
    private fun borderDialog(b:JSONObject?) {
        val box=column();val name=field("Nome da borda",b?.optString("name")?:"")
        val price=field("Preço (ex.: 8,00)",b?.optDouble("price")?.toString()?:"",InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
        val active=CheckBox(this).apply{text="Borda ativa";isChecked=b?.optBoolean("active",true)?:true}
        box.addView(name);box.addView(price);box.addView(active)
        AlertDialog.Builder(this).setTitle(if(b==null)"Nova borda" else "Editar borda").setView(box)
            .setNegativeButton("Cancelar",null).setPositiveButton("Salvar"){_,_->
                val n=name.text.toString().trim();val pr=price.text.toString().replace(",",".").toDoubleOrNull()
                if(n.isBlank()||pr==null||pr<0){notice("Confira nome e preço.");return@setPositiveButton}
                val data=JSONObject().put("name",n).put("price",pr).put("active",active.isChecked)
                runIo({if(b==null)request("POST","/rest/v1/borders",data,true) else request("PATCH","/rest/v1/borders?id=eq."+b.optLong("id"),data,true);true}){notice("Borda salva.");loadBorders()}
            }.show()
    }
    private fun loadSettings() {
        runIo({requestArray("GET","/rest/v1/store_settings?select=*&order=key.asc")}) { a ->
            body.removeViews(1,body.childCount-1);body.addView(button("+ Nova configuração",{settingDialog(null,null)}))
            for(i in 0 until a.length()){val s=a.getJSONObject(i);itemCard(s.optString("key"),s.opt("value").toString()){settingDialog(s.optString("key"),s.opt("value").toString())}}
        }
    }
    private fun settingDialog(key:String?,value:String?) {
        val box=column();val k=field("Chave (ex.: delivery_fee)",key?:"");val v=field("Valor em JSON",value?:"")
        box.addView(k);box.addView(v)
        AlertDialog.Builder(this).setTitle("Configuração da loja").setView(box)
            .setNegativeButton("Cancelar",null).setPositiveButton("Salvar"){_,_->
                try{
                    val parsed=org.json.JSONTokener(v.text.toString()).nextValue()
                    val data=JSONObject().put("key",k.text.toString().trim()).put("value",parsed)
                    if(k.text.toString().trim().matches(Regex("[a-zA-Z][a-zA-Z0-9_]*")))
                        runIo({request("POST","/rest/v1/store_settings?on_conflict=key",data,true,"resolution=merge-duplicates,return=representation");true}){notice("Configuração salva.");loadSettings()}
                    else notice("Use uma chave com letras, números e sublinhados.")
                }catch(_:Exception){notice("O valor precisa ser um JSON válido (ex.: 12, true ou \"texto\").")}
            }.show()
    }
    private fun itemCard(title:String,subtitle:String,edit:()->Unit) {
        val c=card(body);c.addView(text(title,16f,true));c.addView(text(subtitle,12f,false,Color.DKGRAY));c.addView(button("Editar",edit,false))
    }
    private fun <T> runIo(work:()->T,done:(T)->Unit) {
        message.text="Conectando…"
        io.execute { try{val result=work();runOnUiThread{if(!isFinishing)done(result)}}catch(e:Exception){runOnUiThread{if(!isFinishing)notice(e.message?:"Falha ao acessar o servidor.")}} }
    }
    private fun notice(s:String){message.text=s}
    private fun brl(n:Double)=money.format(n)
    private fun enc(s:String)=URLEncoder.encode(s,"UTF-8")
    private fun date(s:String):String=try{SimpleDateFormat("dd/MM/yyyy 'às' HH:mm",Locale("pt","BR")).format(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",Locale.US).parse(s.substringBefore('.').removeSuffix("Z"))!!)}catch(_:Exception){s}
    private fun requestArray(method:String,path:String):JSONArray=JSONArray(requestRaw(method,path,null,false,null))
    private fun request(method:String,path:String,data:JSONObject?=null,prefer:Boolean=false,preferValue:String?=null):JSONObject {
        val raw=requestRaw(method,path,data,prefer,preferValue)
        return if(raw.isBlank()) JSONObject() else JSONObject(raw)
    }
    private fun requestRaw(method:String,path:String,data:JSONObject?,prefer:Boolean,preferValue:String?):String {
        fun call(token:String):Pair<Int,String>{
            val c=URL(base+path).openConnection() as HttpURLConnection
            c.requestMethod=method;c.connectTimeout=15000;c.readTimeout=20000
            c.setRequestProperty("apikey",apiKey);c.setRequestProperty("Authorization","Bearer "+if(token.isBlank())apiKey else token)
            c.setRequestProperty("Accept","application/json")
            if(data!=null){c.doOutput=true;c.setRequestProperty("Content-Type","application/json");c.outputStream.use{it.write(data.toString().toByteArray(Charsets.UTF_8))}}
            if(prefer){c.setRequestProperty("Prefer",preferValue?:"return=representation")}
            val code=c.responseCode;val stream=if(code<400)c.inputStream else c.errorStream
            val body=stream?.bufferedReader()?.use{it.readText()}?:"";c.disconnect();return code to body
        }
        var (code,body)=call(access)
        if(code==401 && refresh.isNotBlank() && !path.contains("/auth/v1/token")){
            val t=URL(base+"/auth/v1/token?grant_type=refresh_token").openConnection() as HttpURLConnection
            t.requestMethod="POST";t.doOutput=true;t.setRequestProperty("apikey",apiKey);t.setRequestProperty("Content-Type","application/json")
            t.outputStream.use{it.write(JSONObject().put("refresh_token",refresh).toString().toByteArray())}
            val tc=t.responseCode;val tb=(if(tc<400)t.inputStream else t.errorStream).bufferedReader().use{it.readText()}
            if(tc<400){val j=JSONObject(tb);access=j.getString("access_token");refresh=j.optString("refresh_token",refresh);prefs.edit().putString("access",access).putString("refresh",refresh).apply();val retry=call(access);code=retry.first;body=retry.second}
        }
        if(code !in 200..299)throw Exception(try{JSONObject(body).optString("message",JSONObject(body).optString("msg",body))}catch(_:Exception){body.ifBlank{"HTTP "+code}})
        return body
    }
    private fun createChannel(){
        if(Build.VERSION.SDK_INT>=26){val nm=getSystemService(NotificationManager::class.java);nm.createNotificationChannel(NotificationChannel("spazio_orders","Novos pedidos",NotificationManager.IMPORTANCE_HIGH).apply{description="Alertas de novos pedidos da Spazio"})}
    }
    private fun notifyNewOrders(count:Int){
        try{
            val nm=getSystemService(NotificationManager::class.java)
            val n=if(Build.VERSION.SDK_INT>=26)Notification.Builder(this,"spazio_orders") else Notification.Builder(this)
            n.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Spazio · novo pedido").setContentText(if(count==1)"1 novo pedido recebido" else "$count novos pedidos recebidos").setAutoCancel(true)
            nm.notify(1001,n.build())
        }catch(_:Exception){}
    }
    override fun onResume(){super.onResume();if(access.isNotBlank()){handler.removeCallbacks(poll);handler.postDelayed(poll,15000)}}
    override fun onPause(){super.onPause();handler.removeCallbacks(poll)}
    override fun onDestroy(){super.onDestroy();handler.removeCallbacks(poll);io.shutdown()}
}
