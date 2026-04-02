package com.pic.catcher.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pic.catcher.base.BaseFragment
import com.pic.catcher.base.LifecycleAutoViewBinding
import com.pic.catcher.databinding.FragmentMainBinding

/**
 * 主页面 (重构中)
 * 逻辑已按照要求完全删除，等待新设计。
 */
class MainFragment : BaseFragment() {
    private var mainBinding: FragmentMainBinding by LifecycleAutoViewBinding<MainFragment, FragmentMainBinding>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentMainBinding.inflate(inflater, container, false).let {
            mainBinding = it
            it.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 页面逻辑已清空，等待后续设计方案。
    }
}
