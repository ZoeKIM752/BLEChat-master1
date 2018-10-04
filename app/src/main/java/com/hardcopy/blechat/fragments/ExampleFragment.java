/*
 * Copyright (C) 2014 Bluetooth Connection Template
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hardcopy.blechat.fragments;

import android.annotation.SuppressLint;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;

import com.hardcopy.blechat.MainActivity;
import com.hardcopy.blechat.R;

import java.io.File;

@SuppressLint("ValidFragment")
public class ExampleFragment extends Fragment implements View.OnClickListener {

	private Context mContext = null;
	private IFragmentListener mFragmentListener = null;
	private Handler mActivityHandler = null;

	TextView mTextChat;
	//EditText mEditChat;
	Button mBtnStart;
	// Stop
	Button mBtnStop;

	public ExampleFragment(Context c, IFragmentListener l, Handler h) {
		mContext = c;
		mFragmentListener = l;
		mActivityHandler = h;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_main_dummy, container, false);
		
		mTextChat = (TextView) rootView.findViewById(R.id.text_chat);
		mTextChat.setMaxLines(1000);
		mTextChat.setVerticalScrollBarEnabled(true);
		mTextChat.setMovementMethod(new ScrollingMovementMethod());
		
		//mEditChat = (EditText) rootView.findViewById(R.id.edit_chat);
		//mEditChat.setOnEditorActionListener(mWriteListener);

		mBtnStart = (Button) rootView.findViewById(R.id.button_start);
		mBtnStart.setOnClickListener(this);

		mBtnStop = (Button) rootView.findViewById(R.id.button_stop);
		mBtnStop.setOnClickListener(this);

		return rootView;
	}
	
	@Override
	public void onClick(View v) {
		switch(v.getId()) {
			case R.id.button_start:
				sendMessage("start");
				mBtnStop.setEnabled(true);
				mBtnStart.setEnabled(false);
				break;

			case R.id.button_stop:
				sendMessage("stop");
				mBtnStop.setEnabled(false);
				mBtnStart.setEnabled(true);
				break;
		}
	}
	
	
    // The action listener for the EditText widget, to listen for the return key
    private TextView.OnEditorActionListener mWriteListener =
        new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                if(message != null && message.length() > 0)
                	sendMessage(message);
            }
            return true;
        }
    };
	
    // Sends user message to remote
    private void sendMessage(String message) {
    	if(message == null || message.length() < 1)
    		return;
    	// send to remote
    	if(mFragmentListener != null) {
			mFragmentListener.OnFragmentCallback(IFragmentListener.CALLBACK_SEND_MESSAGE, 0, 0, message, null, null);
			Log.d(this.getClass().getName(), "send start!!!!!!!!!");
    	}else
    		return;
    	// show on text view
    	if(mTextChat != null) {
    		mTextChat.append("\nSend: ");
    		mTextChat.append(message+"\n");
    		if(message.equals("stop"))
    			mTextChat.append("--------------------------------------\n");
        	int scrollamout = mTextChat.getLayout().getLineTop(mTextChat.getLineCount()) - mTextChat.getHeight();
        	if (scrollamout > mTextChat.getHeight())
        		mTextChat.scrollTo(0, scrollamout);
    	}
    	//mEditChat.setText("");
    }
    
    private static final int NEW_LINE_INTERVAL = 1000;
    private long mLastReceivedTime = 0L;
    
    // Show messages from remote
    public void showMessage(String message) {
        message = message.replace("\r\n","");
    	if(message != null && message.length() > 0) {
    		long current = System.currentTimeMillis();
    		
    		if(current - mLastReceivedTime > NEW_LINE_INTERVAL) {
    			mTextChat.append("\nRcv: ");
    		}

    		if(message.equals("Setting")) {
				mTextChat.append("\nSetting...\n");
			}
    		else if(message.equals("DONE"))
    			mTextChat.append("\nYou can START NOW\n");
    		else {
				mTextChat.append(message+"\n");
			}
			int scrollamout = mTextChat.getLayout().getLineTop(mTextChat.getLineCount()) - mTextChat.getHeight();
    		if (scrollamout > mTextChat.getHeight())
    			mTextChat.scrollTo(0, scrollamout);
        	
        	mLastReceivedTime = current;
    	}
    }
}
