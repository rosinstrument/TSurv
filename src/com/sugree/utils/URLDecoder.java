package com.sugree.utils;
/*
 * http://jcs.mobile-utopia.com/jcs/919_URLDecoder.java
 */

import java.io.ByteArrayOutputStream;


/**
 * Turns Strings of x-www-form-urlEncoded format into regular text.
 *
 * @version 1.0, 4/3/1996
 * @author Elliotte Rusty Harold
 */

public class URLDecoder {

	private URLDecoder() { }

	/**
	 * Translates String from x-www-form-urlEncoded format into text.
	 * @param s String to be translated
	 * @return the translated String.
	 */
	public static String decode(String s) {

		ByteArrayOutputStream out = new ByteArrayOutputStream(s.length());

		for (int i = 0; i < s.length(); i++) {
			int c = (int) s.charAt(i);
			if (c == '+') {
				out.write(' ');
			}
			else if (c == '%') {
				int c1 = Character.digit(s.charAt(++i), 16);
				int c2 = Character.digit(s.charAt(++i), 16);
				out.write((char) (c1 * 16 + c2));
			}
			else {
				out.write(c);
			}
		} // end for

		return out.toString();

	}

}
