/*
 * Copyright Applied Analog (c) 2015/2016
 *
 * This code is free for use in any non-commercial software. It carries
 * no restrictions in such software.
 */

package com.appliedanalog.rcspeedo.controllers.data.logs;

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
