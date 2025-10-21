package com.bbkb.sc.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import androidx.lifecycle.lifecycleScope
import com.bbkb.sc.databinding.ActivitySplashBinding
import com.bbkb.sc.schedule.StartPreference
import com.bbkb.sc.ui.appwidget.TableWidgetManager
import com.bbkb.sc.util.ScheduleUtils
import com.poria.base.base.BaseActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity<ActivitySplashBinding>() {
    override fun onViewBindingCreate() = ActivitySplashBinding.inflate(layoutInflater)

    override fun initView() {
        updateTableWidgetAsync()
        goTo()
    }

    private fun goTo() = lifecycleScope.launch {
        val zc = ScheduleUtils.getZC(System.currentTimeMillis())
        if (zc == -1) {
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
            return@launch
        }
        val intent = StartPreference.latest.run {
            Intent(
                this@SplashActivity,
                if (goToNoteWhenStartApp)
                    NoteCategoryListActivity::class.java
                else if (goToTableWhenStartApp)
                    TableActivity::class.java
                else
                    MainActivity::class.java
            )
        }
        intent.putExtra(KEY_TAG_WHO_START_ME, TAG)
        startActivity(intent)
        finish()
    }

    private fun updateTableWidgetAsync() {
        CoroutineScope(Dispatchers.Default).launch {
            TableWidgetManager.updateAllWidget(baseContext)
        }
    }

    companion object {
        const val TAG = "SplashActivity"
        const val KEY_TAG_WHO_START_ME = "who_start_me"
    }
}