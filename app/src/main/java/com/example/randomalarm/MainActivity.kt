package com.example.randomalarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.randomalarm.databinding.ActivityMainBinding
import java.util.Calendar
import java.util.Random

class MainActivity : AppCompatActivity() {
    private lateinit var alarmManager: AlarmManager
    private lateinit var alarmPendingIntent: PendingIntent// 알람을 위한 PendingIntent 변수 추가
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.numberPicker.minValue = 1
        binding.numberPicker.maxValue = 60

        binding.setAlarmButton.setOnClickListener {
            val hour = binding.timePicker.hour
            val minute = binding.timePicker.minute
            val range = binding.numberPicker.value
            setAlarm(hour, minute, range)
        }
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    fun setAlarm(hour: Int, minute: Int, range: Int) {
        val currentTime = Calendar.getInstance()

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(this, AlarmReceiver::class.java)

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)

        // 진동 설정
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) {
            val vibrationEffect = VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(vibrationEffect)
        }

        // 알람이 울릴 날짜 설정 (매일)
        calendar.set(Calendar.DAY_OF_MONTH, currentTime.get(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.MONTH, currentTime.get(Calendar.MONTH))
        calendar.set(Calendar.YEAR, currentTime.get(Calendar.YEAR))

        val random = Random()
        val randomMinutes = random.nextInt(range*2+1) - range

        calendar.add(Calendar.MINUTE, randomMinutes)

        val alarmHour = calendar.get(Calendar.HOUR)
        val alarmMinute = calendar.get(Calendar.MINUTE)

        val sharedPref = getSharedPreferences("AlarmPreferences", Context.MODE_PRIVATE)
        // SharedPreferences에 알람 정보 저장
        with(sharedPref.edit()) {
            putInt("hour", alarmHour)
            putInt("minute", alarmMinute)
            putInt("range", range)
            apply()
        }


        alarmPendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, FLAG_UPDATE_CURRENT)
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY, // 매일 반복
            alarmPendingIntent
        )

        Toast.makeText(this, "알람 설정 : $alarmHour 시 $alarmMinute 분", Toast.LENGTH_SHORT).show()
    }

    fun getAlarm(view: View) {
        val sharedPref = getSharedPreferences("AlarmPreferences", Context.MODE_PRIVATE)
        val hour = sharedPref.getInt("hour", -1)
        val minute = sharedPref.getInt("minute", -1)
        if (hour != -1 && minute != -1) {
            val range = sharedPref.getInt("range", 0)
            Toast.makeText(this, "설정된 알람 : $hour 시 $minute 분", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "설정된 알람이 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    fun cancelAlarm(view: View) {
        if (::alarmPendingIntent.isInitialized) {
            alarmManager.cancel(alarmPendingIntent)
            Toast.makeText(this, "알람이 취소되었습니다.", Toast.LENGTH_SHORT).show()

            // SharedPreferences에 저장된 알람 정보 제거
            val sharedPref = getSharedPreferences("AlarmPreferences", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                remove("hour")
                remove("minute")
                remove("range")
                apply()
            }
        } else {
            Toast.makeText(this, "취소할 알람이 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}