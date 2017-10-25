package com.qyh.litemvp.nucleus.factory;


import com.qyh.litemvp.nucleus.presenter.Presenter;

public interface PresenterFactory<P extends Presenter> {
    P createPresenter();
}
