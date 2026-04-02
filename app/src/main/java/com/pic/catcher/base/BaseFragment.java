package com.pic.catcher.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * @author Mingyueyixi
 * @date 2024/9/28 19:28
 * @description BaseFragment migrated to androidx
 */
public class BaseFragment extends Fragment implements CustomLifecycleOwner {

    private CustomLifecycleOwnerDelegate mCustomLifecycleOwnerDelegate = new CustomLifecycleOwnerDelegate();

    @NonNull
    @Override
    public CustomLifecycle.State getCurrentState() {
        return mCustomLifecycleOwnerDelegate.getCurrentState();
    }

    @Override
    public void addObserver(@NonNull CustomLifecycle life) {
        if (life == null) {
            return;
        }
        mCustomLifecycleOwnerDelegate.addObserver(life);
    }

    @Override
    public void removeObserver(@NonNull CustomLifecycle life) {
        mCustomLifecycleOwnerDelegate.removeObserver(life);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCustomLifecycleOwnerDelegate.setCurrentState(CustomLifecycle.State.CREATED);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        mCustomLifecycleOwnerDelegate.setCurrentState(CustomLifecycle.State.RESUMED);
    }

    @Override
    public void onStart() {
        super.onStart();
        mCustomLifecycleOwnerDelegate.setCurrentState(CustomLifecycle.State.STARTED);
    }

    @Override
    public void onPause() {
        super.onPause();
        mCustomLifecycleOwnerDelegate.setCurrentState(CustomLifecycle.State.PAUSED);
    }

    @Override
    public void onStop() {
        super.onStop();
        mCustomLifecycleOwnerDelegate.setCurrentState(CustomLifecycle.State.STOPPED);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCustomLifecycleOwnerDelegate.setCurrentState(CustomLifecycle.State.DESTROYED);
    }
}
