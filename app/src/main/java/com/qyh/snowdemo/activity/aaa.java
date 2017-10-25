package com.qyh.snowdemo.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;

import com.qyh.litemvp.nucleus.factory.RequiresPresenter;
import com.qyh.litemvp.nucleus.view.NucleusAppCompatActivity;
import com.vise.snowdemo.R;


/**
 * @author 邱永恒
 * @time 2017/10/19  17:27
 * @desc ${TODD}
 */
@RequiresPresenter(ppp.class)
public class aaa extends NucleusAppCompatActivity<ppp>{

    private Button btn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.aaa);
        btn = (Button) findViewById(R.id.btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getPresenter().show("gaga");
            }
        });
    }
}
