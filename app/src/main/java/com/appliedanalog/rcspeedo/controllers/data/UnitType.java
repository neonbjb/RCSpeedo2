/*
 * Copyright Applied Analog (c) 2015/2016
 *
 * This code is free for use in any non-commercial software. It carries
 * no restrictions in such software.
 */

package com.appliedanalog.rcspeedo.controllers.data;

import java.text.NumberFormat;

public class UnitType {
	
	static NumberFormat format;
	static{
		format = NumberFormat.getNumberInstance();
		format.setMaximumFractionDigits(1);
		format.setMinimumFractionDigits(1);
	}
	
	public String name;
	public String vocal;
	public String suffix;
	public double mult;
	public double add;
	public UnitType(String n, String s, double m, double a){
		name = n;
		suffix = s;
		mult = m;
		add = a;
		vocal = name;
	}
	
	public double doConversion(double in){
		return (in * mult + add);
	}
	
	public double backConversion(double in){
		return ((in - add) / mult);
	}
	
	public String getNeat(double in){
		return format.format(doConversion(in)) + suffix;
	}
	
	public String getVocal(double in){
		return format.format(doConversion(in)) + " " + vocal;
	}
}
