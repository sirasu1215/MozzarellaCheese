package net.sirasu.mozzarellacheese

import android.Manifest
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import pub.devrel.easypermissions.EasyPermissions
import uk.me.berndporr.iirj.Butterworth
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

class MainActivity : AppCompatActivity(), SensorEventListener, View.OnClickListener {
    var soundFile_n = 41 //サウンドファイルの数　ループ変更用// 　
    //var NumCountInstance: com.example.nabeatsurally.SoundLoad? = null
    private var textView: TextView? = null
    private var Hithantei: TextView? = null
    var count = 0
    var sensorx = 0.0
    var sensory = 0.0
    var sensorz = 0.0
    private var sensorManager: SensorManager? = null
    private var accel: Sensor? = null

    //時間関係
    var starttime = LocalDateTime.now()
    var startendtime = LocalDateTime.now()
    var timeflag = true
    var swing_hantei = false
    var hit_hantei = false
    var bl_hit_updown = true
    var hit_count = 0
    var hit_keep_thirty = 0
    var hitout = 0
    var bl_swing_updown = true
    var min_acc = 100.0
    var max_acc = 0.0
    var swing_count = 0
    var bl_onhit = false
    var bl_onswing = false
    var swing_and_hit = 0
    var swing_only = 0
    var hit_only = 0
    var hit_flag = false


    //NearByShare用の変数
    var SERVICE_ID = "atuo.nearby"
    var nickname = "atuo"
    var mRemoteEndpointId:String? = ""
    val activity: MainActivity = this
    val TAG = "myapp"
    var rally_flag = 0

    //音量を保管する
    var before_volume = 0;
    var volume = 0;




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //画面遷移の処理
        val returnButton = findViewById<Button>(R.id.return_button)
        returnButton.setOnClickListener { v: View? -> finish() }

        //ここより下はセンサー系の処理
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        // Get an instance of the SensorManager
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        // Get an instance of the TextView
        textView = findViewById(R.id.text_view)
        Hithantei = findViewById(R.id.textView3)
        accel = sensorManager!!.getDefaultSensor(
            Sensor.TYPE_LINEAR_ACCELERATION
        )
        val buttonStart = findViewById<Button>(R.id.button_start)
        buttonStart.setOnClickListener(this)
        val buttonStop = findViewById<Button>(R.id.button_stop)
        buttonStop.setOnClickListener(this)
        val arr_n = ArrayList<Int?>()
        for (num in 1..soundFile_n) {
            arr_n.add(resources.getIdentifier("n$num", "raw", packageName))
        }
        //NumCountInstance = com.example.nabeatsurally.SoundLoad(applicationContext, arr_n)




