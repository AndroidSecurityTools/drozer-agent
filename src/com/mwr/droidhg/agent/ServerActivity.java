package com.mwr.droidhg.agent;

import java.util.Observable;
import java.util.Observer;

import com.mwr.droidhg.Agent;
import com.mwr.droidhg.agent.views.ConnectorStatusIndicator;
import com.mwr.droidhg.api.ConnectorParameters;
import com.mwr.droidhg.api.ServerParameters;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;

public class ServerActivity extends Activity implements Observer, ConnectorParameters.OnLogMessageListener {

	private ServerParameters parameters = null;
	
	private TextView label_server_fingerprint = null;
	private TextView label_server_ssl = null;
	private CompoundButton server_enabled = null;
	private TextView server_messages = null;
	private ConnectorStatusIndicator server_status_indicator = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        this.setContentView(R.layout.activity_server);
        
        this.label_server_fingerprint = (TextView)this.findViewById(R.id.label_server_fingerprint);
        this.label_server_ssl = (TextView)this.findViewById(R.id.label_server_ssl);
        this.server_enabled = (CompoundButton)this.findViewById(R.id.server_enabled);
        this.server_messages = (TextView)this.findViewById(R.id.server_messages);
        this.server_status_indicator = (ConnectorStatusIndicator)this.findViewById(R.id.server_status_indicator);
        
        this.setServerParameters(Agent.getServerParameters());
        
        this.server_enabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked)
					Agent.startServer();
				else
					Agent.stopServer();
			}
        	
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

	@Override
	public void onLogMessage(String message) {
		String log = this.server_messages.getText() + "\n" + message;
		
		this.server_messages.setText(log);
	}
    
    @Override
    protected void onPause() {
    	super.onPause();
    	
    	Agent.unbindServices();
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	
    	Agent.bindServices();
    }
    
    class CalculateFingerprint extends AsyncTask<Object, Object, String> {

		@Override
		protected String doInBackground(Object... params) {
			try {
				return ServerActivity.this.parameters.getCertificateFingerprint();
			}
			catch(Exception e) {
				return "failed to calculate";
			}
		}
		
		@Override
		protected void onPreExecute() {
			ServerActivity.this.label_server_fingerprint.setText("Calculating Fingerprint...");
		}
		
		@Override
		protected void onPostExecute(String result) {
			ServerActivity.this.fingerprint_calculation = null;
			ServerActivity.this.label_server_fingerprint.setText(result);
		}
    	
    }
    
    private CalculateFingerprint fingerprint_calculation;
    
    private void setServerParameters(ServerParameters parameters) {
    	if(this.parameters != null)
    		this.parameters.deleteObserver(this);
    	
    	this.parameters = parameters;
    	
    	if(this.fingerprint_calculation != null)
    		this.fingerprint_calculation.cancel(true);

		if(ServerActivity.this.parameters.isSSL()) {
	    	this.fingerprint_calculation = new CalculateFingerprint();
	    	this.fingerprint_calculation.execute();
		}
		else {
			this.label_server_fingerprint.setVisibility(View.GONE);
		}
    	
    	this.label_server_ssl.setText(this.parameters.isSSL() ? R.string.ssl_enabled : R.string.ssl_disabled);
    	this.server_enabled.setChecked(this.parameters.isEnabled());
    	this.server_status_indicator.setConnector(this.parameters);
    	
    	this.parameters.addObserver(this);
    	this.parameters.setOnLogMessageListener(this);
    }

	@Override
	public void update(Observable observable, Object data) {
		this.setServerParameters((ServerParameters)observable);
	}

}
