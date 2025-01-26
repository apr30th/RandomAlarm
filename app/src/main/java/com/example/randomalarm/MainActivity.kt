package com.example.randomalarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.randomalarm.databinding.ActivityMainBinding
import java.util.Calendar
import java.util.Random

class MainActivity : AppCompatActivity() {
    private lateinit var alarmManager: AlarmManager
    private lateinit var alarmPendingIntent: PendingIntent
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // UI 초기화
        binding.numberPicker.minValue = 1
        binding.numberPicker.maxValue = 60

        binding.setAlarmButton.setOnClickListener {
            val hour = binding.timePicker.hour
            val minute = binding.timePicker.minute
            val range = binding.numberPicker.value
            setAlarm(hour, minute, range)
        }

        binding.cancelAlarmButton.setOnClickListener {
            cancelAlarm()
        }

        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    private fun checkAndRequestExactAlarmPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }

    private fun setAlarm(hour: Int, minute: Int, range: Int) {
        checkAndRequestExactAlarmPermission() // 권한 확인 및 요청

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // 랜덤 범위 추가
            val random = Random()
            val randomMinutes = random.nextInt(range * 2 + 1) - range
            add(Calendar.MINUTE, randomMinutes)
        }

        // 알람 시간 표시
        val alarmHour = calendar.get(Calendar.HOUR_OF_DAY)
        val alarmMinute = calendar.get(Calendar.MINUTE)

        // 알람 정보 저장
        val sharedPref = getSharedPreferences("AlarmPreferences", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("hour", alarmHour)
            putInt("minute", alarmMinute)
            putInt("range", range)
            apply()
        }

        // PendingIntent 생성
        val alarmIntent = Intent(this, AlarmReceiver::class.java)
        alarmPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 정확한 알람 설정
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            alarmPendingIntent
        )

        Toast.makeText(this, "알람 설정 완료: ${alarmHour}시 ${alarmMinute}분", Toast.LENGTH_SHORT).show()
    }

    fun getAlarm(view: View) {
        val sharedPref = getSharedPreferences("AlarmPreferences", Context.MODE_PRIVATE)
        val hour = sharedPref.getInt("hour", -1)
        val minute = sharedPref.getInt("minute", -1)

        if (hour != -1 && minute != -1) {
            val range = sharedPref.getInt("range", 0)
            Toast.makeText(this, "설정된 알람: ${hour}시 ${minute}분", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "설정된 알람이 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }



    private fun cancelAlarm() {
        if (::alarmPendingIntent.isInitialized) {
            alarmManager.cancel(alarmPendingIntent)
            Toast.makeText(this, "알람이 취소되었습니다.", Toast.LENGTH_SHORT).show()

            // 저장된 알람 정보 제거
            val sharedPref = getSharedPreferences("AlarmPreferences", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                clear()
                apply()
            }
        } else {
            Toast.makeText(this, "취소할 알람이 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}
