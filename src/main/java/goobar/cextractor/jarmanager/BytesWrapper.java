/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package goobar.cextractor.jarmanager;

import java.util.Arrays;

/**
 * Just a very simple wrapper for bytes array. Useful when you want to place it
 * in some kind of Collection or something like that.
 *
 * @author goobar
 */
@SuppressWarnings("javadoc")
public class BytesWrapper
{

	private byte[] bytes;

	public BytesWrapper(byte[] bytes)
	{
		this.bytes = bytes;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null)
		{
			return false;
		}
		if (getClass() != obj.getClass())
		{
			return false;
		}
		final BytesWrapper other = (BytesWrapper) obj;
		if (!Arrays.equals(bytes, other.bytes))
		{
			return false;
		}
		return true;
	}

	/**
	 * @return the bytes
	 */
	public byte[] getBytes()
	{
		return bytes;
	}

	@Override
	public int hashCode()
	{
		int hash = 3;
		hash = 97 * hash + Arrays.hashCode(bytes);
		return hash;
	}

	/**
	 * @param bytes
	 *                the bytes to set
	 */
	public void setBytes(byte[] bytes)
	{
		this.bytes = bytes;
	}
}
