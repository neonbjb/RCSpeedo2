package com.appliedanalog.rcspeedo.logs;

public class EmptyLogEntry extends LogEntry {

	@Override
	public int getType() {
		return EMPTY_ENTRY;
	}

	@Override
	public String getMain() {
		return "";
	}

	@Override
	public String getDateTime() {
		return "";
	}

	@Override
	public String getExt1() {
		return "";
	}

	@Override
	public String getExt2() {
		return "";
	}

	@Override
	public String getExt3() {
		return "";
	}
	
}
