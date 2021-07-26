package com.example.operator.withjavaoperatorsdk;

public class PingStatus {

	private String status;

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return "PingStatus{" + "status='" + status + '\'' + '}';
	}
}
