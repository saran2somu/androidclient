package org.kontalk.service;

import java.util.List;

import org.apache.http.NameValuePair;

public class RequestJob {

    protected String mCommand;
    protected List<NameValuePair> mParams;
    protected byte[] mContent;
    protected RequestListener mListener;
    protected boolean mCancel;

    public RequestJob(String cmd, List<NameValuePair> params) {
        this(cmd, params, null);
    }

    public RequestJob(String cmd, List<NameValuePair> params, byte[] content) {
        mCommand = cmd;
        mParams = params;
        mContent = content;
    }

    public String toString() {
        return getClass().getSimpleName() + ": cmd=" + mCommand;
    }

    public String getCommand() {
        return mCommand;
    }

    public List<NameValuePair> getParams() {
        return mParams;
    }

    public byte[] getContent() {
        return mContent;
    }

    public void setListener(RequestListener listener) {
        mListener = listener;
    }

    public RequestListener getListener() {
        return mListener;
    }

    /**
     * Sets the cancel flag.
     * The {@link RequestWorker} will see this flag and abort executing the
     * request if still possible.
     */
    public void cancel() {
        mCancel = true;
    }

    public boolean isCanceled() {
        return mCancel;
    }
}