        //NearByShare(通信機能)
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE
        )

        //許可したいpermissionを許可できるように
        if (!EasyPermissions.hasPermissions(this, *permissions)) {
            EasyPermissions.requestPermissions(this, "パーミッションに関する説明", 1, *permissions)
        }




        findViewById<Button>(R.id.advertise).setOnClickListener{
            Log.d(TAG,"advertiseをタップ")
            Nearby.getConnectionsClient(this)
                .startAdvertising(
                    nickname,
                    SERVICE_ID,
                    mConnectionLifecycleCallback,
                    AdvertisingOptions(Strategy.P2P_STAR)
                )
                .addOnSuccessListener {
                    // Advertise開始した
                    Log.d(TAG,"Advertise開始した")
                }
                .addOnFailureListener {
                    // Advertiseできなかった
                    Log.d(TAG,"Advertiseできなかった")

                }

        }




        findViewById<Button>(R.id.discovery).setOnClickListener{
            Log.d(TAG,"Discoveryをタップ")

            Nearby.getConnectionsClient(this)
                .startDiscovery(
                    SERVICE_ID,
                    mEndpointDiscoveryCallback,
                    DiscoveryOptions(Strategy.P2P_STAR)
                )
                .addOnSuccessListener {
                    // Discovery開始した
                    Log.d(TAG,"Discovery開始した")
                }
                .addOnFailureListener {
                    // Discovery開始できなかった
                    Log.d(TAG,"Discovery開始できなかった")
                }


        }


    }

    private val mEndpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, discoveredEndpointInfo: DiscoveredEndpointInfo) {
            // Advertise側を発見した
            Log.d(TAG,"Advertise側を発見した")

            // とりあえず問答無用でコネクション要求してみる
            Nearby.getConnectionsClient(activity)
                .requestConnection(nickname, endpointId, mConnectionLifecycleCallback)
        }

        override fun onEndpointLost(endpointId: String) {
            // 見つけたエンドポイントを見失った
            Log.d(TAG,"見つけたエンドポイントを見失った")
        }
    }

    private val mConnectionLifecycleCallback = object : ConnectionLifecycleCallback() {

        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            // 他の端末からコネクションのリクエストを受け取った時
            Log.d(TAG,"他の端末からコネクションのリクエストを受け取った")

            // とりあえず来る者は拒まず即承認
            Nearby.getConnectionsClient(activity)
                .acceptConnection(endpointId, mPayloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {

            // コネクションリクエストの結果を受け取った時
            Log.d(TAG,"コネクションリクエストの結果を受け取った時")

            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    // コネクションが確立した。今後通信が可能。
                    Log.d(TAG,"コネクションが確立した。今後通信が可能。")
                    // 通信時にはendpointIdが必要になるので、フィールドに保持する。
                    mRemoteEndpointId = endpointId

                    findViewById<TextView>(R.id.text_date).text = "通信成功"
                }

                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    // コネクションが拒否された時。通信はできない。
                    Log.d(TAG,"コネクションが拒否された時。通信はできない。")
                    mRemoteEndpointId = null
                }

                ConnectionsStatusCodes.STATUS_ERROR -> {
                    // エラーでコネクションが確立できない時。通信はできない。
                    Log.d(TAG,"エラーでコネクションが確立できない時。通信はできない。")
                    mRemoteEndpointId = null
                }
            }
        }

        // コネクションが切断された時
        override fun onDisconnected(endpointId: String) {
            Log.d(TAG,"コネクションが切断された")
            mRemoteEndpointId = null
        }

    }

    private val mPayloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            when (payload.type) {
                Payload.Type.BYTES -> {
                    // バイト配列を受け取った時
                    val data = payload.asBytes()!!


                    /*スマホ卓球ようの受け取ったタイミングで＋１のやつ
                    val count= findViewById<TextView>(R.id.text_date).text.toString().toInt()+1
                    findViewById<TextView>(R.id.text_date).text = count.toString()

                     */


                    /*受け取った文字列をint型にして＋１して表示するやつ */
                    val countString= String(data)
                    val count = countString.toInt()
                    if(before_volume < count){
                        findViewById<Button>(R.id.textView4).text = "○"
                    }
                    else{
                        findViewById<Button>(R.id.textView4).text = "×"
                    }
                    swing_and_hit = count

                    rally_flag = 0
                    Log.d(TAG,data.toString())


                    Log.d(TAG,"バイト配列を受け取った")
                    // 処理
                }
                Payload.Type.FILE -> {
                    // ファイルを受け取った時
                    val file = payload.asFile()!!
                    // 処理
                }
                Payload.Type.STREAM -> {
                    // ストリームを受け取った時
                    val stream = payload.asStream()!!
                    // 処理
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // 転送状態が更新された時詳細は省略
        }
    }








    override fun onClick(view: View) {
        val i = view.id
        if (i == R.id.button_start) {
            sensorManager!!.registerListener(
                this, accel,
                SensorManager.SENSOR_DELAY_GAME
            )
        } else if (i == R.id.button_stop) {
            sensorManager!!.unregisterListener(this)
        }
    }

    override fun onResume() {
        super.onResume()
        // Listenerの登録
        sensorManager!!.registerListener(
            this, accel,
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    override fun onPause() {
        super.onPause()
        // Listenerを解除
        sensorManager!!.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val HighPassNorm: Double
        val LowPassNorm: Double
        val difftime: Long
        val nowtime: LocalDateTime
        //フィルタ設定
        val order = 10
        if (event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
            sensorx = event.values[0].toDouble()
            sensory = event.values[1].toDouble()
            sensorz = event.values[2].toDouble()

            //フィルタ処理のライブラリ呼び出し
            val butterworth_hx = Butterworth()
            val butterworth_hy = Butterworth()
            val butterworth_hz = Butterworth()
            butterworth_hx.highPass(order, 50.0, 15.0)
            butterworth_hy.highPass(order, 50.0, 15.0)
            butterworth_hz.highPass(order, 50.0, 15.0)
            val butterworth_lx = Butterworth()
            val butterworth_ly = Butterworth()
            val butterworth_lz = Butterworth()
            butterworth_lx.lowPass(order, 50.0, 3.0)
            butterworth_ly.lowPass(order, 50.0, 3.0)
            butterworth_lz.lowPass(order, 50.0, 3.0)
            val hx = butterworth_hx.filter(event.values[0].toDouble())
            val hy = butterworth_hy.filter(event.values[1].toDouble())
            val hz = butterworth_hz.filter(event.values[2].toDouble())
            val lx = butterworth_lx.filter(event.values[0].toDouble())
            val ly = butterworth_ly.filter(event.values[1].toDouble())
            val lz = butterworth_lz.filter(event.values[2].toDouble())

            //加速度　-> ノルム　√ [絶対値](x^2 + y^2 + z^2)
            HighPassNorm = Math.sqrt(
                Math.abs(
                    Math.pow(hx, 2.0) + Math.pow(hy, 2.0) + Math.pow(
                        hz,
                        2.0
                    )
                )
            ) * 10000
            LowPassNorm = Math.sqrt(
                Math.abs(
                    Math.pow(lx, 2.0) + Math.pow(ly, 2.0) + Math.pow(
                        lz,
                        2.0
                    )
                )
            ) * 10000000

            //時間取得
            nowtime = LocalDateTime.now()
            if (timeflag) {
                difftime = ChronoUnit.MILLIS.between(starttime, startendtime)
                timeflag = false
            } else {
                difftime = ChronoUnit.MILLIS.between(starttime, nowtime)
            }
            hit(HighPassNorm, difftime)
            swing(LowPassNorm)
            if (hit_flag) {
                //sound_n(swing_and_hit)
                hit_flag = false
            }
            Hithantei!!.text = swing_and_hit.toString()
            val accelero: String
            accelero = String.format(
                Locale.US,
                "X: %.3f\nY: %.3f\nZ: %.3f",
                event.values[0], event.values[1], event.values[2]
            )
            textView!!.text = accelero
        }
    }

    fun hit(acc_num: Double, Nowtime: Long) {
        //持ってくる値はハイパスかけた後のノルムデータと時間
        if (Nowtime > 4000) {

            //開始4秒はカウントしない
            if (bl_hit_updown) {
                ///5秒（hit_out ３００回データ）経過するとカウントを０にする
                hitout += 1
                if (hitout >= 300) {
                    hit_count = 0
                    swing_and_hit = 0
                    rally_flag = 0

                    date_push()
                }
                if (rally_flag == 0){
                    //trueの場合、ハイパス後ノルムが1.0くらいを越えると１回カウント。
                    //1.0>0.3
                    if (acc_num > 0.2) {
                        hit_count += 1
                        hit_hantei = true
                        bl_hit_updown = false
                        hitout = 0
                        bl_onhit = true //スイングヒットに使用
                    }
                }
            } else if (!bl_hit_updown) {
                //falseの場合、30回データが送り込まれる(0.6秒)までヒット回数を数えないように
                hit_keep_thirty += 1
                if (hit_keep_thirty >= 30) {
                    bl_hit_updown = true
                    hit_keep_thirty = 0
                }
            }
        }
    }

    fun swing(acc_num: Double) {
        //持ってくる値はローパスかけた後のノルムデータ
        if (acc_num > max_acc) {
            //maxより大きかった場合置き換え
            max_acc = acc_num
        } else if (acc_num < max_acc) {
            //maxから加速度が下がった最初のタイミングでスイング推定を行う。
            //極大値と極小値の差を求め、diffnumが10以上だった場合スイングと推定。極小値のリセットとして極大値を入れる
            if (bl_swing_updown) {
                val diffnum = max_acc - min_acc

                //カウント処理に向かう（スイングfalseケース)
                //10.0>1.0
                if (diffnum > 1.0) {
                    swing_count += 1
                    swing_hantei = true
                    bl_onswing = true //スイングヒットのカウントに使用。
                }
                SwingHitCount() //カウント処理に向かう（スイングtrueケース）
                min_acc = max_acc
                bl_swing_updown = false
            }
        }
        if (acc_num < min_acc) {
            //minより小さかった場合置き換え　
            min_acc = acc_num
        } else if (acc_num > min_acc) {
            //minから加速度が上がった最初のタイミングで極大値をリセット。極大値に極小値を入れる。
            if (!bl_swing_updown) {
                max_acc = min_acc
                bl_swing_updown = true
            }
        }
    }

    fun SwingHitCount() {
        //スイングのみ、ヒットのみ、両方、それ以外を設定
        if (bl_onhit && bl_onswing) {
            val audioSensor: AudioSensor = AudioSensor(this)
            this
            audioSensor.start(10, AudioSensor.RECORDING_DB)
            volume = audioSensor.getVolume()

                //swing_and_hit += 1
                bl_onhit = false
                bl_onswing = false
                hit_flag = true
                before_volume = volume
                date_push()


        } else if (!bl_onhit && bl_onswing) {
            swing_only += 1
            bl_onswing = false
            hit_flag = false
        } else if (bl_onhit && !bl_onswing) {
            hit_only += 1
            bl_onhit = false
            hit_flag = false
        } else if (!bl_onhit && !bl_onswing) {
            hit_flag = false
        }
    }
    //nearbyshareのデータを送る機能
    fun date_push(){
        Log.d(TAG,"date_pushをタップ")

        val data = volume.toString().toByteArray()
        val payload = Payload.fromBytes(data)

        Nearby.getConnectionsClient(activity)
            .sendPayload(mRemoteEndpointId.toString(), payload)
        Log.d(TAG,"データを送った")
        /*スマホ卓球ようの送ったタイミングで＋１のやつ
        val count= findViewById<TextView>(R.id.text_date).text.toString().toInt()+1
        findViewById<TextView>(R.id.text_date).text = count.toString()

         */

        rally_flag = 1
    }

    /*
    fun sound_n(count: Int) {
        val Soundcount = count - 1

        //音声呼び出し
        if (Soundcount <= 41) {
            NumCountInstance!!.play_n(Soundcount)
        } else {
            NumCountInstance!!.play_n(41)
        }
    }

     */

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
}