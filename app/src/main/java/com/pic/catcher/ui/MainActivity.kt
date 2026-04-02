package com.pic.catcher.ui

import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import com.google.android.material.color.DynamicColors
import com.pic.catcher.R
import com.pic.catcher.base.BaseActivity
import com.pic.catcher.databinding.LayoutMainBinding
import com.pic.catcher.route.AppRouter
import com.pic.catcher.ui.vm.AppUpdateViewModel
import com.pic.catcher.base.ViewModelProviders

class MainActivity : BaseActivity() {
    private lateinit var binding: LayoutMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // 启用 Material You 动态取色
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        
        // 沉浸式状态栏支持
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        binding = LayoutMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_settings -> {
                    switchFragment(SettingsFragment::class.java)
                    true
                }
                R.id.nav_home -> {
                    switchFragment(HomeFragment::class.java)
                    true
                }
                R.id.nav_other -> {
                    switchFragment(OtherFragment::class.java)
                    true
                }
                else -> false
            }
        }

        // 默认选中中间的首页
        binding.bottomNavigation.selectedItemId = R.id.nav_home

        ViewModelProviders.from(this).get(AppUpdateViewModel::class.java).checkOnEnter(this)
        handleDeeplinkRoute(intent)
    }

    private fun switchFragment(clazz: Class<out Fragment>) {
        val tag = clazz.name
        val transaction = supportFragmentManager.beginTransaction()
        
        // 隐藏当前所有 Fragment
        supportFragmentManager.fragments.forEach { transaction.hide(it) }
        
        var fragment = supportFragmentManager.findFragmentByTag(tag)
        if (fragment == null) {
            fragment = clazz.newInstance()
            transaction.add(R.id.mainContainer, fragment, tag)
        } else {
            transaction.show(fragment)
        }
        transaction.commit()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleDeeplinkRoute(intent)
    }

    private fun handleDeeplinkRoute(intent: Intent?) {
        if (intent == null) return
        val from = intent.getStringExtra("from")
        if (DeepLinkActivity::class.java.name != from) {
            return
        }
        intent.data?.let {
            binding.root.post {
                AppRouter.route(this, it.toString())
            }
        }
    }
}
