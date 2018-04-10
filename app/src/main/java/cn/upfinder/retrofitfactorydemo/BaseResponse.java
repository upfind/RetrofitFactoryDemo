package cn.upfinder.retrofitfactorydemo;

/**
 * Created by ucm on 2018/4/10.
 */

public class BaseResponse<T> {
    private int state;
    private String msg;
    private T data;

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }


    public boolean isOk() {
        return state == 0;
    }
}
