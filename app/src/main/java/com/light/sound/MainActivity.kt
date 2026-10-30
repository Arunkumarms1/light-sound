package com.light.sound

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.material3.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var cameraManager: CameraManager
    private var cameraId: String? = null
    private var isFlashOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val ids = cameraManager.cameraIdList
            cameraId = ids.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                facing == CameraCharacteristics.LENS_FACING_BACK
            } ?: ids.firstOrNull()
        } catch (e: CameraAccessException) {
            Log.e("LightSound", "Camera access error", e)
        }
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme()
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    LightSoundUI(this@MainActivity)
                }
            }
        }
    }

    fun toggleTorch() {
        val id = cameraId ?: return
        try {
            cameraManager.setTorchMode(id, !isFlashOn)
            isFlashOn = !isFlashOn
        } catch (e: CameraAccessException) {
            Log.e("LightSound", "Torch error", e)
        }
    }

    fun startFlashPattern(text: String) {
        val bits = encodeToBinary(text)
        Thread {
            for (bit in bits) {
                if (bit == '1') {
                    handler.post { toggleTorch() }
                    Thread.sleep(200)
                    handler.post { toggleTorch() }
                } else {
                    Thread.sleep(400)
                }
                Thread.sleep(300)
            }
        }.start()
    }

    fun playSoundPattern(text: String) {
        val bits = encodeToBinary(text)
        Thread {
            val sampleRate = 44100
            val durationMs = 100
            val count = (sampleRate * durationMs) / 1000
            val buffer = ShortArray(count)
            for (bit in bits) {
                val freq = if (bit == '1') 880 else 440
                val amplitude = 32767 / 4
                for (i in buffer.indices) {
                    buffer[i] = (amplitude * kotlin.math.sin(2.0 * Math.PI * freq * i / sampleRate)).toInt().toShort()
                }
                val track = AudioTrack(
                    AudioTrack.MODE_STATIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    buffer.size * 2,
                    AudioTrack.MODE_STATIC
                )
                track.write(buffer, 0, buffer.size)
                track.play()
                Thread.sleep(durationMs.toLong())
                track.stop()
                track.release()
                Thread.sleep(200)
            }
        }.start()
    }

    fun startListening() {
        val sampleRate = 44100
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val recorder = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)
        recorder.startRecording()
        Thread {
            val data = ShortArray(bufferSize)
            val bits = mutableListOf<Char>()
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < 15000) {
                val read = recorder.read(data, 0, data.size)
                if (read > 0) {
                    var sum = 0
                    for (i in 0 until read) sum += kotlin.math.abs(data[i].toInt())
                    val avg = sum / read.toFloat()
                    bits.add(if (avg > 800) '1' else if (avg > 100) '0' else ' ')
                }
            }
            recorder.stop()
            recorder.release()
            val msg = decodeBinary(bits.joinToString("").trim())
            handler.post {
                Log.d("LightSound", "Decoded message: $msg")
            }
        }.start()
    }

    private fun encodeToBinary(text: String): String {
        val result = StringBuilder()
        for (c in text.uppercase()) {
            val code = when (c) {
                'A' -> "01"
                'B' -> "1000"
                'C' -> "1010"
                'D' -> "100"
                'E' -> "0"
                'F' -> "0010"
                'G' -> "110"
                'H' -> "0000"
                'I' -> "00"
                'J' -> "0111"
                'K' -> "101"
                'L' -> "0100"
                'M' -> "11"
                'N' -> "10"
                'O' -> "111"
                'P' -> "0110"
                'Q' -> "1101"
                'R' -> "010"
                'S' -> "000"
                'T' -> "1"
                'U' -> "001"
                'V' -> "0001"
                'W' -> "011"
                'X' -> "1001"
                'Y' -> "1011"
                'Z' -> "1100"
                ' ' -> "1111"
                else -> ""
            }
            result.append(code).append(" ")
        }
        return result.toString()
    }

    private fun decodeBinary(bits: String): String {
        val map = mapOf(
            "01" to 'A', "1000" to 'B', "1010" to 'C', "100" to 'D', "0" to 'E',
            "0010" to 'F', "110" to 'G', "0000" to 'H', "00" to 'I', "0111" to 'J',
            "101" to 'K', "0100" to 'L', "11" to 'M', "10" to 'N', "111" to 'O',
            "0110" to 'P', "1101" to 'Q', "010" to 'R', "000" to 'S', "1" to 'T',
            "001" to 'U', "0001" to 'V', "011" to 'W', "1001" to 'X', "1011" to 'Y',
            "1100" to 'Z', "1111" to ' '
        )
        val clean = bits.replace(" ", "")
        val result = StringBuilder()
        var i = 0
        while (i < clean.length) {
            for (len in 4 downTo 1) {
                if (i + len <= clean.length) {
                    val sub = clean.substring(i, i + len)
                    if (map.containsKey(sub)) {
                        result.append(map[sub])
                        i += len
                        break
                    }
                }
            }
            i++
        }
        return result.toString()
    }
}
