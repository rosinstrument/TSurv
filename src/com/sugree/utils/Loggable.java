package com.sugree.utils;

public interface Loggable {
	public void setState(String text);
	public void setProgress(int value);
	public void clear();
	public void print(String text);
	public void println(String text);
	public void setText(String text);
}
